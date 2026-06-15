package com.tracenotify.service;

import com.tracenotify.dto.AuthResponse;
import com.tracenotify.dto.LoginRequest;
import com.tracenotify.dto.RegisterRequest;
import com.tracenotify.exception.ApiException;
import com.tracenotify.model.Role;
import com.tracenotify.model.User;
import com.tracenotify.repository.UserRepository;
import com.tracenotify.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserPreferenceService preferenceService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, UserPreferenceService preferenceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.preferenceService = preferenceService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ApiException(409, "Username already taken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(409, "Email already registered");
        }
        Role role = "ADMIN".equalsIgnoreCase(req.role()) ? Role.ADMIN : Role.USER;
        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(role)
                .build();
        user = userRepository.save(user);
        preferenceService.createDefault(user.getId());
        String token = jwtUtil.generateToken(user.getId().toString(), user.getUsername(), role.name());
        return new AuthResponse(token, user.getId().toString(), user.getUsername(), role.name());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ApiException(401, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(401, "Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId().toString(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getId().toString(), user.getUsername(), user.getRole().name());
    }
}
