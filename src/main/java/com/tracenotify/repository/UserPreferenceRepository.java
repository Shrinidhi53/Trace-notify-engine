package com.tracenotify.repository;

import com.tracenotify.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
}
