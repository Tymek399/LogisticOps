package pl.logistic.logisticops.Model;

import lombok.Builder;
import lombok.Data;


import java.util.List;

@Data
@Builder
class RouteRequest {
    private Double startLat;
    private Double startLon;
    private Double endLat;
    private Double endLon;
    private Long missionId;
    private List<Long> vehicleIds;
}