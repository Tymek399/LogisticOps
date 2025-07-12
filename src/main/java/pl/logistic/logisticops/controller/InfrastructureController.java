// InfrastructureController.java
package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.InfrastructureDTO;
import pl.logistic.logisticops.dto.request.CreateInfrastructureRequestDTO;
import pl.logistic.logisticops.service.InfrastructureService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InfrastructureController {

    private final InfrastructureService infrastructureService;

    @GetMapping
    public ResponseEntity<Page<InfrastructureDTO>> getAllInfrastructure(Pageable pageable) {
        return ResponseEntity.ok(infrastructureService.getAllInfrastructure(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InfrastructureDTO> getInfrastructureById(@PathVariable Long id) {
        return infrastructureService.getInfrastructureById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<InfrastructureDTO>> getInfrastructureByType(@PathVariable String type) {
        return ResponseEntity.ok(infrastructureService.getInfrastructureByType(type));
    }

    @GetMapping("/active")
    public ResponseEntity<List<InfrastructureDTO>> getActiveInfrastructure() {
        return ResponseEntity.ok(infrastructureService.getActiveInfrastructure());
    }

    @GetMapping("/near")
    public ResponseEntity<List<InfrastructureDTO>> getInfrastructureNearPoint(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {
        return ResponseEntity.ok(infrastructureService.getInfrastructureNearPoint(latitude, longitude, radiusKm));
    }

    @GetMapping("/restrictions")
    public ResponseEntity<List<InfrastructureDTO>> getRestrictiveInfrastructure(
            @RequestParam(required = false) Integer maxHeightCm,
            @RequestParam(required = false) Integer maxWeightKg,
            @RequestParam(required = false) Integer maxAxleWeightKg) {
        return ResponseEntity.ok(infrastructureService.getRestrictiveInfrastructure(maxHeightCm, maxWeightKg, maxAxleWeightKg));
    }

    @PostMapping
    public ResponseEntity<InfrastructureDTO> createInfrastructure(
            @Valid @RequestBody CreateInfrastructureRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(infrastructureService.createInfrastructure(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InfrastructureDTO> updateInfrastructure(
            @PathVariable Long id,
            @Valid @RequestBody CreateInfrastructureRequestDTO request) {
        return ResponseEntity.ok(infrastructureService.updateInfrastructure(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InfrastructureDTO> updateInfrastructureStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(infrastructureService.updateInfrastructureStatus(id, isActive));
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncInfrastructureData() {
        infrastructureService.syncInfrastructureData();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInfrastructure(@PathVariable Long id) {
        infrastructureService.deleteInfrastructure(id);
        return ResponseEntity.noContent().build();
    }
}