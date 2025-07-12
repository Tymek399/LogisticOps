package pl.logistic.logisticops.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportConstraints {
    private Integer maxHeightCm;
    private Integer totalWeightKg;
    private Integer maxAxleLoadKg;
    private String transporterModel;
    private String cargoModel;
}
