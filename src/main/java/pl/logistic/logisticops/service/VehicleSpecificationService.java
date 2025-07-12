package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.TransportConstraintsDTO;
import pl.logistic.logisticops.dto.VehicleSpecificationDTO;
import pl.logistic.logisticops.enums.VehicleType;
import pl.logistic.logisticops.mapper.VehicleSpecificationMapper;
import pl.logistic.logisticops.model.VehicleSpecification;
import pl.logistic.logisticops.repository.VehicleSpecificationRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleSpecificationService {

    private final VehicleSpecificationRepository vehicleRepository;
    private final VehicleSpecificationMapper vehicleMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<VehicleSpecificationDTO> getAllVehicles(Pageable pageable) {
        return vehicleRepository.findAll(pageable)
                .map(vehicleMapper::toDTO);
    }

    public Optional<VehicleSpecificationDTO> getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(vehicleMapper::toDTO);
    }

    public List<VehicleSpecificationDTO> getVehiclesByType(VehicleType type) {
        return vehicleRepository.findByType(type.name())
                .stream()
                .map(vehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<VehicleSpecificationDTO> getActiveVehicles() {
        return vehicleRepository.findByActiveTrue()
                .stream()
                .map(vehicleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public VehicleSpecificationDTO createVehicle(VehicleSpecificationDTO vehicleDTO) {
        VehicleSpecification vehicle = vehicleMapper.toEntity(vehicleDTO);

        // Calculate axle load if needed
        if (vehicle.getAxleCount() != null && vehicle.getTotalWeightKg() != null && vehicle.getAxleCount() > 0) {
            int axleLoad = vehicle.getTotalWeightKg() / vehicle.getAxleCount();
            vehicle.setMaxAxleLoadKg(axleLoad);
        }

        vehicle = vehicleRepository.save(vehicle);
        VehicleSpecificationDTO dto = vehicleMapper.toDTO(vehicle);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/vehicles/new", dto);

        return dto;
    }

    public VehicleSpecificationDTO updateVehicle(Long id, VehicleSpecificationDTO vehicleDTO) {
        VehicleSpecification existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        // Update fields
        existingVehicle.setModel(vehicleDTO.getModel());
        existingVehicle.setType(vehicleDTO.getType());
        existingVehicle.setTotalWeightKg(vehicleDTO.getTotalWeightKg());
        existingVehicle.setAxleCount(vehicleDTO.getAxleCount());
        existingVehicle.setMaxAxleLoadKg(vehicleDTO.getMaxAxleLoadKg());
        existingVehicle.setHeightCm(vehicleDTO.getHeightCm());
        existingVehicle.setLengthCm(vehicleDTO.getLengthCm());
        existingVehicle.setWidthCm(vehicleDTO.getWidthCm());
        existingVehicle.setDescription(vehicleDTO.getDescription());

        // Recalculate axle load if needed
        if (existingVehicle.getAxleCount() != null && existingVehicle.getTotalWeightKg() != null && existingVehicle.getAxleCount() > 0) {
            int axleLoad = existingVehicle.getTotalWeightKg() / existingVehicle.getAxleCount();
            existingVehicle.setMaxAxleLoadKg(axleLoad);
        }

        existingVehicle = vehicleRepository.save(existingVehicle);
        VehicleSpecificationDTO dto = vehicleMapper.toDTO(existingVehicle);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/vehicles/updated", dto);

        return dto;
    }

    public VehicleSpecificationDTO updateVehicleStatus(Long id, Boolean active) {
        VehicleSpecification vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        vehicle.setActive(active);
        vehicle = vehicleRepository.save(vehicle);

        VehicleSpecificationDTO dto = vehicleMapper.toDTO(vehicle);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/vehicles/status-changed",
                Map.of("vehicleId", id, "active", active));

        return dto;
    }

    public TransportConstraintsDTO calculateTransportConstraints(List<Long> vehicleIds) {
        List<VehicleSpecification> vehicles = vehicleRepository.findAllById(vehicleIds);

        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException("No vehicles found for provided IDs");
        }

        // Calculate constraints
        Integer maxHeight = vehicles.stream()
                .mapToInt(v -> v.getHeightCm() != null ? v.getHeightCm() : 0)
                .max()
                .orElse(0);

        Integer totalWeight = vehicles.stream()
                .mapToInt(v -> v.getTotalWeightKg() != null ? v.getTotalWeightKg() : 0)
                .sum();

        Integer maxAxleLoad = vehicles.stream()
                .mapToInt(v -> v.getMaxAxleLoadKg() != null ? v.getMaxAxleLoadKg() : 0)
                .max()
                .orElse(0);

        // Find transporter and cargo
        VehicleSpecification transporter = vehicles.stream()
                .filter(v -> v.getType() == VehicleType.TRANSPORTER || v.getType() == VehicleType.LOGISTICS_VEHICLE)
                .findFirst()
                .orElse(null);

        VehicleSpecification cargo = vehicles.stream()
                .filter(v -> v.getType() != VehicleType.TRANSPORTER && v.getType() != VehicleType.LOGISTICS_VEHICLE)
                .findFirst()
                .orElse(null);

        return TransportConstraintsDTO.builder()
                .maxHeightCm(maxHeight)
                .totalWeightKg(totalWeight)
                .maxAxleLoadKg(maxAxleLoad)
                .transporterModel(transporter != null ? transporter.getModel() : "Unknown")
                .cargoModel(cargo != null ? cargo.getModel() : "No cargo")
                .vehicleCount(vehicles.size())
                .description(generateConstraintsDescription(vehicles))
                .build();
    }

    public void deleteVehicle(Long id) {
        VehicleSpecification vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        // Check if vehicle is in use
        if (vehicle.getTransportAssignments() != null && !vehicle.getTransportAssignments().isEmpty()) {
            throw new IllegalStateException("Cannot delete vehicle that is assigned to transports");
        }

        vehicleRepository.delete(vehicle);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/vehicles/deleted",
                Map.of("vehicleId", id));
    }

    public boolean canPassTunnel(Long vehicleId, int tunnelHeightCm) {
        Optional<VehicleSpecification> vehicle = vehicleRepository.findById(vehicleId);
        return vehicle.map(v -> v.getHeightCm() != null && v.getHeightCm() <= tunnelHeightCm).orElse(false);
    }

    public boolean canPassBridge(Long vehicleId, int bridgeMaxAxleLoadKg) {
        Optional<VehicleSpecification> vehicle = vehicleRepository.findById(vehicleId);
        return vehicle.map(v -> v.getMaxAxleLoadKg() != null && v.getMaxAxleLoadKg() <= bridgeMaxAxleLoadKg).orElse(false);
    }

    private String generateConstraintsDescription(List<VehicleSpecification> vehicles) {
        StringBuilder description = new StringBuilder();

        long transporterCount = vehicles.stream()
                .filter(v -> v.getType() == VehicleType.TRANSPORTER || v.getType() == VehicleType.LOGISTICS_VEHICLE)
                .count();

        long cargoCount = vehicles.size() - transporterCount;

        description.append("Transport set: ");
        description.append(transporterCount).append(" transporter(s)");
        if (cargoCount > 0) {
            description.append(" + ").append(cargoCount).append(" cargo unit(s)");
        }

        return description.toString();
    }
}