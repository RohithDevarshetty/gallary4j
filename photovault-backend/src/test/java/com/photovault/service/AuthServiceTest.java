package com.photovault.service;

import com.photovault.config.JwtUtil;
import com.photovault.dto.AuthResponse;
import com.photovault.dto.LoginRequest;
import com.photovault.entity.Photographer;
import com.photovault.repository.PhotographerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PhotographerRepository photographerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private Photographer photographer;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        photographer = Photographer.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .studioName("Test Studio")
            .passwordHash("hashedpassword")
            .plan("trial")
            .build();

        loginRequest = LoginRequest.builder()
            .email("test@example.com")
            .password("password123")
            .build();
    }

    @Test
    void login_Success() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(photographerRepository.findActiveByEmail(loginRequest.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(jwtUtil.generateToken(anyString(), any()))
            .thenReturn("access-token")
            .thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(photographer.getId(), response.getPhotographerId());
        assertEquals(photographer.getEmail(), response.getEmail());
        verify(photographerRepository).save(any(Photographer.class));
    }

    @Test
    void register_Success() {
        // Arrange
        when(photographerRepository.existsByEmail(anyString()))
            .thenReturn(false);
        when(passwordEncoder.encode(anyString()))
            .thenReturn("hashedpassword");
        when(photographerRepository.save(any(Photographer.class)))
            .thenReturn(photographer);
        when(jwtUtil.generateToken(anyString(), any()))
            .thenReturn("access-token")
            .thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.register(
            "test@example.com",
            "password123",
            "Test Studio"
        );

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(photographerRepository).save(any(Photographer.class));
    }

    @Test
    void register_EmailExists_ThrowsException() {
        // Arrange
        when(photographerRepository.existsByEmail(anyString()))
            .thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            authService.register("test@example.com", "password", "Studio")
        );
        verify(photographerRepository, never()).save(any(Photographer.class));
    }
}
