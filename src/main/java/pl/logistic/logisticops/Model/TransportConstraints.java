package pl.logistic.logisticops.Model;

@Data
@Builder
class TransportConstraints {
    private Integer maxHeightCm;
    private Integer totalWeightKg;
    private Integer maxAxleLoadKg;
    private String transporterModel;
    private String cargoModel;
}
