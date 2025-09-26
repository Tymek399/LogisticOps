// InfrastructurePointRepository.java content
package com.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logisticops.model.InfrastructurePoint;

@Repository
public interface InfrastructurePointRepository extends JpaRepository<InfrastructurePoint, Long> {
    // Custom queries can be defined here
}