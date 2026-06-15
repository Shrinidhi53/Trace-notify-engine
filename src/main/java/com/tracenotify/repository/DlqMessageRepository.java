package com.tracenotify.repository;

import com.tracenotify.model.DlqMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DlqMessageRepository extends JpaRepository<DlqMessage, UUID> {
    List<DlqMessage> findAllByOrderByReceivedAtDesc();
}
