package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_tracking", indexes = {
        @Index(name = "idx_tracking_transport", columnList = "transport_id"),
        @Index(name = "idx_tracking_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_tracking_time", columnList = "recorded_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = false)
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleSpecification vehicle;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "heading_degrees")
    private Integer headingDegrees;

    @Column(name = "fuel_level_percentage")
    private Double fuelLevelPercentage;

    @Column(name = "sensor_data", columnDefinition = "TEXT")
    private String sensorData;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}