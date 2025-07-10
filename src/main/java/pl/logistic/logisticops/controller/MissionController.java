package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.Model.Mission;
import pl.logistic.logisticops.enums.MissionStatus;
import pl.logistic.logisticops.service.MissionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {
    
    private final MissionService missionService;
    
    @GetMapping
    public ResponseEntity<List<Mission>> getAllMissions() {
        return ResponseEntity.ok(missionService.getAllMissions());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Mission> getMissionById(@PathVariable Long id) {
        return missionService.getMissionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Mission>> getMissionsByStatus(@PathVariable MissionStatus status) {
        return ResponseEntity.ok(missionService.getMissionsByStatus(status));
    }
    
    @PostMapping
    public ResponseEntity<Mission> createMission(@RequestBody Mission mission, 
                                               @RequestHeader("User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(missionService.createMission(mission, userId));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Mission> updateMissionStatus(@PathVariable Long id, 
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