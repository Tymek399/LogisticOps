package pl.logistic.logisticops.dto;

import lombok.*;
import pl.logistic.logisticops.enums.VehicleType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSpecificationDTO {
    private Long id;
    private String model;
    private VehicleType type;
    private Integer totalWeightKg;
    private Integer axleCount;
    private Integer maxAxleLoadKg;
    private Integer heightCm;
    private Integer lengthCm;
    private Integer widthCm;
    private String description;
    private Boolean active;
}