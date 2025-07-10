package pl.logistic.logisticops.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "unit_id")
    private Long unitId;
    
    @Column(name = "latitude")
    private BigDecimal latitude;
    
    @Column(name = "longitude")
    private BigDecimal longitude;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "speed")
    private BigDecimal speed;
    
    @Column(name = "heading")
    private Integer heading;
}