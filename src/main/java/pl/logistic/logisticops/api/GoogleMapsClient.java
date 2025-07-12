package pl.logistic.logisticops.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.model.RouteSegment;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GoogleMapsClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public GoogleMapsClient(
            RestTemplate restTemplate,
            @Value("${api.googlemaps.key:DEMO_KEY}") String apiKey,
            @Value("${api.googlemaps.url:https://maps.googleapis.com/maps/api}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public List<RouteSegment> getOptimalRoute(
            double startLat,
            double startLon,
            double endLat,
            double endLon,
            int maxHeightCm,
            int maxAxleLoadKg) {

        try {
            // Budujemy URL Directions API z parametrami origin, destination i mode=truck (jeśli potrzebne)
            // Google Directions API nie ma dedykowanych parametrów wysokości czy obciążenia osi,
            // więc przekazujemy tryb transportu jako "driving" lub "truck" w nowoczesnym API (tutaj driving)
            // Można też dodać parametry avoid (toll, highway) jeśli chcemy

            String origin = startLat + "," + startLon;
            String destination = endLat + "," + endLon;

            // Kodowanie parametrów URL
            String originEncoded = URLEncoder.encode(origin, StandardCharsets.UTF_8);
            String destinationEncoded = URLEncoder.encode(destination, StandardCharsets.UTF_8);

            // Przykładowy URL - bez możliwości ustawiania maxHeight, maxAxleLoad (Google nie udostępnia tych parametrów)
            String url = String.format(
                    "%s/directions/json?origin=%s&destination=%s&mode=driving&key=%s",
                    baseUrl,
                    originEncoded,
                    destinationEncoded,
                    apiKey
            );

            Map<String, Object> response = restTemplate.getForObject(new URI(url), Map.class);

            return parseRouteResponse(response);
        } catch (Exception e) {
            // Fallback - trasa prosta
            return createFallbackRoute(startLat, startLon, endLat, endLon);
        }
    }

    private List<RouteSegment> createFallbackRoute(double startLat, double startLon, double endLat, double endLon) {
        double distance = calculateDistance(startLat, startLon, endLat, endLon);
        double estimatedTime = distance / 60.0; // załóżmy 60 km/h

        RouteSegment segment = RouteSegment.builder()
                .fromLocation("Start Point")
                .toLocation("End Point")
                .fromLatitude(startLat)
                .fromLongitude(startLon)
                .toLatitude(endLat)
                .toLongitude(endLon)
                .distanceKm(distance)
                .estimatedTimeMin(estimatedTime)
                .roadCondition("NORMAL")
                .roadName("Direct Route")
                .sequenceOrder(0)
                .build();

        return List.of(segment);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    @SuppressWarnings("unchecked")
    private List<RouteSegment> parseRouteResponse(Map<String, Object> response) {
        List<RouteSegment> segments = new ArrayList<>();

        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes != null && !routes.isEmpty()) {
                Map<String, Object> route = routes.get(0);
                List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");

                int sequenceOrder = 0;
                for (Map<String, Object> leg : legs) {
                    String startAddress = (String) leg.get("start_address");
                    String endAddress = (String) leg.get("end_address");

                    double distanceKm = 0.0;
                    double durationMin = 0.0;

                    Map<String, Object> distance = (Map<String, Object>) leg.get("distance");
                    Map<String, Object> duration = (Map<String, Object>) leg.get("duration");

                    if (distance != null) {
                        distanceKm = ((Number) distance.get("value")).doubleValue() / 1000.0; // meters to km
                    }
                    if (duration != null) {
                        durationMin = ((Number) duration.get("value")).doubleValue() / 60.0; // seconds to minutes
                    }

                    RouteSegment segment = RouteSegment.builder()
                            .fromLocation(startAddress)
                            .toLocation(endAddress)
                            .distanceKm(distanceKm)
                            .estimatedTimeMin(durationMin)
                            .roadCondition("NORMAL")
                            .roadName("Google Maps Route")
                            .sequenceOrder(sequenceOrder++)
                            .build();

                    segments.add(segment);
                }
            }
        } catch (Exception e) {
            // parse error - zwróć pustą listę
            return new ArrayList<>();
        }

        return segments;
    }
}
