package pl.logistic.logisticops.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteObstacleDTO {
    private Long id;
    private Long routeProposalId;
    private Long infrastructureId;
    private String infrastructureName;
    private String infrastructureType;
    private Boolean canPass;
    private String restrictionType;
    private Boolean alternativeRouteNeeded;
    private String notes;
}
