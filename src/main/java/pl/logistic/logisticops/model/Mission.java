package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private MissionPriority priority;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transport> transports;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RouteProposal> routeProposals;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}