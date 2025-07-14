package pl.logistic.logisticops.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
public class TomTomTrafficClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final DecimalFormat coordinateFormat;

    public TomTomTrafficClient(
            RestTemplate restTemplate,
            @Value("${api.tomtom.key}") String apiKey,
            @Value("${api.tomtom.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;

        // Ensure we always use dot as decimal separator for coordinates
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.coordinateFormat = new DecimalFormat("#.######", symbols);
    }

    /**
     * 📊 Pobiera informacje o natężeniu ruchu na danym punkcie
     * FIXED: Proper point format for TomTom API
     */
    public Map<String, Object> getTrafficInfo(double lat, double lon) {
        try {
            // Format coordinates properly (always use dot as decimal separator)
            String latStr = coordinateFormat.format(lat);
            String lonStr = coordinateFormat.format(lon);

            String url = String.format(
                    "%s/traffic/services/4/flowSegmentData/absolute/10/json?point=%s,%s&unit=KMPH&key=%s",
                    baseUrl, latStr, lonStr, apiKey
            );

            log.debug("🔗 TomTom Traffic URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : Collections.emptyMap();

        } catch (Exception e) {
            log.warn("⚠️ Failed to get traffic info from TomTom: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 📊 Overloaded method for backward compatibility
     */
    public Map<String, Object> getTrafficInfo(double startLat, double startLon, double endLat, double endLon) {
        // Use the start point for traffic info (TomTom Flow API works with single points)
        return getTrafficInfo(startLat, startLon);
    }

    /**
     * 🚨 Sprawdza incydenty drogowe w danym obszarze
     * FIXED: Proper bbox format for TomTom API
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTrafficIncidents(double startLat, double startLon,
                                                         double endLat, double endLon, int radiusKm) {
        try {
            // Calculate bounding box with proper formatting
            double minLat = Math.min(startLat, endLat) - (radiusKm * 0.01);
            double maxLat = Math.max(startLat, endLat) + (radiusKm * 0.01);
            double minLon = Math.min(startLon, endLon) - (radiusKm * 0.01);
            double maxLon = Math.max(startLon, endLon) + (radiusKm * 0.01);

            String url = String.format(
                    "%s/traffic/services/5/incidentDetails?bbox=%s,%s,%s,%s&" +
                            "fields={incidents{type,geometry{type,coordinates},properties{iconCategory,description,endTime}}}&" +
                            "key=%s",
                    baseUrl,
                    coordinateFormat.format(minLon), coordinateFormat.format(minLat),
                    coordinateFormat.format(maxLon), coordinateFormat.format(maxLat),
                    apiKey
            );

            log.debug("🔗 TomTom Incidents URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("incidents")) {
                return (List<Map<String, Object>>) response.get("incidents");
            }

        } catch (Exception e) {
            log.warn("⚠️ Failed to get traffic incidents from TomTom: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * ⏱️ Oblicza czas przejazdu z uwzględnieniem aktualnego ruchu
     * FIXED: Proper waypoints format
     */
    public Map<String, Object> calculateTravelTime(List<Map<String, Double>> waypoints) {
        try {
            if (waypoints == null || waypoints.size() < 2) {
                return createErrorResponse("At least 2 waypoints required");
            }

            StringBuilder waypointsStr = new StringBuilder();

            for (Map<String, Double> waypoint : waypoints) {
                Double lat = waypoint.get("latitude");
                Double lon = waypoint.get("longitude");

                if (lat != null && lon != null) {
                    waypointsStr.append(coordinateFormat.format(lat))
                            .append(",")
                            .append(coordinateFormat.format(lon))
                            .append(":");
                }
            }

            if (waypointsStr.length() > 0) {
                waypointsStr.deleteCharAt(waypointsStr.length() - 1);
            }

            String url = String.format(
                    "%s/routing/1/calculateRoute/%s/json?traffic=true&travelMode=truck&key=%s",
                    baseUrl, waypointsStr, apiKey
            );

            log.debug("🔗 TomTom Routing URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return processRouteResponse(response);

        } catch (Exception e) {
            log.error("❌ Error calculating travel time with TomTom", e);
            return createErrorResponse("Travel time calculation failed: " + e.getMessage());
        }
    }

    /**
     * 🛣️ Wyszukuje alternatywne trasy omijające incydenty
     */
    public Map<String, Object> getAlternativeRoute(double startLat, double startLon,
                                                   double endLat, double endLon,
                                                   List<String> avoidTypes) {
        try {
            StringBuilder avoidParams = new StringBuilder();
            if (avoidTypes != null && !avoidTypes.isEmpty()) {
                avoidParams.append("&avoid=");
                avoidParams.append(String.join(",", avoidTypes));
            }

            String url = String.format(
                    "%s/routing/1/calculateRoute/%s,%s:%s,%s/json?" +
                            "traffic=true&travelMode=truck&computeAlternatives=true%s&key=%s",
                    baseUrl,
                    coordinateFormat.format(startLat), coordinateFormat.format(startLon),
                    coordinateFormat.format(endLat), coordinateFormat.format(endLon),
                    avoidParams, apiKey
            );

            log.debug("🔗 TomTom Alternative Route URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : Collections.emptyMap();

        } catch (Exception e) {
            log.warn("⚠️ Failed to get alternative route from TomTom: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 🚧 Sprawdza ograniczenia dla pojazdów ciężkich
     */
    public Map<String, Object> checkTruckRestrictions(double lat, double lon,
                                                      int weightKg, int heightCm, int lengthCm) {
        try {
            // Convert height from cm to meters for TomTom API
            double heightM = heightCm / 100.0;
            double lengthM = lengthCm / 100.0;
            double weightKg_d = weightKg;

            String url = String.format(
                    "%s/routing/1/calculateRoute/%s,%s:%s,%s/json?" +
                            "travelMode=truck&vehicleWeight=%s&vehicleHeight=%s&vehicleLength=%s&key=%s",
                    baseUrl,
                    coordinateFormat.format(lat), coordinateFormat.format(lon),
                    coordinateFormat.format(lat + 0.01), coordinateFormat.format(lon + 0.01),
                    coordinateFormat.format(weightKg_d), coordinateFormat.format(heightM),
                    coordinateFormat.format(lengthM), apiKey
            );

            log.debug("🔗 TomTom Truck Restrictions URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return analyzeRestrictions(response);

        } catch (Exception e) {
            log.warn("⚠️ Failed to check truck restrictions: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 📍 Wyszukuje najbliższe stacje paliw/punkty obsługi
     */
    public List<Map<String, Object>> findNearbyServices(double lat, double lon,
                                                        String serviceType, int radiusKm) {
        try {
            String url = String.format(
                    "%s/search/2/poiSearch/%s.json?lat=%s&lon=%s&radius=%d&key=%s",
                    baseUrl, serviceType,
                    coordinateFormat.format(lat), coordinateFormat.format(lon),
                    radiusKm * 1000, apiKey // radius w metrach
            );

            log.debug("🔗 TomTom POI Search URL: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("results")) {
                return (List<Map<String, Object>>) response.get("results");
            }

        } catch (Exception e) {
            log.warn("⚠️ Failed to find nearby services: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 🔍 Testuje dostępność API TomTom
     * FIXED: Use proper coordinates for Warsaw center
     */
    public boolean testApiConnection() {
        try {
            log.debug("🧪 Testing TomTom API connection...");

            // Test call - sprawdź ruch w centrum Warszawy (52.2297, 21.0122)
            Map<String, Object> result = getTrafficInfo(52.2297, 21.0122);
            boolean isWorking = result != null && !result.isEmpty();

            if (isWorking) {
                log.info("✅ TomTom API connection successful");
                log.debug("📊 Sample response: {}", result.keySet());
            } else {
                log.warn("⚠️ TomTom API connection failed - empty response");
            }

            return isWorking;

        } catch (Exception e) {
            log.error("❌ TomTom API connection test failed: {}", e.getMessage());
            return false;
        }
    }

    // === POMOCNICZE METODY PRYWATNE ===

    @SuppressWarnings("unchecked")
    private Map<String, Object> processRouteResponse(Map<String, Object> response) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (response != null && response.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");

                if (!routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    Map<String, Object> summary = (Map<String, Object>) route.get("summary");

                    if (summary != null) {
                        result.put("travelTimeMinutes",
                                ((Number) summary.get("travelTimeInSeconds")).intValue() / 60);
                        result.put("trafficDelayMinutes",
                                ((Number) summary.getOrDefault("trafficDelayInSeconds", 0)).intValue() / 60);
                        result.put("distanceKm",
                                ((Number) summary.get("lengthInMeters")).doubleValue() / 1000.0);
                        result.put("fuelConsumptionLiters",
                                ((Number) summary.getOrDefault("fuelConsumptionInLiters", 0)).doubleValue());

                        result.put("status", "SUCCESS");
                    }
                }
            } else {
                result.put("status", "NO_ROUTES");
                result.put("message", "No routes found in response");
            }
        } catch (Exception e) {
            log.error("❌ Error processing TomTom route response", e);
            result.put("status", "ERROR");
            result.put("message", "Failed to process route response: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> analyzeRestrictions(Map<String, Object> response) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (response != null && response.containsKey("routes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");

                boolean hasRestrictions = routes.isEmpty();
                result.put("hasRestrictions", hasRestrictions);
                result.put("routeAvailable", !hasRestrictions);

                if (!hasRestrictions && !routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    result.put("routeInfo", route.get("summary"));
                }

                result.put("status", "SUCCESS");
            } else {
                result.put("hasRestrictions", true);
                result.put("routeAvailable", false);
                result.put("status", "NO_ROUTES");
            }
        } catch (Exception e) {
            log.warn("⚠️ Error analyzing restrictions: {}", e.getMessage());
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> createErrorResponse() {
        return createErrorResponse("Failed to calculate travel time");
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("status", "ERROR");
        error.put("message", message);
        error.put("travelTimeMinutes", 0);
        error.put("distanceKm", 0.0);
        return error;
    }
}