package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMissionRequestDTO {

    @Size(min = 2, max = 200, message = "Mission name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private MissionStatus status;
    private MissionPriority priority;
}