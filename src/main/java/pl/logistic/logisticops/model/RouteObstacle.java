package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_obstacles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteObstacle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_proposal_id", nullable = false)
    private RouteProposal routeProposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "infrastructure_id", nullable = false)
    private Infrastructure infrastructure;

    @Column(name = "can_pass", nullable = false)
    private Boolean canPass;

    @Column(name = "restriction_type")
    private String restrictionType; // HEIGHT, WEIGHT, AXLE_WEIGHT

    @Column(name = "alternative_route_needed")
    private Boolean alternativeRouteNeeded = false;

    @Column(name = "notes")
    private String notes;
}
