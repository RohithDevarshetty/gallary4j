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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private PhotographerRepository photographerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private Authentication authentication;

    @InjectMocks private AuthService authService;

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
            .albumsCount(0)
            .albumsLimit(10)
            .storageUsedBytes(0L)
            .storageLimitBytes(10737418240L)
            .build();

        loginRequest = LoginRequest.builder()
            .email("test@example.com")
            .password("password123")
            .build();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokens() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(photographerRepository.findActiveByEmail(loginRequest.getEmail()))
            .thenReturn(Optional.of(photographer));
        when(jwtUtil.generateToken(anyString(), any()))
            .thenReturn("access-token", "refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(photographer.getId(), response.getPhotographerId());
        assertEquals(photographer.getEmail(), response.getEmail());
        assertEquals(photographer.getStudioName(), response.getStudioName());
    }

    @Test
    void login_validCredentials_updatesLastLoginAt() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(photographerRepository.findActiveByEmail(any())).thenReturn(Optional.of(photographer));
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("token");

        authService.login(loginRequest);

        verify(photographerRepository).save(argThat(p -> p.getLastLoginAt() != null));
    }

    @Test
    void login_badCredentials_propagatesException() {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(photographerRepository, never()).save(any());
    }

    @Test
    void login_photographerNotFound_throwsException() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(photographerRepository.findActiveByEmail(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        verify(photographerRepository, never()).save(any());
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_createsPhotographerAndReturnsTokens() {
        when(photographerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        when(photographerRepository.save(any(Photographer.class))).thenReturn(photographer);
        when(jwtUtil.generateToken(anyString(), any()))
            .thenReturn("access-token", "refresh-token");

        AuthResponse response = authService.register("test@example.com", "password123", "Test Studio");

        assertNotNull(response);
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(photographerRepository).save(any(Photographer.class));
    }

    @Test
    void register_newPhotographer_setsTrialPlan() {
        when(photographerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(photographerRepository.save(any(Photographer.class))).thenAnswer(inv -> {
            Photographer p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("token");

        authService.register("new@example.com", "pass", "Studio");

        verify(photographerRepository).save(argThat(p ->
            "trial".equals(p.getPlan()) &&
            p.getPlanExpiresAt() != null &&
            p.getStorageLimitBytes() == 10737418240L &&
            p.getAlbumsLimit() == 10
        ));
    }

    @Test
    void register_newPhotographer_encodesPassword() {
        when(photographerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded-hash");
        when(photographerRepository.save(any())).thenAnswer(inv -> {
            Photographer p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("token");

        authService.register("test@example.com", "rawpassword", "Studio");

        verify(photographerRepository).save(argThat(p -> "encoded-hash".equals(p.getPasswordHash())));
    }

    @Test
    void register_duplicateEmail_throwsExceptionWithoutSaving() {
        when(photographerRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            authService.register("test@example.com", "password", "Studio")
        );
        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(photographerRepository, never()).save(any());
    }

    @Test
    void register_success_tokenContainsPhotographerId() {
        when(photographerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(photographerRepository.save(any(Photographer.class))).thenReturn(photographer);
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("token");

        AuthResponse response = authService.register("test@example.com", "pass", "Studio");

        // Verify token generation was called with photographerId claim
        verify(jwtUtil).generateToken(eq("test@example.com"), argThat(claims ->
            claims.containsKey("photographerId") && claims.containsKey("plan")
        ));
    }
}
