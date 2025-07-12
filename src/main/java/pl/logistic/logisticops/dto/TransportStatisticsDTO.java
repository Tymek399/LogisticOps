package pl.logistic.logisticops.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportStatisticsDTO {
    private Long totalTransports;
    private Long activeTransports;
    private Long completedTransports;
    private Long delayedTransports;
    private Double averageCompletionTime;
    private Double totalDistanceCovered;
    private Double averageFuelConsumption;
    private LocalDateTime lastUpdated;
}
