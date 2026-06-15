package com.tracenotify.repository;

import com.tracenotify.model.ServiceContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceContractRepository extends JpaRepository<ServiceContract, UUID> {
    Optional<ServiceContract> findByServiceName(String serviceName);
    boolean existsByServiceName(String serviceName);
}
