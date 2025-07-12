package pl.logistic.logisticops.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportConstraintsDTO {
    private Integer maxHeightCm;
    private Integer totalWeightKg;
    private Integer maxAxleLoadKg;
    private String transporterModel;
    private String cargoModel;
    private Integer vehicleCount;
    private String description;
}
