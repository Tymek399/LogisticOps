// RouteRepository.java content
package com.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logisticops.model.Route;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    // Custom queries can be defined here
}