package pl.logistic.logisticops.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportVehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = false)
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleSpecification vehicle;

    @Column(name = "role")
    private String role; // TRANSPORTER, CARGO

    @Column(name = "sequence_order")
    private Integer sequenceOrder;
}
