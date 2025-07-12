package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;
import pl.logistic.logisticops.enums.VehicleType;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vehicle_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSpecification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model", nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VehicleType type;

    @Column(name = "total_weight_kg")
    private Integer totalWeightKg;

    @Column(name = "axle_count")
    private Integer axleCount;

    @Column(name = "max_axle_load_kg")
    private Integer maxAxleLoadKg;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "length_cm")
    private Integer lengthCm;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<TransportVehicle> transportAssignments;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
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