package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.RouteProposal;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RouteProposalRepository extends JpaRepository<RouteProposal, Long> {

    List<RouteProposal> findByMissionId(Long missionId);

    List<RouteProposal> findByApproved(boolean approved);

    List<RouteProposal> findByRouteType(String routeType);

    List<RouteProposal> findByGeneratedAtAfter(LocalDateTime date);

    @Query("SELECT rp FROM RouteProposal rp WHERE rp.mission.id = :missionId " +
            "ORDER BY rp.totalDistanceKm ASC")
    List<RouteProposal> findByMissionIdOrderByDistance(@Param("missionId") Long missionId);

    @Query("SELECT rp FROM RouteProposal rp WHERE rp.mission.id = :missionId " +
            "ORDER BY rp.estimatedTimeMinutes ASC")
    List<RouteProposal> findByMissionIdOrderByTime(@Param("missionId") Long missionId);
}