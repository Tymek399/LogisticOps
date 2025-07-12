package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.dto.RouteRequestDTO;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeInfrastructureService {

    private final InfrastructureRepository infrastructureRepository;
    private final TransportRepository transportRepository;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final IntelligentRouteService intelligentRouteService;

    @Value("${api.googlemaps.key}")
    private String googleMapsApiKey;

    /**
     * Monitor infrastructure status changes in real-time
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorInfrastructureStatus() {
        log.debug("Monitoring infrastructure status changes...");

        try {
            checkBridgeClosures();
            checkTunnelRestrictions();
            checkTemporaryRestrictions();
            // HERE Traffic Incidents removed — Google Maps nie udostępnia API do incydentów
        } catch (Exception e) {
            log.error("Error during infrastructure monitoring", e);
        }
    }

    private void checkBridgeClosures() {
        try {
            String url = "https://api.gddkia.gov.pl/incidents/bridges";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("incidents")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> incidents = (List<Map<String, Object>>) response.get("incidents");

                for (Map<String, Object> incident : incidents) {
                    processInfrastructureIncident(incident, "BRIDGE");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch bridge closure data", e);
        }
    }

    private void checkTunnelRestrictions() {
        try {
            String url = "https://api.gddkia.gov.pl/incidents/tunnels";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("restrictions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> restrictions = (List<Map<String, Object>>) response.get("restrictions");

                for (Map<String, Object> restriction : restrictions) {
                    processInfrastructureIncident(restriction, "TUNNEL");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch tunnel restriction data", e);
        }
    }

    private void checkTemporaryRestrictions() {
        try {
            String url = "https://api.gddkia.gov.pl/temporary-restrictions";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("restrictions")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> restrictions = (List<Map<String, Object>>) response.get("restrictions");

                for (Map<String, Object> restriction : restrictions) {
                    updateInfrastructureRestrictions(restriction);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch temporary restrictions", e);
        }
    }

    private void processInfrastructureIncident(Map<String, Object> incident, String type) {
        String incidentId = (String) incident.get("id");
        String name = (String) incident.get("name");
        String status = (String) incident.get("status");

        Infrastructure infrastructure = infrastructureRepository.findByExternalId("GDDKIA_" + incidentId);

        if (infrastructure != null) {
            boolean wasActive = infrastructure.getIsActive();
            boolean isNowActive = !"CLOSED".equalsIgnoreCase(status);

            if (wasActive != isNowActive) {
                infrastructure.setIsActive(isNowActive);
                infrastructure.setUpdatedAt(LocalDateTime.now());
                infrastructureRepository.save(infrastructure);

                String message = String.format("%s %s - status changed to %s",
                        type.toLowerCase(), name, status);

                alertService.createAlert(
                        message,
                        isNowActive ? AlertLevel.MEDIUM : AlertLevel.HIGH,
                        null,
                        infrastructure.getId(),
                        "INFRASTRUCTURE"
                );

                checkAffectedTransports(infrastructure);

                messagingTemplate.convertAndSend("/topic/infrastructure/status", Map.of(
                        "infrastructureId", infrastructure.getId(),
                        "name", infrastructure.getName(),
                        "type", infrastructure.getType(),
                        "isActive", infrastructure.getIsActive()
                ));
            }
        }
    }

    private void updateInfrastructureRestrictions(Map<String, Object> restriction) {
        String infrastructureId = (String) restriction.get("infrastructureId");
        Integer newMaxWeight = (Integer) restriction.get("maxWeight");
        Integer newMaxHeight = (Integer) restriction.get("maxHeight");

        Infrastructure infrastructure = infrastructureRepository.findByExternalId(infrastructureId);

        if (infrastructure != null) {
            boolean restrictionsChanged = false;

            if (newMaxWeight != null && !newMaxWeight.equals(infrastructure.getMaxWeightKg())) {
                infrastructure.setMaxWeightKg(newMaxWeight);
                restrictionsChanged = true;
            }

            if (newMaxHeight != null && !newMaxHeight.equals(infrastructure.getMaxHeightCm())) {
                infrastructure.setMaxHeightCm(newMaxHeight);
                restrictionsChanged = true;
            }

            if (restrictionsChanged) {
                infrastructure.setUpdatedAt(LocalDateTime.now());
                infrastructureRepository.save(infrastructure);

                alertService.createAlert(
                        "Restriction updated for " + infrastructure.getName(),
                        AlertLevel.MEDIUM,
                        null,
                        infrastructure.getId(),
                        "RESTRICTION_CHANGE"
                );

                checkAffectedTransports(infrastructure);
            }
        }
    }

    private void checkAffectedTransports(Infrastructure infrastructure) {
        List<Transport> activeTransports = transportRepository.findActiveWithLocation();

        for (Transport transport : activeTransports) {
            if (isTransportAffected(transport, infrastructure)) {
                alertService.createAlert(
                        "Infrastructure issue affects your route: " + infrastructure.getName(),
                        AlertLevel.HIGH,
                        transport.getId(),
                        infrastructure.getId(),
                        "ROUTE_AFFECTED"
                );

                triggerRouteRecalculation(transport, infrastructure);
            }
        }
    }

    private boolean isTransportAffected(Transport transport, Infrastructure infrastructure) {
        if (transport.getApprovedRoute() != null) {
            List<RouteSegment> segments = transport.getApprovedRoute().getSegments();

            for (RouteSegment segment : segments) {
                if (isSegmentNearInfrastructure(segment, infrastructure)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSegmentNearInfrastructure(RouteSegment segment, Infrastructure infrastructure) {
        if (segment.getFromLatitude() == null || segment.getToLatitude() == null) {
            return false;
        }

        double segmentLat = (segment.getFromLatitude() + segment.getToLatitude()) / 2;
        double segmentLng = (segment.getFromLongitude() + segment.getToLongitude()) / 2;

        double distance = calculateDistance(segmentLat, segmentLng,
                infrastructure.getLatitude(), infrastructure.getLongitude());

        return distance < 5.0; // within 5km
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private void triggerRouteRecalculation(Transport transport, Infrastructure affectedInfrastructure) {
        try {
            // Wywołanie Google Maps Directions API
            Double startLat = transport.getCurrentLatitude();
            Double startLng = transport.getCurrentLongitude();
            Double destLat = getDestinationLatitude(transport);
            Double destLng = getDestinationLongitude(transport);

            if (startLat == null || startLng == null || destLat == null || destLng == null) {
                log.warn("Missing coordinates for transport {} route recalculation", transport.getId());
                return;
            }

            String url = String.format(
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=%f,%f&destination=%f,%f&key=%s&mode=driving&avoid=tolls|ferries|highways",
                    startLat, startLng, destLat, destLng, googleMapsApiKey
            );

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "OK".equals(response.get("status"))) {
                intelligentRouteService.processGoogleMapsRouteResponse(response, transport);
                log.info("Recalculated route for transport {} using Google Maps due to infrastructure issue at {}",
                        transport.getId(), affectedInfrastructure.getName());
            } else {
                log.warn("Google Maps route recalculation failed for transport {}: {}", transport.getId(), response != null ? response.get("status") : "null response");
            }
        } catch (Exception e) {
            log.error("Failed to recalculate route for transport {}", transport.getId(), e);
        }
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
}
