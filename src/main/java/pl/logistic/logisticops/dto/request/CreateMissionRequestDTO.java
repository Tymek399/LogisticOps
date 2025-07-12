package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;
import pl.logistic.logisticops.enums.MissionPriority;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMissionRequestDTO {

    @NotNull(message = "Mission name is required")
    @Size(min = 2, max = 200, message = "Mission name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @NotNull(message = "Priority is required")
    private MissionPriority priority;
}