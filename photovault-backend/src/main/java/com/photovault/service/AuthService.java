package com.photovault.service;

import com.photovault.config.JwtUtil;
import com.photovault.dto.AuthResponse;
import com.photovault.dto.LoginRequest;
import com.photovault.entity.Photographer;
import com.photovault.repository.PhotographerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final PhotographerRepository photographerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Find photographer
        Photographer photographer = photographerRepository.findActiveByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Update last login
        photographer.setLastLoginAt(Instant.now());
        photographerRepository.save(photographer);

        // Generate tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("photographerId", photographer.getId().toString());
        claims.put("plan", photographer.getPlan());

        String token = jwtUtil.generateToken(photographer.getEmail(), claims);
        String refreshToken = jwtUtil.generateToken(photographer.getEmail(), new HashMap<>());

        log.info("Login successful for email: {}", request.getEmail());

        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400000L) // 24 hours
            .photographerId(photographer.getId())
            .email(photographer.getEmail())
            .studioName(photographer.getStudioName())
            .plan(photographer.getPlan())
            .build();
    }

    @Transactional
    public AuthResponse register(String email, String password, String studioName) {
        log.info("Registration attempt for email: {}", email);

        if (photographerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Photographer photographer = Photographer.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .studioName(studioName)
            .plan("trial")
            .planExpiresAt(Instant.now().plus(Duration.ofDays(14)))
            .storageUsedBytes(0L)
            .storageLimitBytes(10737418240L) // 10GB
            .albumsCount(0)
            .albumsLimit(10)
            .build();

        photographer = photographerRepository.save(photographer);

        // Generate tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("photographerId", photographer.getId().toString());
        claims.put("plan", photographer.getPlan());

        String token = jwtUtil.generateToken(photographer.getEmail(), claims);
        String refreshToken = jwtUtil.generateToken(photographer.getEmail(), new HashMap<>());

        log.info("Registration successful for email: {}", email);

        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400000L)
            .photographerId(photographer.getId())
            .email(photographer.getEmail())
            .studioName(photographer.getStudioName())
            .plan(photographer.getPlan())
            .build();
    }
}
