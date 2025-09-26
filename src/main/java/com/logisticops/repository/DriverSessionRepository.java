// DriverSessionRepository.java content
package com.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logisticops.model.DriverSession;

@Repository
public interface DriverSessionRepository extends JpaRepository<DriverSession, Long> {
    // Custom queries can be defined here
}