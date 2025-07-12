package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_segments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_proposal_id", nullable = false)
    private RouteProposal routeProposal;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "from_location")
    private String fromLocation;

    @Column(name = "to_location")
    private String toLocation;

    @Column(name = "from_latitude")
    private Double fromLatitude;

    @Column(name = "from_longitude")
    private Double fromLongitude;

    @Column(name = "to_latitude")
    private Double toLatitude;

    @Column(name = "to_longitude")
    private Double toLongitude;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "estimated_time_min")
    private Double estimatedTimeMin;

    @Column(name = "road_condition")
    private String roadCondition;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "polyline", columnDefinition = "TEXT")
    private String polyline; // Encoded polyline dla Google Maps
}