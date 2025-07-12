package pl.logistic.logisticops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.dto.RouteSegmentDTO;
import pl.logistic.logisticops.dto.TransportConstraintsDTO;
import pl.logistic.logisticops.model.Infrastructure;
import pl.logistic.logisticops.repository.InfrastructureRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleMapsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final InfrastructureRepository infrastructureRepository;

    @Value("${api.googlemaps.key}")
    private String apiKey;

    @Value("${app.restrictions.critical-height-cm:400}")
    private Integer criticalHeightCm;

    @Value("${app.restrictions.critical-weight-kg:45000}")
    private Integer criticalWeightKg;

    /**
     * 🎯 INTELIGENTNE PLANOWANIE TRAS z unikaniem ograniczeń
     * Łączy Google Maps z naszą bazą infrastruktury
     */
    public List<RouteSegmentDTO> getOptimalRoute(Double startLat, Double startLng,
                                                 Double endLat, Double endLng,
                                                 Map<String, Object> constraints) {
        return getIntelligentRoute(startLat, startLng, endLat, endLng, constraints, false);
    }

    public List<RouteSegmentDTO> getAlternativeRoute(Double startLat, Double startLng,
                                                     Double endLat, Double endLng,
                                                     Map<String, Object> constraints) {
        return getIntelligentRoute(startLat, startLng, endLat, endLng, constraints, true);
    }

    /**
     * 🧠 GŁÓWNA LOGIKA INTELIGENTNEGO ROUTINGU
     * 1. Sprawdza ograniczenia infrastrukturalne
     * 2. Generuje trasę z omijaniem problemów
     * 3. Waliduje trasę względem naszej bazy danych
     */
    private List<RouteSegmentDTO> getIntelligentRoute(Double startLat, Double startLng,
                                                      Double endLat, Double endLng,
                                                      Map<String, Object> constraints,
                                                      boolean alternative) {
        try {
            log.info("🧠 Planning intelligent route from [{},{}] to [{},{}]",
                    startLat, startLng, endLat, endLng);

            // 1. Identyfikuj problematyczną infrastrukturę w obszarze
            List<Infrastructure> problematicInfra = findProblematicInfrastructure(
                    startLat, startLng, endLat, endLng, constraints);

            log.info("⚠️ Found {} problematic infrastructure objects", problematicInfra.size());

            // 2. Generuj trasę z Google Maps z unikaniem problemów
            List<RouteSegmentDTO> baseRoute = getGoogleMapsRoute(
                    startLat, startLng, endLat, endLng, constraints, alternative, problematicInfra);

            // 3. Waliduj i popraw trasę
            List<RouteSegmentDTO> validatedRoute = validateAndFixRoute(baseRoute, constraints);

            log.info("✅ Generated route with {} segments", validatedRoute.size());
            return validatedRoute;

        } catch (Exception e) {
            log.error("❌ Error in intelligent routing", e);
            // Fallback: prosta trasa bez ograniczeń
            return getSimpleGoogleRoute(startLat, startLng, endLat, endLng);
        }
    }

    /**
     * 🔍 Znajdź problematyczną infrastrukturę w obszarze trasy
     */
    private List<Infrastructure> findProblematicInfrastructure(Double startLat, Double startLng,
                                                               Double endLat, Double endLng,
                                                               Map<String, Object> constraints) {
        // Rozszerz bounding box o margines bezpieczeństwa (20km)
        double margin = 0.2; // ~20km
        double minLat = Math.min(startLat, endLat) - margin;
        double maxLat = Math.max(startLat, endLat) + margin;
        double minLng = Math.min(startLng, endLng) - margin;
        double maxLng = Math.max(startLng, endLng) + margin;

        // Pobierz infrastrukturę w obszarze
        List<Infrastructure> areaInfra = infrastructureRepository.findNearPoint(
                (minLat + maxLat) / 2, (minLng + maxLng) / 2, 50.0); // 50km radius

        // Filtruj problematyczną infrastrukturę
        return areaInfra.stream()
                .filter(infra -> isProblematicForConstraints(infra, constraints))
                .toList();
    }

    /**
     * 🚫 Sprawdź czy infrastruktura jest problematyczna dla zestawu
     */
    private boolean isProblematicForConstraints(Infrastructure infra, Map<String, Object> constraints) {
        Integer maxHeight = (Integer) constraints.get("maxHeight");
        Integer maxWeight = (Integer) constraints.get("maxWeight");
        Integer maxAxleLoad = (Integer) constraints.get("maxAxleLoad");

        // Sprawdź ograniczenia wysokości
        if (infra.getMaxHeightCm() != null && maxHeight != null) {
            if (maxHeight > infra.getMaxHeightCm()) {
                log.debug("⚠️ Height restriction: {} requires {}cm, limit is {}cm",
                        infra.getName(), maxHeight, infra.getMaxHeightCm());
                return true;
            }
        }

        // Sprawdź ograniczenia wagi
        if (infra.getMaxWeightKg() != null && maxWeight != null) {
            if (maxWeight > infra.getMaxWeightKg()) {
                log.debug("⚠️ Weight restriction: {} requires {}kg, limit is {}kg",
                        infra.getName(), maxWeight, infra.getMaxWeightKg());
                return true;
            }
        }

        // Sprawdź ograniczenia obciążenia osi
        if (infra.getMaxAxleWeightKg() != null && maxAxleLoad != null) {
            if (maxAxleLoad > infra.getMaxAxleWeightKg()) {
                log.debug("⚠️ Axle load restriction: {} requires {}kg, limit is {}kg",
                        infra.getName(), maxAxleLoad, infra.getMaxAxleWeightKg());
                return true;
            }
        }

        return false;
    }

    /**
     * 🗺️ Pobierz trasę z Google Maps z unikaniem ograniczeń
     */
    private List<RouteSegmentDTO> getGoogleMapsRoute(Double startLat, Double startLng,
                                                     Double endLat, Double endLng,
                                                     Map<String, Object> constraints,
                                                     boolean alternative,
                                                     List<Infrastructure> problematicInfra) {
        try {
            String origin = startLat + "," + startLng;
            String destination = endLat + "," + endLng;

            StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
            urlBuilder.append("origin=").append(URLEncoder.encode(origin, StandardCharsets.UTF_8));
            urlBuilder.append("&destination=").append(URLEncoder.encode(destination, StandardCharsets.UTF_8));
            urlBuilder.append("&key=").append(apiKey);
            urlBuilder.append("&alternatives=").append(alternative ? "true" : "false");
            urlBuilder.append("&mode=driving");

            // Dodaj unikanie określonych punktów (jeśli to możliwe)
            if (!problematicInfra.isEmpty()) {
                urlBuilder.append("&avoid=tolls"); // Podstawowe unikanie

                // Dla bardzo problematycznych obiektów dodaj waypoints omijające
                String avoidWaypoints = generateAvoidanceWaypoints(problematicInfra, startLat, startLng, endLat, endLng);
                if (!avoidWaypoints.isEmpty()) {
                    urlBuilder.append("&waypoints=").append(avoidWaypoints);
                }
            }

            String url = urlBuilder.toString();
            log.debug("🔗 Google Maps URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("❌ Google Maps API error: {}", response.getStatusCode());
                return Collections.emptyList();
            }

            return parseGoogleMapsResponse(response.getBody());

        } catch (Exception e) {
            log.error("❌ Error calling Google Maps API", e);
            return Collections.emptyList();
        }
    }

    /**
     * 🔄 Generuj waypoint'y omijające problematyczną infrastrukturę
     */
    private String generateAvoidanceWaypoints(List<Infrastructure> problematicInfra,
                                              Double startLat, Double startLng,
                                              Double endLat, Double endLng) {
        StringBuilder waypoints = new StringBuilder();

        // Znajdź najbardziej problematyczne obiekty w środku trasy
        for (Infrastructure infra : problematicInfra) {
            if (isInRouteMiddle(infra, startLat, startLng, endLat, endLng)) {
                // Generuj punkt omijający (10km na wschód lub zachód)
                double avoidLat = infra.getLatitude();
                double avoidLng = infra.getLongitude() + 0.1; // ~10km na wschód

                if (waypoints.length() > 0) {
                    waypoints.append("|");
                }
                waypoints.append(avoidLat).append(",").append(avoidLng);

                // Maksymalnie 8 waypoints (limit Google Maps)
                if (waypoints.toString().split("\\|").length >= 8) {
                    break;
                }
            }
        }

        return URLEncoder.encode(waypoints.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 📍 Sprawdź czy infrastruktura jest w środkowej części trasy
     */
    private boolean isInRouteMiddle(Infrastructure infra, Double startLat, Double startLng,
                                    Double endLat, Double endLng) {
        // Proste sprawdzenie czy punkt jest w środkowej 1/3 prostej łączącej start-end
        double routeProgress = calculateRouteProgress(infra.getLatitude(), infra.getLongitude(),
                startLat, startLng, endLat, endLng);
        return routeProgress > 0.2 && routeProgress < 0.8; // Środkowe 60% trasy
    }

    private double calculateRouteProgress(Double pointLat, Double pointLng,
                                          Double startLat, Double startLng,
                                          Double endLat, Double endLng) {
        // Simplified projection onto route line
        double totalDist = calculateDistance(startLat, startLng, endLat, endLng);
        double startToPt = calculateDistance(startLat, startLng, pointLat, pointLng);
        return Math.min(1.0, startToPt / totalDist);
    }

    /**
     * ✅ Waliduj i popraw trasę względem naszej bazy infrastruktury
     */
    private List<RouteSegmentDTO> validateAndFixRoute(List<RouteSegmentDTO> route,
                                                      Map<String, Object> constraints) {
        List<RouteSegmentDTO> validatedRoute = new ArrayList<>();

        for (RouteSegmentDTO segment : route) {
            // Sprawdź czy segment przechodzi przez problematyczną infrastrukturę
            List<Infrastructure> conflictingInfra = checkSegmentConflicts(segment, constraints);

            if (conflictingInfra.isEmpty()) {
                // Segment bezpieczny - dodaj bez zmian
                validatedRoute.add(segment);
            } else {
                // Segment problematyczny - dodaj z ostrzeżeniem
                RouteSegmentDTO warningSegment = RouteSegmentDTO.builder()
                        .id(segment.getId())
                        .routeProposalId(segment.getRouteProposalId())
                        .sequenceOrder(segment.getSequenceOrder())
                        .fromLocation(segment.getFromLocation())
                        .toLocation(segment.getToLocation())
                        .fromLatitude(segment.getFromLatitude())
                        .fromLongitude(segment.getFromLongitude())
                        .toLatitude(segment.getToLatitude())
                        .toLongitude(segment.getToLongitude())
                        .distanceKm(segment.getDistanceKm())
                        .estimatedTimeMin(segment.getEstimatedTimeMin())
                        .roadCondition("RESTRICTED") // ⚠️ Zmieniony status
                        .roadName(segment.getRoadName())
                        .polyline(segment.getPolyline())
                        .build();
                validatedRoute.add(warningSegment);

                log.warn("⚠️ Route segment has {} restrictions: {}",
                        conflictingInfra.size(), segment.getRoadName());
            }
        }

        return validatedRoute;
    }

    /**
     * 🔍 Sprawdź konflikty segmentu z infrastrukturą
     */
    private List<Infrastructure> checkSegmentConflicts(RouteSegmentDTO segment,
                                                       Map<String, Object> constraints) {
        if (segment.getFromLatitude() == null || segment.getToLatitude() == null) {
            return Collections.emptyList();
        }

        // Sprawdź infrastrukturę w okolicy segmentu (2km)
        double centerLat = (segment.getFromLatitude() + segment.getToLatitude()) / 2;
        double centerLng = (segment.getFromLongitude() + segment.getToLongitude()) / 2;

        List<Infrastructure> nearbyInfra = infrastructureRepository.findNearPoint(centerLat, centerLng, 2.0);

        return nearbyInfra.stream()
                .filter(infra -> isProblematicForConstraints(infra, constraints))
                .toList();
    }

    /**
     * 📄 Parsuj odpowiedź z Google Maps API
     */
    private List<RouteSegmentDTO> parseGoogleMapsResponse(String jsonResponse) {
        List<RouteSegmentDTO> segments = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            String status = root.path("status").asText();

            if (!"OK".equals(status)) {
                log.error("❌ Google Maps API error: {}", status);
                return segments;
            }

            JsonNode routes = root.path("routes");
            if (routes.isEmpty()) {
                return segments;
            }

            JsonNode route = routes.get(0);
            JsonNode legs = route.path("legs");

            int sequenceOrder = 0;
            for (JsonNode leg : legs) {
                JsonNode steps = leg.path("steps");

                for (JsonNode step : steps) {
                    RouteSegmentDTO segment = RouteSegmentDTO.builder()
                            .sequenceOrder(sequenceOrder++)
                            .fromLatitude(step.path("start_location").path("lat").asDouble())
                            .fromLongitude(step.path("start_location").path("lng").asDouble())
                            .toLatitude(step.path("end_location").path("lat").asDouble())
                            .toLongitude(step.path("end_location").path("lng").asDouble())
                            .distanceKm(step.path("distance").path("value").asDouble() / 1000.0)
                            .estimatedTimeMin(step.path("duration").path("value").asDouble() / 60.0)
                            .roadName(extractRoadName(step.path("html_instructions").asText()))
                            .roadCondition("NORMAL")
                            .polyline(step.path("polyline").path("points").asText())
                            .fromLocation(leg.path("start_address").asText())
                            .toLocation(leg.path("end_address").asText())
                            .build();

                    segments.add(segment);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error parsing Google Maps response", e);
        }

        return segments;
    }

    /**
     * 🛣️ Prosta trasa fallback (bez inteligencji)
     */
    private List<RouteSegmentDTO> getSimpleGoogleRoute(Double startLat, Double startLng,
                                                       Double endLat, Double endLng) {
        try {
            String origin = startLat + "," + startLng;
            String destination = endLat + "," + endLng;

            String url = String.format(
                    "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s",
                    URLEncoder.encode(origin, StandardCharsets.UTF_8),
                    URLEncoder.encode(destination, StandardCharsets.UTF_8),
                    apiKey
            );

            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return parseGoogleMapsResponse(response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ Error in simple Google route", e);
        }

        // Ultimate fallback - linia prosta
        return createFallbackRoute(startLat, startLng, endLat, endLng);
    }

    /**
     * 🆘 Ultimate fallback - linia prosta
     */
    private List<RouteSegmentDTO> createFallbackRoute(Double startLat, Double startLng,
                                                      Double endLat, Double endLng) {
        double distance = calculateDistance(startLat, startLng, endLat, endLng);
        double estimatedTime = distance / 60.0; // 60 km/h average

        RouteSegmentDTO segment = RouteSegmentDTO.builder()
                .sequenceOrder(0)
                .fromLatitude(startLat)
                .fromLongitude(startLng)
                .toLatitude(endLat)
                .toLongitude(endLng)
                .distanceKm(distance)
                .estimatedTimeMin(estimatedTime)
                .roadCondition("UNKNOWN")
                .roadName("Direct Route (Fallback)")
                .fromLocation("Start Point")
                .toLocation("End Point")
                .build();

        log.warn("🆘 Using fallback direct route: {:.1f}km", distance);
        return List.of(segment);
    }

    // ========================================
    // ADDITIONAL METHODS (for compatibility)
    // ========================================

    /**
     * 🔄 Fetch raw route from Google Maps (used by existing IntelligentRouteService)
     */
    public String fetchRouteFromGoogleMaps(Double startLat, Double startLng,
                                           Double endLat, Double endLng,
                                           Map<String, Object> constraints) {
        try {
            String origin = startLat + "," + startLng;
            String destination = endLat + "," + endLng;

            StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
            urlBuilder.append("origin=").append(URLEncoder.encode(origin, StandardCharsets.UTF_8));
            urlBuilder.append("&destination=").append(URLEncoder.encode(destination, StandardCharsets.UTF_8));
            urlBuilder.append("&key=").append(apiKey);
            urlBuilder.append("&mode=driving");

            String url = urlBuilder.toString();
            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("❌ Error fetching raw route from Google Maps", e);
        }

        return "{}"; // Empty JSON fallback
    }

    /**
     * 🔧 Process Google Maps route response (returns route segments)
     */
    public List<RouteSegmentDTO> processGoogleMapsRouteResponse(String jsonResponse) {
        return parseGoogleMapsResponse(jsonResponse);
    }

    public Map<String, Object> geocodeAddress(String address) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                    "address=" + URLEncoder.encode(address, StandardCharsets.UTF_8) +
                    "&key=" + apiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }

        } catch (Exception e) {
            log.error("❌ Error in geocoding", e);
        }

        return Collections.emptyMap();
    }

    public Map<String, Object> reverseGeocode(Double latitude, Double longitude) {
        try {
            String latlng = latitude + "," + longitude;
            String url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                    "latlng=" + URLEncoder.encode(latlng, StandardCharsets.UTF_8) +
                    "&key=" + apiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }

        } catch (Exception e) {
            log.error("❌ Error in reverse geocoding", e);
        }

        return Collections.emptyMap();
    }

    public Map<String, Object> getDistanceMatrix(List<String> origins, List<String> destinations) {
        try {
            String originsParam = String.join("|", origins);
            String destinationsParam = String.join("|", destinations);

            String url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                    "origins=" + URLEncoder.encode(originsParam, StandardCharsets.UTF_8) +
                    "&destinations=" + URLEncoder.encode(destinationsParam, StandardCharsets.UTF_8) +
                    "&key=" + apiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }

        } catch (Exception e) {
            log.error("❌ Error in distance matrix", e);
        }

        return Collections.emptyMap();
    }

    public Map<String, Object> getTrafficInfo(Double startLat, Double startLng,
                                              Double endLat, Double endLng) {
        // Google Maps nie ma dedykowanego traffic API
        // Można użyć TomTom lub HERE dla traffic data
        return Collections.emptyMap();
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

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

    private String extractRoadName(String htmlInstructions) {
        if (htmlInstructions == null) return "Unknown Road";

        // Usuń HTML tags i wyciągnij nazwę drogi
        String cleanText = htmlInstructions.replaceAll("<[^>]*>", " ");

        // Szukaj wzorców dróg
        if (cleanText.contains("A1") || cleanText.contains("A2") || cleanText.contains("A4")) {
            return cleanText.substring(cleanText.indexOf("A"), cleanText.indexOf("A") + 2);
        }
        if (cleanText.contains("S7") || cleanText.contains("S8") || cleanText.contains("S2")) {
            return cleanText.substring(cleanText.indexOf("S"), cleanText.indexOf("S") + 2);
        }
        if (cleanText.contains("DK")) {
            int start = cleanText.indexOf("DK");
            return cleanText.substring(start, Math.min(start + 4, cleanText.length()));
        }

        return cleanText.length() > 50 ? cleanText.substring(0, 50) + "..." : cleanText;
    }
}