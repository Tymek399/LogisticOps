package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.dto.request.CreateTransportRequestDTO;
import pl.logistic.logisticops.dto.request.UpdateTransportLocationRequestDTO;
import pl.logistic.logisticops.enums.TransportStatus;
import pl.logistic.logisticops.service.TransportService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/transports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransportController {

    private final TransportService transportService;

    @GetMapping
    public ResponseEntity<Page<TransportDTO>> getAllTransports(Pageable pageable) {
        return ResponseEntity.ok(transportService.getAllTransports(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<TransportDTO>> getActiveTransports() {
        return ResponseEntity.ok(transportService.getActiveTransports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportDTO> getTransportById(@PathVariable Long id) {
        return transportService.getTransportById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<List<TransportDTO>> getTransportsByMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(transportService.getTransportsByMission(missionId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransportDTO>> getTransportsByStatus(@PathVariable TransportStatus status) {
        return ResponseEntity.ok(transportService.getTransportsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<TransportDTO> createTransport(
            @Valid @RequestBody CreateTransportRequestDTO request,
            @RequestHeader("User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transportService.createTransport(request, userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TransportDTO> updateTransportStatus(
            @PathVariable Long id,
            @RequestParam TransportStatus status) {
        return ResponseEntity.ok(transportService.updateTransportStatus(id, status));
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<TransportDTO> updateTransportLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransportLocationRequestDTO request) {
        return ResponseEntity.ok(transportService.updateTransportLocation(id, request));
    }

    @PostMapping("/{id}/approve-route/{routeId}")
    public ResponseEntity<TransportDTO> approveRoute(
            @PathVariable Long id,
            @PathVariable Long routeId) {
        return ResponseEntity.ok(transportService.approveRoute(id, routeId));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<List<VehicleTrackingDTO>> getTransportTracking(@PathVariable Long id) {
        return ResponseEntity.ok(transportService.getTransportTracking(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransport(@PathVariable Long id) {
        transportService.deleteTransport(id);
        return ResponseEntity.noContent().build();
    }
}
