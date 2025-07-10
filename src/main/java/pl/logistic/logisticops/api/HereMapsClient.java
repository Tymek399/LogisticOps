package pl.logistic.logisticops.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.Model.RouteSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HereMapsClient {
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    
    public HereMapsClient(
            RestTemplate restTemplate,
            @Value("${api.here.key}") String apiKey,
            @Value("${api.here.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Pobiera optymalną trasę z punktu A do B z uwzględnieniem ograniczeń wysokości i obciążenia
     */
    public List<RouteSegment> getOptimalRoute(
            double startLat, 
            double startLon, 
            double endLat, 
            double endLon, 
            int maxHeightCm, 
            int maxAxleLoadKg) {
        
        String url = String.format(
                "%s/routing/v8/routes?transportMode=truck&origin=%f,%f&destination=%f,%f" +
                "&return=polyline,actions,instructions,summary&truck[axleCount]=2" +
                "&truck[height]=%d&truck[grossWeight]=%d&apiKey=%s",
                baseUrl, startLat, startLon, endLat, endLon, 
                maxHeightCm / 100, // Konwersja cm na metry
                maxAxleLoadKg / 1000, // Konwersja kg na tony
                apiKey
        );
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return parseRouteResponse(response, "optimal");
    }
    
    /**
     * Pobiera bezpieczną trasę z punktu A do B (minimalizuje ograniczenia)
     */
    public List<RouteSegment> getSafeRoute(
            double startLat, 
            double startLon, 
            double endLat, 
            double endLon, 
            int maxHeightCm, 
            int maxAxleLoadKg) {
        
        String url = String.format(
                "%s/routing/v8/routes?transportMode=truck&origin=%f,%f&destination=%f,%f" +
                "&avoid=difficult&return=polyline,actions,instructions,summary&truck[axleCount]=2" +
                "&truck[height]=%d&truck[grossWeight]=%d&apiKey=%s",
                baseUrl, startLat, startLon, endLat, endLon, 
                maxHeightCm / 100, // Konwersja cm na metry
                maxAxleLoadKg / 1000, // Konwersja kg na tony
                apiKey
        );
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return parseRouteResponse(response, "safe");
    }
    
    /**
     * Pobiera alternatywną trasę z punktu A do B
     */
    public List<RouteSegment> getAlternativeRoute(
            double startLat, 
            double startLon, 
            double endLat, 
            double endLon, 
            int maxHeightCm, 
            int maxAxleLoadKg) {
        
        String url = String.format(
                "%s/routing/v8/routes?transportMode=truck&origin=%f,%f&destination=%f,%f" +
                "&alternatives=1&return=polyline,actions,instructions,summary&truck[axleCount]=2" +
                "&truck[height]=%d&truck[grossWeight]=%d&apiKey=%s",
                baseUrl, startLat, startLon, endLat, endLon, 
                maxHeightCm / 100, // Konwersja cm na metry
                maxAxleLoadKg / 1000, // Konwersja kg na tony
                apiKey
        );
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return parseRouteResponse(response, "alternative");
    }
    
    /**
     * Parsuje odpowiedź z HERE Maps API na listę segmentów trasy
     */
    @SuppressWarnings("unchecked")
    private List<RouteSegment> parseRouteResponse(Map<String, Object> response, String routeType) {
        List<RouteSegment> segments = new ArrayList<>();
        
        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            
            if (routes != null && !routes.isEmpty()) {
                Map<String, Object> route = routes.get(0);
                List<Map<String, Object>> sections = (List<Map<String, Object>>) route.get("sections");
                
                for (Map<String, Object> section : sections) {
                    Map<String, Object> summary = (Map<String, Object>) section.get("summary");
                    double distanceKm = ((Number) summary.get("length")).doubleValue() / 1000; // m na km
                    double durationMin = ((Number) summary.get("duration")).doubleValue() / 60; // s na min
                    
                    List<Map<String, Object>> spans = (List<Map<String, Object>>) section.get("spans");
                    
                    if (spans != null && !spans.isEmpty()) {
                        for (int i = 0; i < spans.size(); i++) {
                            Map<String, Object> span = spans.get(i);
                            
                            String roadName = (String) span.get("names");
                            if (roadName == null) {
                                roadName = "Segment " + i;
                            }
                            
                            String from = i == 0 ? "Start" : "Point " + i;
                            String to = i == spans.size() - 1 ? "Destination" : "Point " + (i + 1);
                            
                            double segmentDistanceKm = ((Number) span.get("length")).doubleValue() / 1000;
                            double segmentDurationMin = ((Number) span.get("duration")).doubleValue() / 60;
                            
                            String roadCondition = "NORMAL";
                            if (span.containsKey("speedLimit")) {
                                roadCondition = "SPEED_LIMIT_" + span.get("speedLimit");
                            }
                            
                            RouteSegment segment = RouteSegment.builder()
                                    .from(from)
                                    .to(to)
                                    .distanceKm(segmentDistanceKm)
                                    .estimatedTimeMin(segmentDurationMin)
                                    .roadCondition(roadCondition)
                                    .build();
                            
                            segments.add(segment);
                        }
                    } else {
                        // Jeśli nie ma danych o odcinkach, dodaj cały segment jako jeden odcinek
                        RouteSegment segment = RouteSegment.builder()
                                .from("Start")
                                .to("Destination")
                                .distanceKm(distanceKm)
                                .estimatedTimeMin(durationMin)
                                .roadCondition("NORMAL")
                                .build();
                        
                        segments.add(segment);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas parsowania odpowiedzi HERE Maps API", e);
        }
        
        return segments;
    }
}