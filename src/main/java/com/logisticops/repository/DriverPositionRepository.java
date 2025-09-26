// DriverPositionRepository.java content
package com.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logisticops.model.DriverPosition;

@Repository
public interface DriverPositionRepository extends JpaRepository<DriverPosition, Long> {
    // Custom queries can be defined here
}