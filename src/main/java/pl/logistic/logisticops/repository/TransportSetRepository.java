package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.TransportSet;

import java.util.List;

@Repository
public interface TransportSetRepository extends JpaRepository<TransportSet, Long> {

    @Query("SELECT ts FROM TransportSet ts JOIN ts.transporter t WHERE t.id = :transporterId")
    List<TransportSet> findByTransporterId(@Param("transporterId") Long transporterId);

    @Query("SELECT ts FROM TransportSet ts JOIN ts.cargo c WHERE c.id = :cargoId")
    List<TransportSet> findByCargoId(@Param("cargoId") Long cargoId);

    @Query("SELECT ts FROM TransportSet ts JOIN ts.transporter t " +
            "WHERE t.type = :transporterType")
    List<TransportSet> findByTransporterType(@Param("transporterType") String transporterType);
}