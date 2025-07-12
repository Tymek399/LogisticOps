package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.model.MilitaryGpsData;
import pl.logistic.logisticops.service.MilitaryGpsIntegrationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/military-gps")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
class MilitaryGpsController {

    private final MilitaryGpsIntegrationService militaryGpsService;

    /**
     * 📡 Endpoint dla urządzeń GPS wojskowych
     */
    @PostMapping("/position")
    public ResponseEntity<Map<String, String>> receiveGpsPosition(
            @RequestBody MilitaryGpsData gpsData,
            @RequestHeader("Authorization") String authToken) {

        // Walidacja tokenu autoryzacji
        if (!isValidMilitaryToken(authToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "UNAUTHORIZED", "message", "Invalid military token"));
        }

        // Przetwórz dane GPS
        militaryGpsService.receiveSecureGpsData(gpsData);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "GPS position received and processed",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 📊 Status urządzeń GPS w terenie
     */
    @GetMapping("/devices/status")
    public ResponseEntity<List<Map<String, Object>>> getActiveDevicesStatus() {
        // Zwróć status aktywnych urządzeń GPS
        List<Map<String, Object>> devices = List.of(
                Map.of("deviceId", "GTX-12345678", "status", "ACTIVE", "lastUpdate", "2025-07-12T18:30:00"),
                Map.of("deviceId", "DAGR-87654321", "status", "ACTIVE", "lastUpdate", "2025-07-12T18:29:45"),
                Map.of("deviceId", "SOTAS-11223344", "status", "OFFLINE", "lastUpdate", "2025-07-12T18:15:30")
        );

        return ResponseEntity.ok(devices);
    }

    private boolean isValidMilitaryToken(String token) {
        // Walidacja tokenu wojskowego (symulacja)
        return token != null && token.startsWith("Bearer MIL-");
    }
}
