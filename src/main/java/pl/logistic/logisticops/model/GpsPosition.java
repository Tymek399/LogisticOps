package pl.logistic.logisticops.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class GpsPosition {
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Integer heading;
    private Double accuracy;
    private LocalDateTime timestamp;
    private Long vehicleId;
    private Long transportId;
    private String deviceId;
    private String securityLevel;
}