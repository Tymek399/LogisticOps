package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "route_proposals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Column(name = "route_type")
    private String routeType; // OPTIMAL, SAFE, ALTERNATIVE

    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "estimated_time_minutes")
    private Double estimatedTimeMinutes;

    @Column(name = "fuel_consumption_liters")
    private Double fuelConsumptionLiters;

    @Column(name = "approved", nullable = false)
    private Boolean approved = false;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "routeProposal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<RouteSegment> segments;

    @OneToMany(mappedBy = "routeProposal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RouteObstacle> obstacles;

    @OneToMany(mappedBy = "approvedRoute", fetch = FetchType.LAZY)
    private List<Transport> transports;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
