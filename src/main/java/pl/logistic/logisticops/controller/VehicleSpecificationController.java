package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.Model.VehicleSpecification;
import pl.logistic.logisticops.service.VehicleSpecificationService;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleSpecificationController {
    
    private final VehicleSpecificationService vehicleService;
    
    @GetMapping
    public ResponseEntity<List<VehicleSpecification>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<VehicleSpecification> getVehicleById(@PathVariable Long id) {
        return vehicleService.getVehicleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<List<VehicleSpecification>> getVehiclesByType(@PathVariable String type) {
        return ResponseEntity.ok(vehicleService.getVehiclesByType(type));
    }
    
    @PostMapping
    public ResponseEntity<VehicleSpecification> createVehicle(@RequestBody VehicleSpecification vehicle) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.saveVehicle(vehicle));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<VehicleSpecification> updateVehicle(@PathVariable Long id, 
                                                            @RequestBody VehicleSpecification vehicle) {
        return vehicleService.getVehicleById(id)
                .map(existingVehicle -> {
                    vehicle.setId(id);
                    return ResponseEntity.ok(vehicleService.saveVehicle(vehicle));
                })
                .orElse(ResponseEntity.notFound().build());
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