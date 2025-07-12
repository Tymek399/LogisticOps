package pl.logistic.logisticops.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRequestDTO {
    private Double startLatitude;
    private Double startLongitude;
    private String startAddress;
    private Double endLatitude;
    private Double endLongitude;
    private String endAddress;
    private Long missionId;
    private List<Long> vehicleIds;
    private LocalDateTime plannedDeparture;
    private String routeType; // OPTIMAL, SAFE, ALTERNATIVE
}