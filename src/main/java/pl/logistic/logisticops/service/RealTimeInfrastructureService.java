package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.Model.*;
import pl.logistic.logisticops.enums.AlertLevel;
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

    /**
     * Monitor infrastructure status changes in real-time
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorInfrastructureStatus() {
        log.debug("Monitoring infrastructure status changes...");

        // Check for bridge closures from GDDKiA
        checkBridgeClosures();

        // Check for tunnel restrictions
        checkTunnelRestrictions();

        // Check for temporary weight restrictions
        checkTemporaryRestrictions();

        // Monitor traffic incidents affecting infrastructure
        monitorTrafficIncidents();
    }

    private void checkBridgeClosures() {
        try {
            // Fetch real-time data from GDDKiA incident API
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
            // Monitor tunnel status
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
            // Check for temporary weight/height restrictions
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

    private void monitorTrafficIncidents() {
        try {
            // Get traffic incidents that might affect infrastructure
            String url = "https://api.here.com/traffic/6.3/incidents.json?bbox=49.0,14.1,54.9,24.0";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                processTrafficIncidents(response);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch traffic incidents", e);
        }
    }

    private void processInfrastructureIncident(Map<String, Object> incident, String type) {
        String incidentId = (String) incident.get("id");
        String name = (String) incident.get("name");
        String status = (String) incident.get("status");

        // Find corresponding infrastructure
        Infrastructure infrastructure = infrastructureRepository.findByExternalId("GDDKIA_" + incidentId);

        if (infrastructure != null) {
            boolean wasActive = infrastructure.getIsActive();
            boolean isNowActive = !"CLOSED".equals(status);

            if (wasActive != isNowActive) {
                // Status changed
                infrastructure.setIsActive(isNowActive);
                infrastructure.setUpdatedAt(LocalDateTime.now());
                infrastructureRepository.save(infrastructure);

                // Create alert
                String message = String.format("%s %s - status changed to %s",
                        type.toLowerCase(), name, status);

                Alert alert = alertService.createAlert(
                        message,
                        isNowActive ? AlertLevel.Medium : AlertLevel.High,
                        null,
                        "INFRASTRUCTURE"
                );

                // Check if any active transports are affected
                checkAffectedTransports(infrastructure, alert);

                // Send real-time update
                messagingTemplate.convertAndSend("/topic/infrastructure/status", Map.of(
                        "infrastructureId", infrastructure.getId(),
                        "name", infrastructure.getName(),
                        "type", infrastructure.getType(),
                        "isActive", infrastructure.getIsActive(),
                        "alert", alert
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

                // Alert about restriction changes
                Alert alert = alertService.createAlert(
                        "Restriction updated for " + infrastructure.getName(),
                        AlertLevel.Medium,
                        null,
                        "RESTRICTION_CHANGE"
                );

                // Check affected transports
                checkAffectedTransports(infrastructure, alert);
            }
        }
    }

    private void processTrafficIncidents(Map<String, Object> trafficData) {
        // Process traffic incidents and correlate with infrastructure
        @SuppressWarnings("unchecked")
        Map<String, Object> trafficItems = (Map<String, Object>) trafficData.get("TRAFFIC_ITEMS");

        if (trafficItems != null && trafficItems.containsKey("TRAFFIC_ITEM")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> incidents = (List<Map<String, Object>>) trafficItems.get("TRAFFIC_ITEM");

            for (Map<String, Object> incident : incidents) {
                processTrafficIncident(incident);
            }
        }
    }

    private void processTrafficIncident(Map<String, Object> incident) {
        String trafficItemId = (String) incident.get("TRAFFIC_ITEM_ID");
        String description = (String) incident.get("TRAFFIC_ITEM_DESCRIPTION");

        // Check if incident affects any known infrastructure
        List<Infrastructure> nearbyInfrastructure = findNearbyInfrastructure(incident);

        for (Infrastructure infra : nearbyInfrastructure) {
            Alert alert = alertService.createAlert(
                    "Traffic incident near " + infra.getName() + ": " + description,
                    AlertLevel.Medium,
                    null,
                    "TRAFFIC_INFRASTRUCTURE"
            );

            checkAffectedTransports(infra, alert);
        }
    }

    private List<Infrastructure> findNearbyInfrastructure(Map<String, Object> incident) {
        // Extract coordinates from incident and find nearby infrastructure
        // Simplified implementation
        return infrastructureRepository.findByIsActiveTrue();
    }

    private void checkAffectedTransports(Infrastructure infrastructure, Alert alert) {
        // Find active transports that might be affected by this infrastructure change
        List<Transport> activeTransports = transportRepository.findActiveWithLocation();

        for (Transport transport : activeTransports) {
            if (isTransportAffected(transport, infrastructure)) {
                // Create transport-specific alert
                Alert transportAlert = alertService.createAlert(
                        "Infrastructure issue affects your route: " + alert.getMessage(),
                        AlertLevel.High,
                        transport.getId(),
                        "ROUTE_AFFECTED"
                );

                // Trigger route recalculation
                triggerRouteRecalculation(transport, infrastructure);
            }
        }
    }

    private boolean isTransportAffected(Transport transport, Infrastructure infrastructure) {
        // Check if transport's route passes through or near the affected infrastructure
        // This would involve spatial calculations in a real implementation

        if (transport.getApprovedRoute() != null) {
            // Get route segments and check if any pass near the infrastructure
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
        // Simplified distance calculation
        double segmentLat = (segment.getFromLatitude() + segment.getToLatitude()) / 2;
        double segmentLng = (segment.getFromLongitude() + segment.getToLongitude()) / 2;

        double distance = calculateDistance(segmentLat, segmentLng,
                infrastructure.getLatitude(), infrastructure.getLongitude());

        return distance < 5.0; // Within 5km
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Haversine formula for distance calculation
        double R = 6371; // Earth's radius in km
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
            // Trigger automatic route recalculation
            RouteRequest request = RouteRequest.builder()
                    .startLat(transport.getCurrentLatitude())
                    .startLon(transport.getCurrentLongitude())
                    .endLat(transport.getApprovedRoute().getSegments().get(
                            transport.getApprovedRoute().getSegments().size() - 1).getToLatitude())
                    .endLon(transport.getApprovedRoute().getSegments().get(
                            transport.getApprovedRoute().getSegments().size() - 1).getToLongitude())
                    .missionId(transport.getMission().getId())
                    .transportSetIds(transport.getVehicles().stream()
                            .map(tv -> tv.getVehicle().getId())
                            .toList())
                    .build();

            List<RouteProposal> newRoutes = intelligentRouteService.generateIntelligentRoutes(request);

            if (!newRoutes.isEmpty()) {
                RouteProposal newRoute = newRoutes.get(0);

                // Send route suggestion to operators
                messagingTemplate.convertAndSend("/topic/transport/" + transport.getId() + "/route-suggestion",
                        Map.of(
                                "originalRoute", transport.getApprovedRoute().getId(),
                                "newRoute", newRoute,
                                "reason", "Infrastructure issue: " + affectedInfrastructure.getName(),
                                "affectedInfrastructure", affectedInfrastructure,
                                "requiresApproval", true
                        ));

                log.info("Generated alternative route for transport {} due to infrastructure issue at {}",
                        transport.getId(), affectedInfrastructure.getName());
            }
        } catch (Exception e) {
            log.error("Failed to recalculate route for transport {}", transport.getId(), e);
        }
    }
}
