package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "infrastructure", indexes = {
        @Index(name = "idx_infrastructure_type", columnList = "type"),
        @Index(name = "idx_infrastructure_active", columnList = "isActive"),
        @Index(name = "idx_infrastructure_location", columnList = "latitude, longitude")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Infrastructure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type; // BRIDGE, TUNNEL, WEIGHT_STATION, etc.

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "road_number")
    private String roadNumber;

    @Column(name = "max_height_cm")
    private Integer maxHeightCm;

    @Column(name = "max_weight_kg")
    private Integer maxWeightKg;

    @Column(name = "max_axle_weight_kg")
    private Integer maxAxleWeightKg;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "infrastructure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RouteObstacle> routeObstacles;

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