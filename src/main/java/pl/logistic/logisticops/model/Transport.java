package pl.logistic.logisticops.model;


import jakarta.persistence.*;
import lombok.*;
import pl.logistic.logisticops.enums.TransportStatus;
import pl.logistic.logisticops.model.Mission;
import pl.logistic.logisticops.model.RouteProposal;
import pl.logistic.logisticops.model.TransportVehicle;
import pl.logistic.logisticops.model.VehicleTracking;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transport_number", unique = true, nullable = false)
    private String transportNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_route_id")
    private RouteProposal approvedRoute;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "progress_percentage")
    private Double progressPercentage = 0.0;

    @Column(name = "distance_covered_km")
    private Double distanceCoveredKm = 0.0;

    @Column(name = "distance_remaining_km")
    private Double distanceRemainingKm;

    @Column(name = "planned_departure")
    private LocalDateTime plannedDeparture;

    @Column(name = "actual_departure")
    private LocalDateTime actualDeparture;

    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "transport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransportVehicle> vehicles;

    @OneToMany(mappedBy = "transport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleTracking> trackingHistory;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
