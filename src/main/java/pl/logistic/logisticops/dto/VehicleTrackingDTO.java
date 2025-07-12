package pl.logistic.logisticops.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTrackingDTO {
    private Long id;
    private Long transportId;
    private Long vehicleId;
    private String vehicleModel;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Integer headingDegrees;
    private Double fuelLevelPercentage;
    private String sensorData;
    private LocalDateTime recordedAt;
}