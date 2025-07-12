package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.MissionDTO;
import pl.logistic.logisticops.dto.request.CreateMissionRequestDTO;
import pl.logistic.logisticops.dto.request.UpdateMissionRequestDTO;
import pl.logistic.logisticops.enums.MissionStatus;
import pl.logistic.logisticops.service.MissionService;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MissionController {

    private final MissionService missionService;

    @GetMapping
    public ResponseEntity<Page<MissionDTO>> getAllMissions(Pageable pageable) {
        return ResponseEntity.ok(missionService.getAllMissions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MissionDTO> getMissionById(@PathVariable Long id) {
        return missionService.getMissionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<MissionDTO>> getMissionsByStatus(
            @PathVariable MissionStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(missionService.getMissionsByStatus(status, pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MissionDTO>> getMissionsByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(missionService.getMissionsByUser(userId, pageable));
    }

    @PostMapping
    public ResponseEntity<MissionDTO> createMission(
            @Valid @RequestBody CreateMissionRequestDTO request,
            @RequestHeader("User-Id") Long userId) {

        MissionDTO missionDTO = MissionDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .priority(request.getPriority())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(missionService.createMission(missionDTO, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MissionDTO> updateMission(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMissionRequestDTO request) {

        MissionDTO missionDTO = MissionDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .priority(request.getPriority())
                .build();

        return ResponseEntity.ok(missionService.updateMission(id, missionDTO));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MissionDTO> updateMissionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        MissionStatus status = MissionStatus.valueOf(statusUpdate.get("status"));
        return ResponseEntity.ok(missionService.updateMissionStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(@PathVariable Long id) {
        missionService.deleteMission(id);
        return ResponseEntity.noContent().build();
    }
}
