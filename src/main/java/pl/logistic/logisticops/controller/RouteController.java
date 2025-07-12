package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.service.RouteService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<List<RouteProposalDTO>> getAllRouteProposals() {
        return ResponseEntity.ok(routeService.getAllRouteProposals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteProposalDTO> getRouteProposalById(@PathVariable Long id) {
        return routeService.getRouteProposalById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<List<RouteProposalDTO>> generateRoutes(
            @Valid @RequestBody RouteRequestDTO request) {

        List<RouteProposalDTO> proposals = routeService.generateRouteProposals(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(proposals);
    }

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<List<RouteProposalDTO>> getRouteProposalsForMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(routeService.getRouteProposalsForMission(missionId));
    }

    @PostMapping("/{proposalId}/approve")
    public ResponseEntity<RouteProposalDTO> approveRouteProposal(@PathVariable Long proposalId) {
        return ResponseEntity.ok(routeService.approveRouteProposal(proposalId));
    }

    @PostMapping("/{proposalId}/reject")
    public ResponseEntity<RouteProposalDTO> rejectRouteProposal(@PathVariable Long proposalId) {
        return ResponseEntity.ok(routeService.rejectRouteProposal(proposalId));
    }

    @GetMapping("/{routeId}/obstacles")
    public ResponseEntity<List<RouteObstacleDTO>> getRouteObstacles(@PathVariable Long routeId) {
        return ResponseEntity.ok(routeService.getRouteObstacles(routeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRouteProposal(@PathVariable Long id) {
        routeService.deleteRouteProposal(id);
        return ResponseEntity.noContent().build();
    }
}