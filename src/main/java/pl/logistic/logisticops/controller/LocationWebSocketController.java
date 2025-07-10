package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import pl.logistic.logisticops.Model.VehicleTracking;
import pl.logistic.logisticops.service.LocationService;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LocationWebSocketController {
    
    private final LocationService locationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Obsługuje aktualizacje lokalizacji przesyłane przez WebSocket
     */
    @MessageMapping("/location/update")
    public void updateLocation(Map<String, Object> locationData) {
        Long unitId = Long.valueOf(locationData.get("unitId").toString());
        BigDecimal latitude = new BigDecimal(locationData.get("latitude").toString());
        BigDecimal longitude = new BigDecimal(locationData.get("longitude").toString());
        BigDecimal speed = new BigDecimal(locationData.get("speed").toString());
        Integer heading = (Integer) locationData.get("heading");
        
        // Zapisz lokalizację
        VehicleTracking log = locationService.logUnitLocation(unitId, latitude, longitude, speed, heading);
        
        // Wyślij aktualizację do wszystkich subskrybentów
        messagingTemplate.convertAndSend("/topic/location/unit/" + unitId, log);
        messagingTemplate.convertAndSend("/topic/location/all", log);
    }
    
    /**
     * Pobiera i publikuje ostatnie znane lokalizacje wszystkich jednostek
     */
    @MessageMapping("/location/get-all")
    @SendTo("/topic/location/all-units")
    public Map<String, Object> getAllLastLocations() {
        // Implementacja zwracająca ostatnie lokalizacje wszystkich jednostek
        // Tutaj byłaby logika pobierająca dane z serwisu
        return Map.of("message", "Funkcja w implementacji");
    }
}