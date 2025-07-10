package pl.logistic.logisticops.Model;
import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "infrastructures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Infrastructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
    
    @Column(name = "type")
    private String type; // BRIDGE, TUNNEL
    
    @Column(name = "max_weight_tons")
    private double maxWeightTons;
    
    @Column(name = "max_height_cm")
    private int maxHeightCm;
    
    @Column(name = "latitude")
    private double latitude;
    
    @Column(name = "longitude")
    private double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_set_id")
    private TransportSet transportSet;
}