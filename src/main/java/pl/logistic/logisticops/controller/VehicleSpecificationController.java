package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.VehicleSpecificationDTO;
import pl.logistic.logisticops.dto.request.CreateVehicleRequestDTO;
import pl.logistic.logisticops.enums.VehicleType;
import pl.logistic.logisticops.service.VehicleSpecificationService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleSpecificationController {

    private final VehicleSpecificationService vehicleService;

    @GetMapping
    public ResponseEntity<Page<VehicleSpecificationDTO>> getAllVehicles(Pageable pageable) {
        return ResponseEntity.ok(vehicleService.getAllVehicles(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleSpecificationDTO> getVehicleById(@PathVariable Long id) {
        return vehicleService.getVehicleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<VehicleSpecificationDTO>> getVehiclesByType(@PathVariable VehicleType type) {
        return ResponseEntity.ok(vehicleService.getVehiclesByType(type));
    }

    @GetMapping("/active")
    public ResponseEntity<List<VehicleSpecificationDTO>> getActiveVehicles() {
        return ResponseEntity.ok(vehicleService.getActiveVehicles());
    }

    @PostMapping
    public ResponseEntity<VehicleSpecificationDTO> createVehicle(
            @Valid @RequestBody CreateVehicleRequestDTO request) {

        VehicleSpecificationDTO vehicleDTO = VehicleSpecificationDTO.builder()
                .model(request.getModel())
                .type(request.getType())
                .totalWeightKg(request.getTotalWeightKg())
                .axleCount(request.getAxleCount())
                .maxAxleLoadKg(request.getMaxAxleLoadKg())
                .heightCm(request.getHeightCm())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .description(request.getDescription())
                .active(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createVehicle(vehicleDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleSpecificationDTO> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody CreateVehicleRequestDTO request) {

        VehicleSpecificationDTO vehicleDTO = VehicleSpecificationDTO.builder()
                .model(request.getModel())
                .type(request.getType())
                .totalWeightKg(request.getTotalWeightKg())
                .axleCount(request.getAxleCount())
                .maxAxleLoadKg(request.getMaxAxleLoadKg())
                .heightCm(request.getHeightCm())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .description(request.getDescription())
                .build();

        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicleDTO));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VehicleSpecificationDTO> updateVehicleStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return ResponseEntity.ok(vehicleService.updateVehicleStatus(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/can-pass-tunnel")
    public ResponseEntity<Boolean> canPassTunnel(@PathVariable Long id, @RequestParam int heightCm) {
        return ResponseEntity.ok(vehicleService.canPassTunnel(id, heightCm));
    }

    @GetMapping("/{id}/can-pass-bridge")
    public ResponseEntity<Boolean> canPassBridge(@PathVariable Long id, @RequestParam int loadKg) {
        return ResponseEntity.ok(vehicleService.canPassBridge(id, loadKg));
    }
}