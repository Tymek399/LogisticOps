// TransportSetRepository.java content
package com.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logisticops.model.TransportSet;

@Repository
public interface TransportSetRepository extends JpaRepository<TransportSet, Long> {
    // Custom queries can be defined here
}