package pl.logistic.logisticops.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import pl.logistic.logisticops.enums.UnitStatus;
import pl.logistic.logisticops.enums.UnitType;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "type")
    private UnitType type; // TANK, APC, SUPPORT
    
    @Column(name = "status")
    private UnitStatus status; // ACTIVE, MOVING, OFFLINE, MAINTENANCE
    
    @Column(name = "latitude")
    private BigDecimal latitude;
    
    @Column(name = "longitude")
    private BigDecimal longitude;
    
    @Column(name = "fuel_level")
    private Integer fuelLevel;
    
    @Column(name = "total_weight_kg")
    private Integer totalWeightKg;
    
    @Column(name = "axle_count")
    private Integer axleCount;
    
    @Column(name = "height_cm")
    private Integer heightCm;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}