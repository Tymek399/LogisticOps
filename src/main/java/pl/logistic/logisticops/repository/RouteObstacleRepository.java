package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.RouteObstacle;

import java.util.List;

@Repository
public interface RouteObstacleRepository extends JpaRepository<RouteObstacle, Long> {

    List<RouteObstacle> findByRouteProposalId(Long routeProposalId);

    List<RouteObstacle> findByInfrastructureId(Long infrastructureId);

    List<RouteObstacle> findByCanPassFalse();

    List<RouteObstacle> findByAlternativeRouteNeededTrue();

    List<RouteObstacle> findByRestrictionType(String restrictionType);

    @Query("SELECT ro FROM RouteObstacle ro WHERE ro.routeProposal.id = :routeId " +
            "AND ro.canPass = false")
    List<RouteObstacle> findBlockingObstaclesByRoute(@Param("routeId") Long routeId);

    @Query("SELECT COUNT(ro) FROM RouteObstacle ro WHERE ro.routeProposal.id = :routeId")
    Long countByRouteProposalId(@Param("routeId") Long routeId);
}
