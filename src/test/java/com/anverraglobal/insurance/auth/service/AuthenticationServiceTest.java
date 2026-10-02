package com.anverraglobal.insurance.auth.service;

import com.anverraglobal.insurance.auth.dto.AuthResponse;
import com.anverraglobal.insurance.auth.dto.RefreshRequest;
import com.anverraglobal.insurance.auth.entity.RefreshToken;
import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.repository.RefreshTokenRepository;
import com.anverraglobal.insurance.exception.UnauthorizedException;
import com.anverraglobal.insurance.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void refresh_AtomicConsumptionSucceeds_ReturnsNewTokens() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-token");

        User user = new User();
        user.setId(1L);

        RefreshToken token = new RefreshToken();
        token.setId(10L);
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setRevoked(false);
        token.setUser(user);

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.deleteByIdReturningCount(10L)).thenReturn(1);
        when(jwtService.generateToken(any(Authentication.class))).thenReturn("new-access-token");

        AuthResponse response = authenticationService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals("valid-token", response.getRefreshToken());

        verify(refreshTokenRepository).deleteByIdReturningCount(10L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_AtomicConsumptionFails_ThrowsUnauthorizedException() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-token");

        User user = new User();
        user.setId(1L);

        RefreshToken token = new RefreshToken();
        token.setId(10L);
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setRevoked(false);
        token.setUser(user);

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.deleteByIdReturningCount(10L)).thenReturn(0);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authenticationService.refresh(request));
        assertEquals("Refresh token has already been consumed", ex.getMessage());

        verify(refreshTokenRepository).deleteByIdReturningCount(10L);
        // Ensure no new tokens are saved
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
