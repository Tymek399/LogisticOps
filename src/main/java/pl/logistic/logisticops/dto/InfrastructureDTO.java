package pl.logistic.logisticops.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureDTO {
    private Long id;
    private String externalId;
    private String name;
    private String type;
    private Double latitude;
    private Double longitude;
    private String roadNumber;
    private Integer maxHeightCm;
    private Integer maxWeightKg;
    private Integer maxAxleWeightKg;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
