package pl.logistic.logisticops.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "transport_sets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private VehicleSpecification transporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id", nullable = false)
    private VehicleSpecification cargo;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "transportSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Infrastructure> infrastructures;
}