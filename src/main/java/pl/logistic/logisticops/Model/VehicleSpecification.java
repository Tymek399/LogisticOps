package pl.logistic.logisticops.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "model")
    private String model;

    @Column(name = "type")
    private String type; // Czołg, Transporter, Pojazd wsparcia

    @Column(name = "total_weight_kg")
    private Integer totalWeightKg;

    @Column(name = "axle_count")
    private Integer axleCount;

    @Column(name = "max_axle_load_kg")
    private Integer maxAxleLoadKg;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "description")
    private String description;
}