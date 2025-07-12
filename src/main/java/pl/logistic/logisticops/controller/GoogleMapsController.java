package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.RouteSegmentDTO;
import pl.logistic.logisticops.service.GoogleMapsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GoogleMapsController {

    private final GoogleMapsService googleMapsService;

    @GetMapping("/directions")
    public ResponseEntity<List<RouteSegmentDTO>> getDirections(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam Double endLat,
            @RequestParam Double endLng,
            @RequestParam(required = false) Integer maxHeight,
            @RequestParam(required = false) Integer maxWeight,
            @RequestParam(required = false) Integer maxAxleLoad,
            @RequestParam(required = false) Boolean avoidRestrictions) {

        Map<String, Object> constraints = new HashMap<>();
        if (maxHeight != null) {
            constraints.put("maxHeight", maxHeight);
        }
        if (maxWeight != null) {
            constraints.put("maxWeight", maxWeight);
        }
        if (maxAxleLoad != null) {
            constraints.put("maxAxleLoad", maxAxleLoad);
        }
        if (avoidRestrictions != null) {
            constraints.put("avoidRestrictions", avoidRestrictions);
        }

        List<RouteSegmentDTO> route = googleMapsService.getOptimalRoute(
                startLat, startLng, endLat, endLng, constraints
        );
        return ResponseEntity.ok(route);
    }

    @GetMapping("/directions/alternative")
    public ResponseEntity<List<RouteSegmentDTO>> getAlternativeDirections(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam Double endLat,
            @RequestParam Double endLng,
            @RequestParam(required = false) Integer maxHeight,
            @RequestParam(required = false) Integer maxWeight,
            @RequestParam(required = false) Integer maxAxleLoad,
            @RequestParam(required = false) Boolean avoidRestrictions) {

        Map<String, Object> constraints = new HashMap<>();
        if (maxHeight != null) {
            constraints.put("maxHeight", maxHeight);
        }
        if (maxWeight != null) {
            constraints.put("maxWeight", maxWeight);
        }
        if (maxAxleLoad != null) {
            constraints.put("maxAxleLoad", maxAxleLoad);
        }
        if (avoidRestrictions != null) {
            constraints.put("avoidRestrictions", avoidRestrictions);
        }

        List<RouteSegmentDTO> route = googleMapsService.getAlternativeRoute(
                startLat, startLng, endLat, endLng, constraints
        );
        return ResponseEntity.ok(route);
    }

    @GetMapping("/geocode")
    public ResponseEntity<Map<String, Object>> geocodeAddress(@RequestParam String address) {
        return ResponseEntity.ok(googleMapsService.geocodeAddress(address));
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<Map<String, Object>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return ResponseEntity.ok(googleMapsService.reverseGeocode(latitude, longitude));
    }

    @GetMapping("/distance-matrix")
    public ResponseEntity<Map<String, Object>> getDistanceMatrix(
            @RequestParam List<String> origins,
            @RequestParam List<String> destinations) {
        return ResponseEntity.ok(googleMapsService.getDistanceMatrix(origins, destinations));
    }

    @GetMapping("/traffic")
    public ResponseEntity<Map<String, Object>> getTrafficInfo(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam Double endLat,
            @RequestParam Double endLng) {
        return ResponseEntity.ok(googleMapsService.getTrafficInfo(startLat, startLng, endLat, endLng));
    }
}
