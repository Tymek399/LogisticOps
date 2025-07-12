package pl.logistic.logisticops.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRequest {
    private Double startLat;
    private Double startLon;
    private Double endLat;
    private Double endLon;
    private Long missionId;
    private List<Long> transportSetIds;
}