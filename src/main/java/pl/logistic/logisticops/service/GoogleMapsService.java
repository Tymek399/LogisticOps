package pl.logistic.logisticops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.dto.RouteSegmentDTO;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class GoogleMapsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Wstaw swój klucz API Google Maps
    private final String apiKey = "YOUR_GOOGLE_MAPS_API_KEY";

    public List<RouteSegmentDTO> getOptimalRoute(Double startLat, Double startLng, Double endLat, Double endLng, Map<String, Object> constraints) {
        return getRoute(startLat, startLng, endLat, endLng, constraints, false);
    }

    public List<RouteSegmentDTO> getAlternativeRoute(Double startLat, Double startLng, Double endLat, Double endLng, Map<String, Object> constraints) {
        return getRoute(startLat, startLng, endLat, endLng, constraints, true);
    }

    private List<RouteSegmentDTO> getRoute(Double startLat, Double startLng, Double endLat, Double endLng, Map<String, Object> constraints, boolean alternatives) {
        try {
            String origin = startLat + "," + startLng;
            String destination = endLat + "," + endLng;

            StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
            urlBuilder.append("origin=").append(URLEncoder.encode(origin, StandardCharsets.UTF_8));
            urlBuilder.append("&destination=").append(URLEncoder.encode(destination, StandardCharsets.UTF_8));
            urlBuilder.append("&key=").append(apiKey);
            urlBuilder.append("&alternatives=").append(alternatives);

            // Tu możesz dodać obsługę constraints np. avoid=tolls|highways itp.
            // Na razie ignorujemy constraints lub możesz rozbudować

            String url = urlBuilder.toString();

            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Google Directions API returned status: {}", response.getStatusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("status").asText();

            if (!"OK".equals(status)) {
                log.error("Google Directions API error: {}", status);
                return Collections.emptyList();
            }

            // Bierzemy pierwszą trasę (lub wszystkie, jeśli chcesz rozszerzyć)
            JsonNode routes = root.path("routes");
            if (routes.isEmpty()) {
                return Collections.emptyList();
            }

            JsonNode route = routes.get(0);
            JsonNode legs = route.path("legs");
            if (legs.isEmpty()) {
                return Collections.emptyList();
            }

            JsonNode leg = legs.get(0);
            JsonNode steps = leg.path("steps");

            List<RouteSegmentDTO> segments = new ArrayList<>();
            int order = 0;

            for (JsonNode step : steps) {
                RouteSegmentDTO segment = new RouteSegmentDTO();

                segment.setSequenceOrder(order++);
                segment.setFromLatitude(step.path("start_location").path("lat").asDouble());
                segment.setFromLongitude(step.path("start_location").path("lng").asDouble());
                segment.setToLatitude(step.path("end_location").path("lat").asDouble());
                segment.setToLongitude(step.path("end_location").path("lng").asDouble());

                segment.setFromLocation(step.path("html_instructions").asText());  // tu możesz chcieć wyczyścić html
                segment.setToLocation(""); // Google Directions nie podaje "toLocation" tekstowo osobno

                segment.setDistanceKm(step.path("distance").path("value").asDouble() / 1000.0);
                segment.setEstimatedTimeMin(step.path("duration").path("value").asDouble() / 60.0);

                segment.setRoadName(step.path("html_instructions").asText()); // można dodać lepsze mapowanie na nazwę drogi
                segment.setRoadCondition(null);
                segment.setPolyline(step.path("polyline").path("points").asText());

                segments.add(segment);
            }

            return segments;

        } catch (Exception e) {
            log.error("Exception in getRoute", e);
            return Collections.emptyList();
        }
    }

    public Map<String, Object> geocodeAddress(String address) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                    "address=" + URLEncoder.encode(address, StandardCharsets.UTF_8) +
                    "&key=" + apiKey;
            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Google Geocode API returned status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(root, Map.class);

        } catch (Exception e) {
            log.error("Exception in geocodeAddress", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> reverseGeocode(Double latitude, Double longitude) {
        try {
            String latlng = latitude + "," + longitude;
            String url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                    "latlng=" + URLEncoder.encode(latlng, StandardCharsets.UTF_8) +
                    "&key=" + apiKey;
            ResponseEntity<String> response = restTemplate.getForEntity(new URI(url), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Google Reverse Geocode API returned status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(root, Map.class);

        } catch (Exception e) {
            log.error("Exception in reverseGeocode", e);
            return Collections.emptyMap();
        }
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

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Google Distance Matrix API returned status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(root, Map.class);

        } catch (Exception e) {
            log.error("Exception in getDistanceMatrix", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> getTrafficInfo(Double startLat, Double startLng, Double endLat, Double endLng) {
        // Google Maps API nie oferuje osobnego endpointu "traffic info" przez REST w Directions API.
        // Zazwyczaj ruch jest zawarty w danych trasy (duration_in_traffic).
        // Możesz zaimplementować własną logikę lub użyć innego API.
        // Na potrzeby przykładu zwrócimy pustą mapę.

        return Collections.emptyMap();
    }
}
