package pl.logistic.logisticops.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TomTomTrafficClient {
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    
    public TomTomTrafficClient(
            RestTemplate restTemplate,
            @Value("${api.tomtom.key}") String apiKey,
            @Value("${api.tomtom.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Pobiera informacje o natężeniu ruchu na danym odcinku drogi
     */
    public Map<String, Object> getTrafficInfo(double startLat, double startLon, double endLat, double endLon) {
        String url = String.format(
                "%s/traffic/services/4/flowSegmentData/absolute/10/json?point=%f,%f&unit=KMPH&key=%s",
                baseUrl, startLat, startLon, apiKey
        );
        
        return restTemplate.getForObject(url, Map.class);
    }
    
    /**
     * Sprawdza czy na trasie są incydenty (wypadki, utrudnienia)
     */
    public List<Map<String, Object>> getTrafficIncidents(double startLat, double startLon, double endLat, double endLon, int radius) {
        String url = String.format(
                "%s/traffic/services/5/incidentDetails?bbox=%f,%f,%f,%f&fields={incidents{type,geometry{type,coordinates},properties{iconCategory}}}&key=%s",
                baseUrl, 
                Math.min(startLat, endLat) - 0.1, 
                Math.min(startLon, endLon) - 0.1,
                Math.max(startLat, endLat) + 0.1, 
                Math.max(startLon, endLon) + 0.1,
                apiKey
        );
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return (List<Map<String, Object>>) ((Map<String, Object>) response.get("incidents"));
    }
    
    /**
     * Oblicza czas przejazdu dla danej trasy na podstawie aktualnych danych o ruchu
     */
    public Map<String, Object> calculateTravelTime(List<Map<String, Double>> waypoints) {
        StringBuilder waypointsStr = new StringBuilder();
        
        for (Map<String, Double> waypoint : waypoints) {
            waypointsStr.append(waypoint.get("latitude"))
                    .append(",")
                    .append(waypoint.get("longitude"))
                    .append(":");
        }
        
        // Usuń ostatni dwukropek
        if (waypointsStr.length() > 0) {
            waypointsStr.deleteCharAt(waypointsStr.length() - 1);
        }
        
        String url = String.format(
                "%s/routing/calculateRoute/%s/json?traffic=true&travelMode=truck&key=%s",
                baseUrl, waypointsStr, apiKey
        );
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        // Wyodrębnij tylko potrzebne informacje
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> routeSummary = (Map<String, Object>) 
                    ((List<Map<String, Object>>) ((Map<String, Object>) response.get("routes")).get("route")).get(0).get("summary");
            
            result.put("travelTimeMinutes", ((Number) routeSummary.get("travelTimeInSeconds")).intValue() / 60);
            result.put("trafficDelayMinutes", ((Number) routeSummary.get("trafficDelayInSeconds")).intValue() / 60);
            result.put("distanceKm", ((Number) routeSummary.get("lengthInMeters")).intValue() / 1000.0);
        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas parsowania odpowiedzi TomTom API", e);
        }
        
        return result;
    }
}