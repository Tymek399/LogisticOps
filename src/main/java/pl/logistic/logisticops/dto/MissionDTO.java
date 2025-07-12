package pl.logistic.logisticops.dto;

import java.time.LocalDateTime;
import lombok.*;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;

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
    private MissionStatus status;
    private MissionPriority priority;
    private Long createdByUserId;
    private LocalDateTime createdAt;
}
