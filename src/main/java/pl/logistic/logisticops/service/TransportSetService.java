package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.TransportSetDTO;
import pl.logistic.logisticops.mapper.TransportSetMapper;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportSetService {

    private final TransportSetRepository transportSetRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final TransportSetMapper transportSetMapper;

    public List<TransportSetDTO> getAllTransportSets() {
        return transportSetRepository.findAll()
                .stream()
                .map(transportSetMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<TransportSetDTO> getTransportSetById(Long id) {
        return transportSetRepository.findById(id)
                .map(transportSetMapper::toDTO);
    }

    public TransportSetDTO createTransportSet(Long transporterId, Long cargoId, String description) {
        VehicleSpecification transporter = vehicleRepository.findById(transporterId)
                .orElseThrow(() -> new IllegalArgumentException("Transporter not found"));

        VehicleSpecification cargo = vehicleRepository.findById(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("Cargo not found"));

        TransportSet transportSet = TransportSet.builder()
                .transporter(transporter)
                .cargo(cargo)
                .description(description)
                .build();

        transportSet = transportSetRepository.save(transportSet);
        return transportSetMapper.toDTO(transportSet);
    }

    public void deleteTransportSet(Long id) {
        transportSetRepository.deleteById(id);
    }

    // Legacy method compatibility
    public TransportSet addObstacle(Long id, Object obstacle) {
        // This method was in the original controller but doesn't match the new model
        // Return the transport set without modification for now
        return transportSetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transport set not found"));
    }

    public boolean canPassObstacle(Long transportSetId, Long obstacleId) {
        // Simplified implementation - always return true for now
        return true;
    }
}