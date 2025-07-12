package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.dto.request.IntelligentRouteRequestDTO;
import pl.logistic.logisticops.service.IntelligentRouteService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intelligent-routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IntelligentRouteController {

    private final IntelligentRouteService intelligentRouteService;

    @PostMapping("/generate")
    public ResponseEntity<List<RouteProposalDTO>> generateIntelligentRoutes(
            @Valid @RequestBody IntelligentRouteRequestDTO request) {

        RouteRequestDTO routeRequest = RouteRequestDTO.builder()
                .startLatitude(request.getStartLatitude())
                .startLongitude(request.getStartLongitude())
                .endLatitude(request.getEndLatitude())
                .endLongitude(request.getEndLongitude())
                .startAddress(request.getStartAddress())
                .endAddress(request.getEndAddress())
                .missionId(request.getMissionId())
                .vehicleIds(request.getVehicleIds())
                .plannedDeparture(request.getPlannedDeparture())
                .build();

        return ResponseEntity.ok(intelligentRouteService.generateIntelligentRoutes(routeRequest));
    }

    @PostMapping("/validate/{routeId}")
    public ResponseEntity<Map<String, Object>> validateRoute(@PathVariable Long routeId) {
        RouteProposalDTO route = intelligentRouteService.getRouteById(routeId);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> validation = intelligentRouteService.validateRoute(route);
        return ResponseEntity.ok(validation);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<RouteProposalDTO> getRouteById(@PathVariable Long routeId) {
        RouteProposalDTO route = intelligentRouteService.getRouteById(routeId);
        return route != null ? ResponseEntity.ok(route) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{routeId}/optimize")
    public ResponseEntity<RouteProposalDTO> optimizeRoute(@PathVariable Long routeId) {
        RouteProposalDTO optimized = intelligentRouteService.optimizeExistingRoute(routeId);
        return ResponseEntity.ok(optimized);
    }
}