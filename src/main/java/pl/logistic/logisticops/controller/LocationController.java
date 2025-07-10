package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.Model.VehicleTracking;
import pl.logistic.logisticops.service.LocationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    
    private final LocationService locationService;
    
    @PostMapping("/log")
    public ResponseEntity<VehicleTracking> logUnitLocation(@RequestBody Map<String, Object> locationData) {
        Long unitId = Long.valueOf(locationData.get("unitId").toString());
        BigDecimal latitude = new BigDecimal(locationData.get("latitude").toString());
        BigDecimal longitude = new BigDecimal(locationData.get("longitude").toString());
        BigDecimal speed = new BigDecimal(locationData.get("speed").toString());
        Integer heading = (Integer) locationData.get("heading");
        
        return ResponseEntity.ok(locationService.logUnitLocation(unitId, latitude, longitude, speed, heading));
    }
    
    @GetMapping("/unit/{unitId}/history")
    public ResponseEntity<List<VehicleTracking>> getUnitLocationHistory(
            @PathVariable Long unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        
        return ResponseEntity.ok(locationService.getUnitLocationHistory(unitId, from, to));
    }
    
    @GetMapping("/unit/{unitId}/last")
    public ResponseEntity<VehicleTracking> getLastKnownLocation(@PathVariable Long unitId) {
        VehicleTracking location = locationService.getLastKnownLocation(unitId);
        return location != null ? ResponseEntity.ok(location) : ResponseEntity.notFound().build();
    }
}