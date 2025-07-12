package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.MissionDTO;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;
import pl.logistic.logisticops.mapper.MissionMapper;
import pl.logistic.logisticops.model.Mission;
import pl.logistic.logisticops.model.User;
import pl.logistic.logisticops.repository.MissionRepository;
import pl.logistic.logisticops.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final MissionMapper missionMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<MissionDTO> getAllMissions(Pageable pageable) {
        return missionRepository.findAll(pageable)
                .map(missionMapper::toDTO);
    }

    public Optional<MissionDTO> getMissionById(Long id) {
        return missionRepository.findById(id)
                .map(missionMapper::toDTO);
    }

    public Page<MissionDTO> getMissionsByStatus(MissionStatus status, Pageable pageable) {
        return missionRepository.findByStatus(status, pageable)
                .map(missionMapper::toDTO);
    }

    public Page<MissionDTO> getMissionsByUser(Long userId, Pageable pageable) {
        return missionRepository.findByCreatedByUserId(userId, pageable)
                .map(missionMapper::toDTO);
    }

    public MissionDTO createMission(MissionDTO missionDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Mission mission = Mission.builder()
                .name(missionDTO.getName())
                .description(missionDTO.getDescription())
                .startDate(missionDTO.getStartDate())
                .endDate(missionDTO.getEndDate())
                .status(MissionStatus.PLANNED)
                .priority(missionDTO.getPriority() != null ? missionDTO.getPriority() : MissionPriority.MEDIUM)
                .createdByUserId(user.getId())
                .build();

        mission = missionRepository.save(mission);
        MissionDTO dto = missionMapper.toDTO(mission);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/missions/new", dto);

        return dto;
    }

    /**
     * Updates an existing mission with the provided data.
     *
     * @param id The ID of the mission to update.
     * @param missionDTO A DTO containing the new data for the mission.
     * @return A DTO representing the updated mission.
     * @throws ResourceNotFoundException if no mission is found with the given ID.
     */
    public MissionDTO updateMission(Long id, MissionDTO missionDTO) {
        // 1. Find the existing mission in the database by its ID.
        Mission existingMission = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));

        // 2. Update the properties of the existing mission entity with new values from the DTO.
        if (missionDTO.getName() != null) {
            existingMission.setName(missionDTO.getName());
        }
        if (missionDTO.getDescription() != null) {
            existingMission.setDescription(missionDTO.getDescription());
        }
        if (missionDTO.getStartDate() != null) {
            existingMission.setStartDate(missionDTO.getStartDate());
        }
        if (missionDTO.getEndDate() != null) {
            existingMission.setEndDate(missionDTO.getEndDate());
        }
        if (missionDTO.getStatus() != null) {
            existingMission.setStatus(missionDTO.getStatus());
        }
        if (missionDTO.getPriority() != null) {
            existingMission.setPriority(missionDTO.getPriority());
        }

        // 3. Save the updated mission entity back to the database.
        Mission updatedMission = missionRepository.save(existingMission);

        // 4. Map the updated entity back to a DTO.
        MissionDTO updatedDto = missionMapper.toDTO(updatedMission);

        // 5. Send a WebSocket notification about the update.
        messagingTemplate.convertAndSend("/topic/missions/updated", updatedDto);

        // 6. Return the updated DTO.
        return updatedDto;
    }

    public MissionDTO updateMissionStatus(Long id, MissionStatus status) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));

        MissionStatus oldStatus = mission.getStatus();
        mission.setStatus(status);

        if (status == MissionStatus.COMPLETED || status == MissionStatus.CANCELLED) {
            if (mission.getEndDate() == null) {
                mission.setEndDate(LocalDateTime.now());
            }
        }

        mission = missionRepository.save(mission);
        MissionDTO dto = missionMapper.toDTO(mission);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/missions/status-changed",
                Map.of("missionId", id, "oldStatus", oldStatus, "newStatus", status, "mission", dto));

        return dto;
    }

    public void deleteMission(Long id) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission not found with id: " + id));

        missionRepository.delete(mission);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/missions/deleted",
                Map.of("missionId", id));
    }

    // A simple exception class for handling cases where a resource is not found.
    // This would typically be in its own file in an 'exception' package.
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
