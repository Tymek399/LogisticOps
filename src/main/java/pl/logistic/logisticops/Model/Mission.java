package pl.logistic.logisticops.Model;

import java.time.LocalDateTime;

import jakarta.annotation.Priority;
import jakarta.persistence.*;
import lombok.*;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MissionStatus status; // PLANNED, ACTIVE, COMPLETED, CANCELLED
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private MissionPriority priority; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}