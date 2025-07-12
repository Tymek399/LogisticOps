package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.mapper.RouteProposalMapper;
import pl.logistic.logisticops.model.RouteProposal;
import pl.logistic.logisticops.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService {

    private final RouteProposalRepository routeRepository;
    private final RouteObstacleRepository obstacleRepository;
    private final IntelligentRouteService intelligentRouteService;
    private final RouteProposalMapper routeMapper;

    public List<RouteProposalDTO> getAllRouteProposals() {
        return routeRepository.findAll()
                .stream()
                .map(routeMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<RouteProposalDTO> getRouteProposalById(Long id) {
        return routeRepository.findById(id)
                .map(routeMapper::toDTO);
    }

    public List<RouteProposalDTO> getRouteProposalsForMission(Long missionId) {
        return routeRepository.findByMissionId(missionId)
                .stream()
                .map(routeMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<RouteProposalDTO> generateRouteProposals(RouteRequestDTO request) {
        return intelligentRouteService.generateIntelligentRoutes(request);
    }

    public RouteProposalDTO approveRouteProposal(Long id) {
        RouteProposal proposal = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route proposal not found"));

        proposal.setApproved(true);
        proposal = routeRepository.save(proposal);

        return routeMapper.toDTO(proposal);
    }

    public RouteProposalDTO rejectRouteProposal(Long id) {
        RouteProposal proposal = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route proposal not found"));

        proposal.setApproved(false);
        proposal = routeRepository.save(proposal);

        return routeMapper.toDTO(proposal);
    }

    public List<RouteObstacleDTO> getRouteObstacles(Long routeId) {
        return obstacleRepository.findByRouteProposalId(routeId)
                .stream()
                .map(this::mapObstacleToDTO)
                .collect(Collectors.toList());
    }

    public void deleteRouteProposal(Long id) {
        routeRepository.deleteById(id);
    }

    private RouteObstacleDTO mapObstacleToDTO(pl.logistic.logisticops.model.RouteObstacle obstacle) {
        return RouteObstacleDTO.builder()
                .id(obstacle.getId())
                .routeProposalId(obstacle.getRouteProposal().getId())
                .infrastructureId(obstacle.getInfrastructure().getId())
                .infrastructureName(obstacle.getInfrastructure().getName())
                .infrastructureType(obstacle.getInfrastructure().getType())
                .canPass(obstacle.getCanPass())
                .restrictionType(obstacle.getRestrictionType())
                .alternativeRouteNeeded(obstacle.getAlternativeRouteNeeded())
                .notes(obstacle.getNotes())
                .build();
    }
}