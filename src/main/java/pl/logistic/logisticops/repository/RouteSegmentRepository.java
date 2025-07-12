package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.RouteSegment;

import java.util.List;

@Repository
public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    List<RouteSegment> findByRouteProposalIdOrderBySequenceOrder(Long routeProposalId);

    @Query("SELECT rs FROM RouteSegment rs WHERE rs.routeProposal.id = :routeId " +
            "ORDER BY rs.sequenceOrder")
    List<RouteSegment> findByRouteProposalIdOrdered(@Param("routeId") Long routeId);

    @Query("SELECT SUM(rs.distanceKm) FROM RouteSegment rs WHERE rs.routeProposal.id = :routeId")
    Double getTotalDistanceByRouteProposalId(@Param("routeId") Long routeId);

    @Query("SELECT SUM(rs.estimatedTimeMin) FROM RouteSegment rs WHERE rs.routeProposal.id = :routeId")
    Double getTotalTimeByRouteProposalId(@Param("routeId") Long routeId);

    @Query("SELECT COUNT(rs) FROM RouteSegment rs WHERE rs.routeProposal.id = :routeId")
    Long countByRouteProposalId(@Param("routeId") Long routeId);

    void deleteByRouteProposalId(Long routeProposalId);
}