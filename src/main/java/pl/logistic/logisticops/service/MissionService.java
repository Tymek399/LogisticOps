package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.Mission;
import pl.logistic.logisticops.enums.MissionStatus;
import pl.logistic.logisticops.repository.MissionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MissionService {
    
    private final MissionRepository missionRepository;
    
    public List<Mission> getAllMissions() {
        return missionRepository.findAll();
    }
    
    public Optional<Mission> getMissionById(Long id) {
        return missionRepository.findById(id);
    }
    
    public List<Mission> getMissionsByStatus(MissionStatus status) {
        return missionRepository.findByStatus(status);
    }
    
    public Mission createMission(Mission mission, Long userId) {
        mission.setCreatedByUserId(userId);
        mission.setCreatedAt(LocalDateTime.now());
        mission.setStatus(MissionStatus.PLANNED);
        
        return missionRepository.save(mission);
    }
    
    public Mission updateMissionStatus(Long id, MissionStatus status) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono misji o ID: " + id));
        
        mission.setStatus(status);
        
        if (status == MissionStatus.COMPLETED || status == MissionStatus.CANCELLED) {
            mission.setEndDate(LocalDateTime.now());
        }
        
        return missionRepository.save(mission);
    }
    
    public void deleteMission(Long id) {
        missionRepository.deleteById(id);
    }
}