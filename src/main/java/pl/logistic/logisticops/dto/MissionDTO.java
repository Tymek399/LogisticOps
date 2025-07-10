package pl.logistic.logisticops.dto;

import java.time.LocalDateTime;

import jakarta.annotation.Priority;
import lombok.*;
import pl.logistic.logisticops.Model.Mission;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;
import pl.logistic.logisticops.enums.UnitStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissionDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private MissionStatus status;   // lub enum MissionStatus
    private MissionPriority priority; // lub enum MissionPriority
    private Long createdBy;
    private LocalDateTime createdAt;
}
