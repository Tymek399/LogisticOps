package pl.logistic.logisticops.reposiotry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.Model.TransportSet;

@Repository
public interface TransportSetRepository extends JpaRepository<TransportSet, Long> {
    // Możesz dodać metody wyszukiwania po transporterze, cargo itp.
}
