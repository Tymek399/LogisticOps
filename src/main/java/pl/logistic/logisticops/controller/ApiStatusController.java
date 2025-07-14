package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.service.ApiHealthCheckService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 🩺 API STATUS MONITORING CONTROLLER
 *
 * Endpoints:
 * - GET  /api/status        - Overall API health
 * - GET  /api/status/detail - Detailed API status
 * - POST /api/status/test   - Manual API test
 */
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiStatusController {

    private final ApiHealthCheckService apiHealthCheck;

    /**
     * 📊 Overall system API health
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getOverallStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean hasMaps = apiHealthCheck.hasWorkingMapsApi();
        boolean hasTraffic = apiHealthCheck.hasWorkingTrafficApi();

        status.put("overall", hasMaps && hasTraffic ? "HEALTHY" :
                hasMaps || hasTraffic ? "DEGRADED" : "CRITICAL");
        status.put("hasWorkingMapsApi", hasMaps);
        status.put("hasWorkingTrafficApi", hasTraffic);
        status.put("timestamp", LocalDateTime.now());

        // System capabilities based on API availability
        Map<String, String> capabilities = new HashMap<>();
        capabilities.put("routing", hasMaps ? "AVAILABLE" : "FALLBACK_ONLY");
        capabilities.put("trafficMonitoring", hasTraffic ? "REAL_TIME" : "DISABLED");
        capabilities.put("incidentDetection", hasTraffic ? "ACTIVE" : "MANUAL_ONLY");
        capabilities.put("routeOptimization", hasMaps ? "INTELLIGENT" : "STATIC");

        status.put("capabilities", capabilities);

        return ResponseEntity.ok(status);
    }

    /**
     * 🔍 Detailed API status breakdown
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getDetailedStatus() {
        Map<String, Object> response = new HashMap<>();

        Map<String, Boolean> apiStatuses = apiHealthCheck.getAllApiStatuses();

        // Add details for each API
        Map<String, Map<String, Object>> details = new HashMap<>();

        // Google Maps details
        Map<String, Object> googleMaps = new HashMap<>();
        googleMaps.put("status", apiStatuses.getOrDefault("GOOGLE_MAPS", false) ? "WORKING" : "NOT_WORKING");
        googleMaps.put("services", Map.of(
                "directions", apiStatuses.getOrDefault("GOOGLE_MAPS", false),
                "geocoding", apiStatuses.getOrDefault("GOOGLE_MAPS", false),
                "distanceMatrix", apiStatuses.getOrDefault("GOOGLE_MAPS", false)
        ));
        googleMaps.put("description", "Primary routing and geocoding service");
        details.put("googleMaps", googleMaps);

        // TomTom details
        Map<String, Object> tomTom = new HashMap<>();
        tomTom.put("status", apiStatuses.getOrDefault("TOMTOM", false) ? "WORKING" : "NOT_WORKING");
        tomTom.put("services", Map.of(
                "trafficFlow", apiStatuses.getOrDefault("TOMTOM", false),
                "incidentDetection", apiStatuses.getOrDefault("TOMTOM", false),
                "routingWithTraffic", apiStatuses.getOrDefault("TOMTOM", false)
        ));
        tomTom.put("description", "Real-time traffic monitoring and incidents");
        details.put("tomTom", tomTom);

        // HERE details (if configured)
        Map<String, Object> here = new HashMap<>();
        here.put("status", apiStatuses.getOrDefault("HERE", false) ? "WORKING" : "NOT_CONFIGURED");
        here.put("services", Map.of(
                "places", apiStatuses.getOrDefault("HERE", false),
                "routing", apiStatuses.getOrDefault("HERE", false)
        ));
        here.put("description", "Backup mapping and places service");
        details.put("here", here);

        response.put("apis", details);
        response.put("summary", apiStatuses);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 🧪 Manual API test trigger
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> triggerManualTest() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Trigger manual health check
            apiHealthCheck.periodicHealthCheck();

            result.put("message", "Manual API test completed");
            result.put("status", "SUCCESS");
            result.put("timestamp", LocalDateTime.now());
            result.put("newStatus", apiHealthCheck.getAllApiStatuses());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("message", "Manual API test failed: " + e.getMessage());
            result.put("status", "ERROR");
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 🚨 System operational mode based on API availability
     */
    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getOperationalMode() {
        Map<String, Object> mode = new HashMap<>();

        boolean hasMaps = apiHealthCheck.hasWorkingMapsApi();
        boolean hasTraffic = apiHealthCheck.hasWorkingTrafficApi();

        if (hasMaps && hasTraffic) {
            mode.put("mode", "FULL_OPERATIONAL");
            mode.put("description", "All systems operational with real-time data");
            mode.put("features", Map.of(
                    "intelligentRouting", true,
                    "realTimeTrafficMonitoring", true,
                    "incidentDetection", true,
                    "routeOptimization", true,
                    "infrastructureSync", true
            ));
        } else if (hasMaps) {
            mode.put("mode", "ROUTING_ONLY");
            mode.put("description", "Routing available, traffic monitoring disabled");
            mode.put("features", Map.of(
                    "intelligentRouting", true,
                    "realTimeTrafficMonitoring", false,
                    "incidentDetection", false,
                    "routeOptimization", true,
                    "infrastructureSync", false
            ));
        } else if (hasTraffic) {
            mode.put("mode", "MONITORING_ONLY");
            mode.put("description", "Traffic monitoring available, routing uses fallback");
            mode.put("features", Map.of(
                    "intelligentRouting", false,
                    "realTimeTrafficMonitoring", true,
                    "incidentDetection", true,
                    "routeOptimization", false,
                    "infrastructureSync", false
            ));
        } else {
            mode.put("mode", "FALLBACK");
            mode.put("description", "Static data only - no external APIs available");
            mode.put("features", Map.of(
                    "intelligentRouting", false,
                    "realTimeTrafficMonitoring", false,
                    "incidentDetection", false,
                    "routeOptimization", false,
                    "infrastructureSync", false
            ));
        }

        mode.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(mode);
    }
}