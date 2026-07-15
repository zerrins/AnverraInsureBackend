package com.anverraglobal.insurance.auth;

import com.anverraglobal.insurance.auth.entity.RefreshToken;
import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.repository.RefreshTokenRepository;
import com.anverraglobal.insurance.auth.repository.UserRepository;
import com.anverraglobal.insurance.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for {@link RefreshTokenRepository}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Token creation and basic persistence</li>
 *   <li>Lookup by token string</li>
 *   <li>Bulk revocation on logout</li>
 *   <li>Cleanup of expired + revoked tokens</li>
 *   <li>Active token count (multi-device)</li>
 *   <li>Domain helper: isValid()</li>
 * </ul>
 * </p>
 */
@DisplayName("RefreshTokenRepository")
class RefreshTokenRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("refresh-test@anverraglobal.com")
                .password("$2a$10$hashedpassword")
                .name("Refresh Test User")
                .phone("+919123456789")
                .status(UserStatus.ACTIVE)
                .build());
        userRepository.flush();
    }

    private RefreshToken buildToken(User user, boolean expired, boolean revoked) {
        return RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(expired
                        ? LocalDateTime.now().minusDays(1)
                        : LocalDateTime.now().plusDays(7))
                .revoked(revoked)
                .build();
    }

    @Nested
    @DisplayName("save and find")
    class SaveAndFind {

        @Test
        @DisplayName("should persist a refresh token")
        void shouldPersistToken() {
            RefreshToken token = buildToken(testUser, false, false);
            RefreshToken saved = refreshTokenRepository.save(token);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.isValid()).isTrue();
        }

        @Test
        @DisplayName("should find token by its string value")
        void shouldFindByTokenString() {
            RefreshToken token = refreshTokenRepository.save(buildToken(testUser, false, false));
            refreshTokenRepository.flush();

            Optional<RefreshToken> found = refreshTokenRepository.findByToken(token.getToken());

            assertThat(found).isPresent();
            assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("should return empty for unknown token string")
        void shouldReturnEmptyForUnknownToken() {
            Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("revocation")
    class Revocation {

        @Test
        @DisplayName("revokeAllByUser should mark all user tokens as revoked")
        void shouldRevokeAllByUser() {
            RefreshToken t1 = refreshTokenRepository.save(buildToken(testUser, false, false));
            RefreshToken t2 = refreshTokenRepository.save(buildToken(testUser, false, false));
            refreshTokenRepository.flush();

            refreshTokenRepository.revokeAllByUser(testUser);
            refreshTokenRepository.flush();

            RefreshToken r1 = refreshTokenRepository.findById(t1.getId()).orElseThrow();
            RefreshToken r2 = refreshTokenRepository.findById(t2.getId()).orElseThrow();
            assertThat(r1.isRevoked()).isTrue();
            assertThat(r2.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("revokeAllByUserId should revoke tokens for given user ID")
        void shouldRevokeAllByUserId() {
            RefreshToken token = refreshTokenRepository.save(buildToken(testUser, false, false));
            refreshTokenRepository.flush();

            refreshTokenRepository.revokeAllByUserId(testUser.getId());
            refreshTokenRepository.flush();

            RefreshToken reloaded = refreshTokenRepository.findById(token.getId()).orElseThrow();
            assertThat(reloaded.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("cleanup")
    class Cleanup {

        @Test
        @DisplayName("deleteExpiredAndRevoked should delete expired tokens")
        void shouldDeleteExpiredTokens() {
            RefreshToken active  = refreshTokenRepository.save(buildToken(testUser, false, false));
            RefreshToken expired = refreshTokenRepository.save(buildToken(testUser, true,  false));
            RefreshToken revoked = refreshTokenRepository.save(buildToken(testUser, false, true));
            refreshTokenRepository.flush();

            refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
            refreshTokenRepository.flush();

            assertThat(refreshTokenRepository.findById(active.getId())).isPresent();
            assertThat(refreshTokenRepository.findById(expired.getId())).isEmpty();
            assertThat(refreshTokenRepository.findById(revoked.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("active token count")
    class ActiveTokenCount {

        @Test
        @DisplayName("countActiveTokensByUser should count only valid tokens")
        void shouldCountActiveTokensOnly() {
            refreshTokenRepository.save(buildToken(testUser, false, false)); // valid
            refreshTokenRepository.save(buildToken(testUser, false, false)); // valid
            refreshTokenRepository.save(buildToken(testUser, true,  false)); // expired
            refreshTokenRepository.save(buildToken(testUser, false, true));  // revoked
            refreshTokenRepository.flush();

            long count = refreshTokenRepository.countActiveTokensByUser(testUser, LocalDateTime.now());

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("domain helper: isValid()")
    class DomainHelper {

        @Test
        @DisplayName("isValid should return true for active non-expired token")
        void isValidTrue() {
            RefreshToken token = buildToken(testUser, false, false);
            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("isValid should return false for expired token")
        void isValidFalseWhenExpired() {
            RefreshToken token = buildToken(testUser, true, false);
            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("isValid should return false for revoked token")
        void isValidFalseWhenRevoked() {
            RefreshToken token = buildToken(testUser, false, false);
            token.revoke();
            assertThat(token.isValid()).isFalse();
        }
    }
}
