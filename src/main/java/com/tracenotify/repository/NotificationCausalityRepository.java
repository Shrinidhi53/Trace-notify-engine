package com.tracenotify.repository;

import com.tracenotify.model.NotificationCausality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationCausalityRepository extends JpaRepository<NotificationCausality, UUID> {
    List<NotificationCausality> findByNotificationIdOrderByStepOrderAsc(UUID notificationId);
}
