package com.tracenotify.service;

import com.tracenotify.dto.PreferenceResponse;
import com.tracenotify.exception.ApiException;
import com.tracenotify.model.UserPreference;
import com.tracenotify.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository repository;

    public UserPreferenceService(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    public void createDefault(UUID userId) {
        if (!repository.existsById(userId)) {
            repository.save(UserPreference.builder()
                    .userId(userId)
                    .inAppEnabled(true)
                    .build());
        }
    }

    public boolean isInAppEnabled(UUID userId) {
        return repository.findById(userId)
                .map(UserPreference::isInAppEnabled)
                .orElse(true);
    }

    public PreferenceResponse get(UUID userId) {
        UserPreference pref = repository.findById(userId)
                .orElseGet(() -> {
                    createDefault(userId);
                    return repository.findById(userId).orElseThrow();
                });
        return new PreferenceResponse(pref.isInAppEnabled());
    }

    public PreferenceResponse update(UUID userId, boolean inAppEnabled) {
        UserPreference pref = repository.findById(userId)
                .orElse(UserPreference.builder().userId(userId).build());
        pref.setInAppEnabled(inAppEnabled);
        repository.save(pref);
        return new PreferenceResponse(inAppEnabled);
    }
}
