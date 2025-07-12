package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.InfrastructureDTO;
import pl.logistic.logisticops.dto.request.CreateInfrastructureRequestDTO;
import pl.logistic.logisticops.mapper.InfrastructureMapper;
import pl.logistic.logisticops.model.Infrastructure;
import pl.logistic.logisticops.repository.InfrastructureRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InfrastructureService {

    private final InfrastructureRepository infrastructureRepository;
    private final PolishInfrastructureService polishInfrastructureService;
    private final SimpMessagingTemplate messagingTemplate;
    private final InfrastructureMapper infrastructureMapper;

    public Page<InfrastructureDTO> getAllInfrastructure(Pageable pageable) {
        return infrastructureRepository.findAll(pageable)
                .map(infrastructureMapper::toDTO);
    }

    public Optional<InfrastructureDTO> getInfrastructureById(Long id) {
        return infrastructureRepository.findById(id)
                .map(infrastructureMapper::toDTO);
    }

    public List<InfrastructureDTO> getInfrastructureByType(String type) {
        return infrastructureRepository.findByType(type)
                .stream()
                .map(infrastructureMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<InfrastructureDTO> getActiveInfrastructure() {
        return infrastructureRepository.findByIsActiveTrue()
                .stream()
                .map(infrastructureMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<InfrastructureDTO> getInfrastructureNearPoint(Double latitude, Double longitude, Double radiusKm) {
        return infrastructureRepository.findNearPoint(latitude, longitude, radiusKm)
                .stream()
                .map(infrastructureMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<InfrastructureDTO> getRestrictiveInfrastructure(Integer maxHeightCm, Integer maxWeightKg, Integer maxAxleWeightKg) {
        return infrastructureRepository.findPotentialRestrictions(maxHeightCm, maxWeightKg, maxAxleWeightKg)
                .stream()
                .map(infrastructureMapper::toDTO)
                .collect(Collectors.toList());
    }

    public InfrastructureDTO createInfrastructure(CreateInfrastructureRequestDTO request) {
        Infrastructure infrastructure = Infrastructure.builder()
                .externalId(request.getExternalId())
                .name(request.getName())
                .type(request.getType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .roadNumber(request.getRoadNumber())
                .maxHeightCm(request.getMaxHeightCm())
                .maxWeightKg(request.getMaxWeightKg())
                .maxAxleWeightKg(request.getMaxAxleWeightKg())
                .description(request.getDescription())
                .isActive(true)
                .build();

        infrastructure = infrastructureRepository.save(infrastructure);

        InfrastructureDTO dto = infrastructureMapper.toDTO(infrastructure);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/infrastructure/new", dto);

        return dto;
    }

    public InfrastructureDTO updateInfrastructure(Long id, CreateInfrastructureRequestDTO request) {
        Infrastructure infrastructure = infrastructureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Infrastructure not found"));

        infrastructure.setName(request.getName());
        infrastructure.setType(request.getType());
        infrastructure.setLatitude(request.getLatitude());
        infrastructure.setLongitude(request.getLongitude());
        infrastructure.setRoadNumber(request.getRoadNumber());
        infrastructure.setMaxHeightCm(request.getMaxHeightCm());
        infrastructure.setMaxWeightKg(request.getMaxWeightKg());
        infrastructure.setMaxAxleWeightKg(request.getMaxAxleWeightKg());
        infrastructure.setDescription(request.getDescription());

        infrastructure = infrastructureRepository.save(infrastructure);

        InfrastructureDTO dto = infrastructureMapper.toDTO(infrastructure);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/infrastructure/updated", dto);

        return dto;
    }

    public InfrastructureDTO updateInfrastructureStatus(Long id, Boolean isActive) {
        Infrastructure infrastructure = infrastructureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Infrastructure not found"));

        Boolean oldStatus = infrastructure.getIsActive();
        infrastructure.setIsActive(isActive);

        infrastructure = infrastructureRepository.save(infrastructure);

        InfrastructureDTO dto = infrastructureMapper.toDTO(infrastructure);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/infrastructure/status-changed",
                Map.of("infrastructure", dto, "oldStatus", oldStatus, "newStatus", isActive));

        return dto;
    }

    public void syncInfrastructureData() {
        polishInfrastructureService.manualSync();

        // Send notification that sync is complete
        messagingTemplate.convertAndSend("/topic/infrastructure/sync-complete",
                Map.of("timestamp", System.currentTimeMillis(), "status", "completed"));
    }

    public void deleteInfrastructure(Long id) {
        Infrastructure infrastructure = infrastructureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Infrastructure not found"));

        infrastructureRepository.delete(infrastructure);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/infrastructure/deleted",
                Map.of("infrastructureId", id));
    }
}
