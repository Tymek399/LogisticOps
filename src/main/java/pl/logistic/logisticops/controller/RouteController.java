package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.Model.RouteProposal;
import pl.logistic.logisticops.service.RouteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    
    private final RouteService routeService;
    
    @PostMapping("/generate")
    public ResponseEntity<List<RouteProposal>> generateRoutes(
            @RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<Long> vehicleIds = (List<Long>) request.get("vehicleIds");
        double startLat = (double) request.get("startLat");
        double startLon = (double) request.get("startLon");
        double endLat = (double) request.get("endLat");
        double endLon = (double) request.get("endLon");
        Long missionId = Long.valueOf(request.get("missionId").toString());
        
        List<RouteProposal> proposals = routeService.generateRouteProposals(
                vehicleIds, startLat, startLon, endLat, endLon, missionId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(proposals);
    }
    
    @GetMapping("/mission/{missionId}")
    public ResponseEntity<List<RouteProposal>> getRouteProposalsForMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(routeService.getRouteProposalsForMission(missionId));
    }
    
    @PostMapping("/{proposalId}/approve")
    public ResponseEntity<RouteProposal> approveRouteProposal(@PathVariable Long proposalId) {
        return ResponseEntity.ok(routeService.approveRouteProposal(proposalId));
    }
}