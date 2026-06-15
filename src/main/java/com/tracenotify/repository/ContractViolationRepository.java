package com.tracenotify.repository;

import com.tracenotify.model.ContractViolation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractViolationRepository extends JpaRepository<ContractViolation, UUID> {
    List<ContractViolation> findAllByOrderByTimestampDesc();
}
