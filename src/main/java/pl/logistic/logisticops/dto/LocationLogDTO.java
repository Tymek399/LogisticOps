package pl.logistic.logisticops.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationLogDTO {
    private Long id;
    private Long unitId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime timestamp;
    private Double speed;
    private Integer heading;
}
