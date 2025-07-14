package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.api.TomTomTrafficClient;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 🔄 REAL-TIME INFRASTRUCTURE MONITORING
 *
 * SOURCES (NO GDDKiA):
 * - 🚛 TomTom Traffic API (primary)
 * - 🗺️ Google Maps Directions API (routing)
 * - 📊 Internal infrastructure database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeInfrastructureService {

    private final InfrastructureRepository infrastructureRepository;
    private final TransportRepository transportRepository;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IntelligentRouteService intelligentRouteService;
    private final TomTomTrafficClient tomTomClient;
    private final ApiHealthCheckService apiHealthCheck;

    @Value("${api.googlemaps.key}")
    private String googleMapsApiKey;

    /**
     * 🔄 Monitor infrastructure status changes using TomTom API ONLY
     * (GDDKiA API calls removed)
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorInfrastructureStatus() {
        log.debug("🔍 Monitoring infrastructure status (TomTom + Google only)...");

        try {
            // Check if TomTom API is working
            if (!apiHealthCheck.hasWorkingTrafficApi()) {
                log.warn("⚠️ No working traffic API available, skipping monitoring");
                return;
            }

            // Use TomTom for traffic monitoring
            checkTomTomTrafficIncidents();
            checkInfrastructureTrafficFlow();

            // Use Google Maps for route validation when needed
            validateCriticalRoutes();

        } catch (Exception e) {
            log.error("❌ Error during infrastructure monitoring", e);
        }
    }

    /**
     * 🚨 Sprawdź incydenty TomTom na kluczowych trasach
     */
    private void checkTomTomTrafficIncidents() {
        try {
            log.debug("🚛 Checking TomTom traffic incidents...");

            // Define critical routes for military transport
            List<CriticalRoute> routes = List.of(
                    new CriticalRoute("A2-Warsaw-Berlin", 52.2297, 21.0122, 52.5200, 13.4050),
                    new CriticalRoute("A4-Krakow-Wroclaw", 50.0647, 19.9450, 51.1079, 17.0385),
                    new CriticalRoute("A1-Gdansk-Katowice", 54.3520, 18.6466, 50.2649, 19.0238),
                    new CriticalRoute("S8-Warsaw-Bialystok", 52.2297, 21.0122, 53.1325, 23.1688)
            );

            for (CriticalRoute route : routes) {
                List<Map<String, Object>> incidents = tomTomClient.getTrafficIncidents(
                        route.startLat, route.startLon, route.endLat, route.endLon, 50
                );

                if (!incidents.isEmpty()) {
                    processTrafficIncidents(incidents, route);
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ TomTom incidents check failed: {}", e.getMessage());
        }
    }

    /**
     * 📊 Sprawdź natężenie ruchu na znanej infrastrukturze
     */
    private void checkInfrastructureTrafficFlow() {
        try {
            List<Infrastructure> criticalInfra = infrastructureRepository.findActiveByTypes(
                    List.of("BRIDGE", "TUNNEL", "HEIGHT_RESTRICTION")
            );

            for (Infrastructure infra : criticalInfra) {
                Map<String, Object> trafficInfo = tomTomClient.getTrafficInfo(
                        infra.getLatitude(), infra.getLongitude(),
                        infra.getLatitude() + 0.01, infra.getLongitude() + 0.01
                );

                analyzeTrafficFlow(infra, trafficInfo);
            }

        } catch (Exception e) {
            log.warn("⚠️ Traffic flow analysis failed: {}", e.getMessage());
        }
    }

    /**
     * 🗺️ Waliduj kluczowe trasy używając Google Maps
     */
    private void validateCriticalRoutes() {
        if (!apiHealthCheck.hasWorkingMapsApi()) {
            return;
        }

        try {
            // Sprawdź czy główne trasy są nadal dostępne
            // (implementacja opcjonalna - można dodać później)

        } catch (Exception e) {
            log.warn("⚠️ Route validation failed: {}", e.getMessage());
        }
    }

    /**
     * 🚨 Przetwórz incydenty z TomTom
     */
    @SuppressWarnings("unchecked")
    private void processTrafficIncidents(List<Map<String, Object>> incidents, CriticalRoute route) {
        for (Map<String, Object> incident : incidents) {
            try {
                Map<String, Object> properties = (Map<String, Object>) incident.get("properties");
                if (properties == null) continue;

                String iconCategory = (String) properties.get("iconCategory");
                String description = (String) properties.get("description");

                if (isCriticalIncident(iconCategory)) {
                    createIncidentAlert(route, iconCategory, description);
                    checkAffectedTransports(route, description);
                }

            } catch (Exception e) {
                log.warn("⚠️ Error processing incident: {}", e.getMessage());
            }
        }
    }

    /**
     * 📈 Analizuj natężenie ruchu
     */
    @SuppressWarnings("unchecked")
    private void analyzeTrafficFlow(Infrastructure infra, Map<String, Object> trafficInfo) {
        try {
            if (trafficInfo == null || !trafficInfo.containsKey("flowSegmentData")) {
                return;
            }

            Map<String, Object> flowData = (Map<String, Object>) trafficInfo.get("flowSegmentData");
            Double currentSpeed = (Double) flowData.get("currentSpeed");
            Double freeFlowSpeed = (Double) flowData.get("freeFlowSpeed");

            if (currentSpeed != null && freeFlowSpeed != null && freeFlowSpeed > 0) {
                double speedRatio = currentSpeed / freeFlowSpeed;

                // Very slow traffic might indicate closure
                if (speedRatio < 0.2) {
                    alertService.createAlert(
                            String.format("🚧 Very slow traffic near %s (%.0f%% of normal speed)",
                                    infra.getName(), speedRatio * 100),
                            AlertLevel.HIGH,
                            null,
                            infra.getId(),
                            "TRAFFIC_SLOWDOWN"
                    );

                    updateInfrastructureStatus(infra, false, "Traffic slowdown detected");
                }
                // Traffic recovered
                else if (speedRatio > 0.8 && !infra.getIsActive()) {
                    updateInfrastructureStatus(infra, true, "Traffic flow normalized");
                }
            }

        } catch (Exception e) {
            log.warn("⚠️ Error analyzing traffic flow for {}: {}", infra.getName(), e.getMessage());
        }
    }

    /**
     * 🚨 Stwórz alert o incydencie
     */
    private void createIncidentAlert(CriticalRoute route, String type, String description) {
        AlertLevel level = determineAlertLevel(type);

        String message = String.format("🚨 TRAFFIC INCIDENT on %s: %s",
                route.name, description != null ? description : type);

        alertService.createAlert(message, level, null, null, "TRAFFIC_INCIDENT");

        // WebSocket broadcast
        messagingTemplate.convertAndSend("/topic/infrastructure/incidents", Map.of(
                "route", route.name,
                "type", type,
                "description", description,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 🔄 Aktualizuj status infrastruktury
     */
    private void updateInfrastructureStatus(Infrastructure infra, boolean isActive, String reason) {
        boolean wasActive = infra.getIsActive();

        if (wasActive != isActive) {
            infra.setIsActive(isActive);
            infra.setUpdatedAt(LocalDateTime.now());
            infrastructureRepository.save(infra);

            log.info("🔄 Infrastructure status changed: {} -> {} ({})",
                    infra.getName(), isActive ? "ACTIVE" : "INACTIVE", reason);

            messagingTemplate.convertAndSend("/topic/infrastructure/status", Map.of(
                    "infrastructureId", infra.getId(),
                    "name", infra.getName(),
                    "type", infra.getType(),
                    "isActive", isActive,
                    "reason", reason
            ));
        }
    }

    /**
     * 📞 Sprawdź transporty dotknięte incydentem
     */
    private void checkAffectedTransports(CriticalRoute route, String incidentDescription) {
        List<Transport> activeTransports = transportRepository.findActiveWithLocation();

        for (Transport transport : activeTransports) {
            if (isTransportOnRoute(transport, route)) {
                alertService.createAlert(
                        String.format("⚠️ Your route may be affected: %s", incidentDescription),
                        AlertLevel.HIGH,
                        transport.getId(),
                        null,
                        "ROUTE_AFFECTED"
                );

                // Trigger route recalculation
                triggerRouteRecalculation(transport, incidentDescription);
            }
        }
    }

    /**
     * 🗺️ Przelicz trasę dla transportu
     */
    private void triggerRouteRecalculation(Transport transport, String reason) {
        if (!apiHealthCheck.hasWorkingMapsApi()) {
            log.warn("⚠️ Cannot recalculate route - no working maps API");
            return;
        }

        try {
            Double startLat = transport.getCurrentLatitude();
            Double startLng = transport.getCurrentLongitude();

            if (startLat == null || startLng == null) {
                log.warn("⚠️ Cannot recalculate route - missing transport location");
                return;
            }

            // Get destination from approved route
            Double destLat = getDestinationLatitude(transport);
            Double destLng = getDestinationLongitude(transport);

            if (destLat == null || destLng == null) {
                log.warn("⚠️ Cannot recalculate route - missing destination");
                return;
            }

            // Use Google Maps for recalculation
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=%f,%f&destination=%f,%f&key=%s&mode=driving&alternatives=true",
                    startLat, startLng, destLat, destLng, googleMapsApiKey
            );

            // Note: In real implementation, use RestTemplate here
            // Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            log.info("🔄 Route recalculation triggered for transport {} due to: {}",
                    transport.getId(), reason);

        } catch (Exception e) {
            log.error("❌ Failed to recalculate route for transport {}: {}",
                    transport.getId(), e.getMessage());
        }
    }

    // === HELPER METHODS ===

    private boolean isCriticalIncident(String iconCategory) {
        return iconCategory != null && (
                iconCategory.contains("ROAD_CLOSED") ||
                        iconCategory.contains("BRIDGE_CLOSED") ||
                        iconCategory.contains("BLOCKED") ||
                        iconCategory.contains("CONSTRUCTION")
        );
    }

    private AlertLevel determineAlertLevel(String type) {
        if (type.contains("CLOSED") || type.contains("BLOCKED")) {
            return AlertLevel.CRITICAL;
        } else if (type.contains("CONSTRUCTION") || type.contains("ACCIDENT")) {
            return AlertLevel.HIGH;
        }
        return AlertLevel.MEDIUM;
    }

    private boolean isTransportOnRoute(Transport transport, CriticalRoute route) {
        if (transport.getCurrentLatitude() == null || transport.getCurrentLongitude() == null) {
            return false;
        }

        double margin = 0.5; // ~50km margin
        return transport.getCurrentLatitude() >= Math.min(route.startLat, route.endLat) - margin &&
                transport.getCurrentLatitude() <= Math.max(route.startLat, route.endLat) + margin &&
                transport.getCurrentLongitude() >= Math.min(route.startLon, route.endLon) - margin &&
                transport.getCurrentLongitude() <= Math.max(route.startLon, route.endLon) + margin;
    }

    private Double getDestinationLatitude(Transport transport) {
        if (transport.getApprovedRoute() != null && !transport.getApprovedRoute().getSegments().isEmpty()) {
            List<RouteSegment> segments = transport.getApprovedRoute().getSegments();
            return segments.get(segments.size() - 1).getToLatitude();
        }
        return null;
    }

    private Double getDestinationLongitude(Transport transport) {
        if (transport.getApprovedRoute() != null && !transport.getApprovedRoute().getSegments().isEmpty()) {
            List<RouteSegment> segments = transport.getApprovedRoute().getSegments();
            return segments.get(segments.size() - 1).getToLongitude();
        }
        return null;
    }

    /**
     * Helper class for critical routes
     */
    private static class CriticalRoute {
        final String name;
        final double startLat, startLon, endLat, endLon;

        CriticalRoute(String name, double startLat, double startLon, double endLat, double endLon) {
            this.name = name;
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
        }
    }
}