// UserRepository.java content
package com.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logisticops.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom queries can be defined here
}