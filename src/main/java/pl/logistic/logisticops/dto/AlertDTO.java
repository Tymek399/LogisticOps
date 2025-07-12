package pl.logistic.logisticops.dto;

import lombok.*;
import pl.logistic.logisticops.enums.AlertLevel;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDTO {
    private Long id;
    private String message;
    private AlertLevel level;
    private String type;
    private Long relatedTransportId;
    private String relatedTransportName;
    private Long relatedInfrastructureId;
    private String relatedInfrastructureName;
    private Boolean resolved;
    private LocalDateTime timestamp;
    private LocalDateTime resolvedAt;
}