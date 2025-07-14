package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.model.Infrastructure;
import pl.logistic.logisticops.model.Transport;
import pl.logistic.logisticops.repository.InfrastructureRepository;
import pl.logistic.logisticops.repository.TransportRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 🚛 SERWIS INTEGRACJI Z TOMTOM TRAFFIC API
 *
 * Funkcjonalności:
 * - 🚨 Monitoring incydentów drogowych w czasie rzeczywistym
 * - 🚧 Śledzenie zamknięć dróg i mostów
 * - 📍 Analiza ruchu na kluczowych trasach militarnych
 * - ⚠️ Automatyczne alerty dla dotkniętych transportów
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TomTomInfrastructureService {

    private final InfrastructureRepository infrastructureRepository;
    private final TransportRepository transportRepository;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${api.tomtom.key}")
    private String tomTomApiKey;

    @Value("${api.tomtom.url}")
    private String tomTomBaseUrl;

    /**
     * 🔄 GŁÓWNY MONITOR - sprawdza incydenty co 5 minut
     */
    @Scheduled(fixedRate = 300000) // 5 minut
    public void monitorTrafficIncidents() {
        if (tomTomApiKey == null || tomTomApiKey.isEmpty()) {
            log.warn("⚠️ TomTom API key not configured, skipping monitoring");
            return;
        }

        log.debug("🔍 Starting TomTom traffic incidents monitoring...");

        try {
            // Monituj kluczowe korytarze transportowe Polski
            monitorCriticalRoutes();

            // Sprawdź ruch wokół znanych obiektów infrastruktury
            monitorKnownInfrastructure();

        } catch (Exception e) {
            log.error("❌ Error during TomTom traffic monitoring", e);
        }
    }

    /**
     * 🛣️ MONITORING KLUCZOWYCH TRAS MILITARNYCH
     */
    private void monitorCriticalRoutes() {
        List<CriticalRoute> routes = List.of(
                new CriticalRoute("A2-WARSZAWA-BERLIN", 52.2297, 21.0122, 52.5200, 13.4050, "Główny korytarz zachodni"),
                new CriticalRoute("A4-KRAKÓW-WROCŁAW", 50.0647, 19.9450, 51.1079, 17.0385, "Południowy korytarz"),
                new CriticalRoute("A1-GDAŃSK-KATOWICE", 54.3520, 18.6466, 50.2649, 19.0238, "Korytarz północ-południe"),
                new CriticalRoute("S8-WARSZAWA-BIAŁYSTOK", 52.2297, 21.0122, 53.1325, 23.1688, "Korytarz wschodni"),
                new CriticalRoute("S7-WARSZAWA-KRAKÓW", 52.2297, 21.0122, 50.0647, 19.9450, "Korytarz centralny")
        );

        for (CriticalRoute route : routes) {
            checkRouteIncidents(route);
        }
    }

    /**
     * 🏗️ MONITORING ZNANEJ INFRASTRUKTURY
     */
    private void monitorKnownInfrastructure() {
        List<Infrastructure> criticalInfra = infrastructureRepository.findActiveByTypes(
                List.of("BRIDGE", "TUNNEL", "HEIGHT_RESTRICTION")
        );

        for (Infrastructure infra : criticalInfra) {
            checkInfrastructureTraffic(infra);
        }
    }

    /**
     * 🔍 Sprawdź incydenty na konkretnej trasie
     */
    private void checkRouteIncidents(CriticalRoute route) {
        try {
            String url = String.format(
                    "%s/traffic/services/5/incidentDetails?bbox=%f,%f,%f,%f" +
                            "&fields={incidents{type,geometry{type,coordinates},properties{iconCategory,description}}}" +
                            "&key=%s",
                    tomTomBaseUrl,
                    Math.min(route.startLat, route.endLat) - 0.2,
                    Math.min(route.startLon, route.endLon) - 0.2,
                    Math.max(route.startLat, route.endLat) + 0.2,
                    Math.max(route.startLon, route.endLon) + 0.2,
                    tomTomApiKey
            );

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            processIncidentResponse(response, route);

        } catch (Exception e) {
            log.warn("⚠️ Failed to check incidents for route {}: {}", route.name, e.getMessage());
        }
    }

    /**
     * 📊 Sprawdź ruch wokół infrastruktury
     */
    private void checkInfrastructureTraffic(Infrastructure infra) {
        try {
            String url = String.format(
                    "%s/traffic/services/4/flowSegmentData/absolute/10/json?" +
                            "point=%f,%f&unit=KMPH&key=%s",
                    tomTomBaseUrl,
                    infra.getLatitude(),
                    infra.getLongitude(),
                    tomTomApiKey
            );

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            processTrafficFlowResponse(response, infra);

        } catch (Exception e) {
            log.warn("⚠️ Failed to check traffic for infrastructure {}: {}", infra.getName(), e.getMessage());
        }
    }

    /**
     * 🚨 Przetwórz odpowiedź o incydentach
     */
    @SuppressWarnings("unchecked")
    private void processIncidentResponse(Map<String, Object> response, CriticalRoute route) {
        if (response == null || !response.containsKey("incidents")) {
            return;
        }

        List<Map<String, Object>> incidents = (List<Map<String, Object>>) response.get("incidents");

        for (Map<String, Object> incident : incidents) {
            Map<String, Object> properties = (Map<String, Object>) incident.get("properties");
            if (properties == null) continue;

            String iconCategory = (String) properties.get("iconCategory");
            String description = (String) properties.get("description");

            // Sprawdź czy to krytyczny incydent
            if (isCriticalIncident(iconCategory)) {
                createIncidentAlert(route, iconCategory, description);

                // Znajdź dotknięte transporty
                notifyAffectedTransports(route, description);
            }
        }
    }

    /**
     * 📈 Przetwórz dane o natężeniu ruchu
     */
    @SuppressWarnings("unchecked")
    private void processTrafficFlowResponse(Map<String, Object> response, Infrastructure infra) {
        if (response == null || !response.containsKey("flowSegmentData")) {
            return;
        }

        Map<String, Object> flowData = (Map<String, Object>) response.get("flowSegmentData");
        Double currentSpeed = (Double) flowData.get("currentSpeed");
        Double freeFlowSpeed = (Double) flowData.get("freeFlowSpeed");

        if (currentSpeed != null && freeFlowSpeed != null) {
            double speedRatio = currentSpeed / freeFlowSpeed;

            // Jeśli ruch < 20% normalnego, może być zamknięcie
            if (speedRatio < 0.2) {
                alertService.createAlert(
                        String.format("🚧 Bardzo wolny ruch przy %s - możliwe zamknięcie", infra.getName()),
                        AlertLevel.HIGH,
                        null,
                        infra.getId(),
                        "TRAFFIC_SLOWDOWN"
                );
            }
        }
    }

    /**
     * 🚨 Sprawdź czy incydent jest krytyczny
     */
    private boolean isCriticalIncident(String iconCategory) {
        if (iconCategory == null) return false;

        return iconCategory.contains("ROAD_CLOSED") ||
                iconCategory.contains("BRIDGE_CLOSED") ||
                iconCategory.contains("BLOCKED") ||
                iconCategory.contains("CONSTRUCTION") ||
                iconCategory.contains("ACCIDENT");
    }

    /**
     * 📢 Stwórz alert o incydencie
     */
    private void createIncidentAlert(CriticalRoute route, String type, String description) {
        AlertLevel level = determineAlertLevel(type);

        String message = String.format("🚨 INCYDENT na %s: %s - %s",
                route.name, type, description != null ? description : "Brak szczegółów");

        alertService.createAlert(message, level, null, null, "TRAFFIC_INCIDENT");

        // WebSocket broadcast
        messagingTemplate.convertAndSend("/topic/traffic/incidents", Map.of(
                "route", route.name,
                "type", type,
                "description", description,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 📞 Powiadom dotknięte transporty
     */
    private void notifyAffectedTransports(CriticalRoute route, String description) {
        List<Transport> activeTransports = transportRepository.findActiveWithLocation();

        for (Transport transport : activeTransports) {
            if (isTransportOnRoute(transport, route)) {
                alertService.createAlert(
                        String.format("⚠️ Twoja trasa może być dotknięta: %s", description),
                        AlertLevel.HIGH,
                        transport.getId(),
                        null,
                        "ROUTE_AFFECTED"
                );

                // Wysyłaj do konkretnego transportu
                messagingTemplate.convertAndSend(
                        "/topic/transport/" + transport.getId() + "/alerts",
                        Map.of("incident", description, "route", route.name)
                );
            }
        }
    }

    /**
     * 🗺️ Sprawdź czy transport jest na trasie
     */
    private boolean isTransportOnRoute(Transport transport, CriticalRoute route) {
        if (transport.getCurrentLatitude() == null || transport.getCurrentLongitude() == null) {
            return false;
        }

        // Prosta heurystyka - sprawdź czy transport jest w bounding box trasy
        double margin = 0.5; // ~50km margin
        return transport.getCurrentLatitude() >= Math.min(route.startLat, route.endLat) - margin &&
                transport.getCurrentLatitude() <= Math.max(route.startLat, route.endLat) + margin &&
                transport.getCurrentLongitude() >= Math.min(route.startLon, route.endLon) - margin &&
                transport.getCurrentLongitude() <= Math.max(route.startLon, route.endLon) + margin;
    }

    private AlertLevel determineAlertLevel(String type) {
        if (type.contains("CLOSED") || type.contains("BLOCKED")) {
            return AlertLevel.CRITICAL;
        } else if (type.contains("CONSTRUCTION") || type.contains("ACCIDENT")) {
            return AlertLevel.HIGH;
        }
        return AlertLevel.MEDIUM;
    }

    /**
     * 📍 Klasa pomocnicza dla tras krytycznych
     */
    private static class CriticalRoute {
        final String name;
        final double startLat, startLon, endLat, endLon;
        final String description;

        CriticalRoute(String name, double startLat, double startLon,
                      double endLat, double endLon, String description) {
            this.name = name;
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
            this.description = description;
        }
    }
}