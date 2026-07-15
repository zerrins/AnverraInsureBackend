package com.anverraglobal.insurance.auth;

import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.entity.UserRole;
import com.anverraglobal.insurance.auth.repository.UserRepository;
import com.anverraglobal.insurance.model.enums.RoleName;
import com.anverraglobal.insurance.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for {@link UserRepository}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Basic CRUD persistence</li>
 *   <li>Unique constraint enforcement (email, uuid, phone)</li>
 *   <li>Soft-delete behaviour</li>
 *   <li>Login tracking queries</li>
 *   <li>Role management via User aggregate</li>
 * </ul>
 * </p>
 */
@DisplayName("UserRepository")
class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User buildTestUser(String emailSuffix) {
        return User.builder()
                .email("test" + emailSuffix + "@anverraglobal.com")
                .password("$2a$10$hashedpassword")
                .name("Test User " + emailSuffix)
                .phone("+9198765" + emailSuffix)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("save and find")
    class SaveAndFind {

        @Test
        @DisplayName("should persist user and generate UUID on pre-persist")
        void shouldPersistUserWithGeneratedUuid() {
            User user = buildTestUser("001");
            User saved = userRepository.save(user);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUuid()).isNotNull().hasSize(36);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should find active user by email")
        void shouldFindByEmail() {
            userRepository.save(buildTestUser("002"));

            Optional<User> found = userRepository.findByEmailAndDeletedFalse("test002@anverraglobal.com");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test002@anverraglobal.com");
        }

        @Test
        @DisplayName("should find active user by phone")
        void shouldFindByPhone() {
            userRepository.save(buildTestUser("003"));

            Optional<User> found = userRepository.findByPhoneAndDeletedFalse("+9198765003");

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should find active user by UUID")
        void shouldFindByUuid() {
            User saved = userRepository.save(buildTestUser("004"));
            String uuid = saved.getUuid();

            Optional<User> found = userRepository.findByUuidAndDeletedFalse(uuid);

            assertThat(found).isPresent();
            assertThat(found.get().getUuid()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("should return empty when user not found by email")
        void shouldReturnEmptyForMissingEmail() {
            Optional<User> found = userRepository.findByEmailAndDeletedFalse("nonexistent@example.com");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("unique constraint enforcement")
    class UniqueConstraints {

        @Test
        @DisplayName("should reject duplicate email")
        void shouldRejectDuplicateEmail() {
            User u1 = buildTestUser("010");
            User u2 = User.builder()
                    .email("test010@anverraglobal.com")  // same email
                    .password("$2a$10$hash")
                    .name("Other User")
                    .phone("+91999999010")
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(u1);
            userRepository.flush();

            assertThatThrownBy(() -> {
                userRepository.save(u2);
                userRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should reject duplicate phone")
        void shouldRejectDuplicatePhone() {
            User u1 = buildTestUser("011");
            User u2 = User.builder()
                    .email("unique@anverraglobal.com")
                    .password("$2a$10$hash")
                    .name("Other User")
                    .phone("+9198765011")  // same phone as test011
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(u1);
            userRepository.flush();

            assertThatThrownBy(() -> {
                userRepository.save(u2);
                userRepository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("existsByEmail returns true for registered email")
        void existsByEmailShouldReturnTrue() {
            userRepository.save(buildTestUser("012"));
            assertThat(userRepository.existsByEmail("test012@anverraglobal.com")).isTrue();
        }

        @Test
        @DisplayName("existsByPhone returns true for registered phone")
        void existsByPhoneShouldReturnTrue() {
            userRepository.save(buildTestUser("013"));
            assertThat(userRepository.existsByPhone("+9198765013")).isTrue();
        }

        @Test
        @DisplayName("existsByEmail returns false for unknown email")
        void existsByEmailShouldReturnFalse() {
            assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("soft delete")
    class SoftDelete {

        @Test
        @DisplayName("soft-deleted user should not be found by email")
        void softDeletedUserShouldNotBeFoundByEmail() {
            User user = userRepository.save(buildTestUser("020"));
            userRepository.flush();

            userRepository.softDeleteById(user.getId(), LocalDateTime.now());
            userRepository.flush();

            Optional<User> found = userRepository.findByEmailAndDeletedFalse("test020@anverraglobal.com");
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("soft-deleted user should still exist in DB")
        void softDeletedUserShouldExistInDb() {
            User user = userRepository.save(buildTestUser("021"));
            userRepository.flush();

            userRepository.softDeleteById(user.getId(), LocalDateTime.now());
            userRepository.flush();

            // findById (no soft-delete filter) should still find it
            Optional<User> raw = userRepository.findById(user.getId());
            assertThat(raw).isPresent();
            assertThat(raw.get().isDeleted()).isTrue();
            assertThat(raw.get().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("findAllByDeletedFalse should exclude soft-deleted users")
        void findAllShouldExcludeSoftDeleted() {
            User active = userRepository.save(buildTestUser("022"));
            User toDelete = userRepository.save(buildTestUser("023"));
            userRepository.flush();

            userRepository.softDeleteById(toDelete.getId(), LocalDateTime.now());
            userRepository.flush();

            var all = userRepository.findAllByDeletedFalse();
            assertThat(all).extracting(User::getEmail)
                    .contains("test022@anverraglobal.com")
                    .doesNotContain("test023@anverraglobal.com");
        }
    }

    @Nested
    @DisplayName("login tracking")
    class LoginTracking {

        @Test
        @DisplayName("recordSuccessfulLogin should update lastLogin and reset loginAttempts")
        void recordSuccessfulLoginShouldUpdateFields() {
            User user = buildTestUser("030");
            user.setLoginAttempts(3);
            User saved = userRepository.save(user);
            userRepository.flush();

            LocalDateTime loginTime = LocalDateTime.now();
            userRepository.recordSuccessfulLogin(saved.getId(), loginTime);
            userRepository.flush();

            User updated = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getLoginAttempts()).isZero();
            assertThat(updated.getLastLogin()).isNotNull();
        }

        @Test
        @DisplayName("incrementLoginAttempts should increment counter")
        void incrementLoginAttemptsShouldIncrement() {
            User user = userRepository.save(buildTestUser("031"));
            userRepository.flush();

            userRepository.incrementLoginAttempts(user.getId());
            userRepository.flush();

            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getLoginAttempts()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("role management")
    class RoleManagement {

        @Test
        @DisplayName("should persist roles via User aggregate")
        void shouldPersistRoles() {
            User user = buildTestUser("040");
            user.addRole(RoleName.AGENT);
            user.addRole(RoleName.BROKER);

            User saved = userRepository.save(user);
            userRepository.flush();

            User loaded = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(loaded.getRoles())
                    .extracting(UserRole::getRole)
                    .containsExactlyInAnyOrder(RoleName.AGENT, RoleName.BROKER);
        }

        @Test
        @DisplayName("hasRole should return true for assigned role")
        void hasRoleShouldWork() {
            User user = buildTestUser("041");
            user.addRole(RoleName.ADMIN);

            assertThat(user.hasRole(RoleName.ADMIN)).isTrue();
            assertThat(user.hasRole(RoleName.DE_AGENT)).isFalse();
        }

        @Test
        @DisplayName("removeRole should remove specific role only")
        void removeRoleShouldRemoveSpecificRole() {
            User user = buildTestUser("042");
            user.addRole(RoleName.AGENT);
            user.addRole(RoleName.SUB_AGENT);
            user.removeRole(RoleName.SUB_AGENT);

            assertThat(user.hasRole(RoleName.AGENT)).isTrue();
            assertThat(user.hasRole(RoleName.SUB_AGENT)).isFalse();
        }
    }
}
