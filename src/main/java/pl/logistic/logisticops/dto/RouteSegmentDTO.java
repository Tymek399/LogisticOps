package pl.logistic.logisticops.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegmentDTO {
    private Long id;
    private Long routeProposalId;
    private Integer sequenceOrder;
    private String fromLocation;
    private String toLocation;
    private Double fromLatitude;
    private Double fromLongitude;
    private Double toLatitude;
    private Double toLongitude;
    private Double distanceKm;
    private Double estimatedTimeMin;
    private String roadCondition;
    private String roadName;
    private String polyline;
}