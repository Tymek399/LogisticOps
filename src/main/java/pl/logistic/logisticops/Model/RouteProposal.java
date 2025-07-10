package pl.logistic.logisticops.Model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "mission_id")
    private Long missionId;
    
    // Dla List należy używać @ElementCollection lub @OneToMany 
    // zamiast prostej adnotacji @Column
    private List<RouteSegment> segments;
    
    private List<Infrastructure> infrastructures;
    
    @Column(name = "approved")
    private boolean approved;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
}