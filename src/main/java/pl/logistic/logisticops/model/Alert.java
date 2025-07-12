package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;
import pl.logistic.logisticops.enums.AlertLevel;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private AlertLevel level;

    @Column(name = "type")
    private String type; // TRAFFIC, INFRASTRUCTURE, FUEL, SPEED, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transport_id")
    private Transport relatedTransport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_infrastructure_id")
    private Infrastructure relatedInfrastructure;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}