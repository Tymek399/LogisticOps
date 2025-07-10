package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.logistic.logisticops.Model.RouteProposal;

import java.util.List;

public interface RouteProposalRepository extends JpaRepository<RouteProposal, Long> {
    
    List<RouteProposal> findByMissionId(Long missionId);
    
    List<RouteProposal> findByApproved(boolean approved);
}