package pl.logistic.logisticops.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import pl.logistic.logisticops.enums.UnitStatus;
import pl.logistic.logisticops.enums.UnitType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitDTO {
    private Long id;
    private String name;
    private UnitType type;
    private UnitStatus status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer fuelLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
