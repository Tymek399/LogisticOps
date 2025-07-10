package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.VehicleSpecification;
import pl.logistic.logisticops.repository.VehicleSpecificationRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleSpecificationService {
    
    private final VehicleSpecificationRepository vehicleSpecRepository;
    
    public List<VehicleSpecification> getAllVehicles() {
        return vehicleSpecRepository.findAll();
    }
    
    public Optional<VehicleSpecification> getVehicleById(Long id) {
        return vehicleSpecRepository.findById(id);
    }
    
    public List<VehicleSpecification> getVehiclesByType(String type) {
        return vehicleSpecRepository.findByType(type);
    }
    
    public VehicleSpecification saveVehicle(VehicleSpecification vehicle) {
        // Przeliczanie obciążenia na oś
        if (vehicle.getAxleCount() != null && vehicle.getTotalWeightKg() != null && vehicle.getAxleCount() > 0) {
            int axleLoad = vehicle.getTotalWeightKg() / vehicle.getAxleCount();
            vehicle.setMaxAxleLoadKg(axleLoad);
        }
        
        return vehicleSpecRepository.save(vehicle);
    }
    
    public void deleteVehicle(Long id) {
        vehicleSpecRepository.deleteById(id);
    }
    
    /**
     * Sprawdza, czy pojazd może przejechać przez określony tunel
     * @param vehicleId ID pojazdu
     * @param tunnelHeightCm wysokość tunelu w cm
     * @return true jeśli pojazd może przejechać, false jeśli nie
     */
    public boolean canPassTunnel(Long vehicleId, int tunnelHeightCm) {
        Optional<VehicleSpecification> vehicle = vehicleSpecRepository.findById(vehicleId);
        return vehicle.map(v -> v.getHeightCm() < tunnelHeightCm).orElse(false);
    }
    
    /**
     * Sprawdza, czy pojazd może przejechać przez określony most
     * @param vehicleId ID pojazdu
     * @param bridgeLoadKg maksymalne obciążenie mostu w kg
     * @return true jeśli pojazd może przejechać, false jeśli nie
     */
    public boolean canPassBridge(Long vehicleId, int bridgeLoadKg) {
        Optional<VehicleSpecification> vehicle = vehicleSpecRepository.findById(vehicleId);
        return vehicle.map(v -> v.getMaxAxleLoadKg() < bridgeLoadKg).orElse(false);
    }
}