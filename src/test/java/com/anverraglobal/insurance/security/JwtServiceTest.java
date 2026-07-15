package com.anverraglobal.insurance.security;

import com.anverraglobal.insurance.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        jwtService = new JwtService(appProperties);
    }

    @Test
    void generateTokenFromUserId_generatesValidToken() {
        Long userId = 1L;
        String token = jwtService.generateTokenFromUserId(userId);
        assertNotNull(token);
        
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.getUserIdFromJWT(token));
    }

    @Test
    void generateRefreshToken_generatesValidUUID() {
        String refreshToken = jwtService.generateRefreshToken();
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
    }
}
