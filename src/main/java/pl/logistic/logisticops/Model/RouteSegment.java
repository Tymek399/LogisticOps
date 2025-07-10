package pl.logistic.logisticops.Model;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "from_location")
    private String from;
    
    @Column(name = "to_location")
    private String to;
    
    @Column(name = "distance_km")
    private double distanceKm;
    
    @Column(name = "estimated_time_min")
    private double estimatedTimeMin;
    
    @Column(name = "road_condition")
    private String roadCondition;
}