package pl.logistic.logisticops.Model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "safe_mode_enabled")
    private boolean safeModeEnabled;
    
    @Column(name = "issue")
    private String issue;
    
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;
    
    @Column(name = "fallback_modules")
    private String fallbackModules;
}