package com.anverraglobal.insurance.auth;

import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.model.enums.UserStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Bean Validation constraints on the {@link User} entity.
 *
 * <p>These are pure unit tests — no Spring context or database required.
 * They validate that the constraint annotations ({@code @NotBlank}, {@code @Email},
 * {@code @Size}, etc.) are correctly declared on the entity fields.</p>
 *
 * <p>Business rules validated (docs/05_business_rules.md §Authentication Rules):
 * <ul>
 *   <li>Email must be present and valid format</li>
 *   <li>Password (hash) must be present</li>
 *   <li>Name must be present</li>
 *   <li>rewardPoints must be non-negative</li>
 *   <li>loginAttempts must be non-negative</li>
 * </ul>
 * </p>
 */
@DisplayName("UserEntityValidationTest")
class UserEntityValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private User validUser() {
        return User.builder()
                .email("valid@anverraglobal.com")
                .password("$2a$10$hashedpassword")
                .name("Valid User")
                .phone("+919876543210")
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Set<ConstraintViolation<User>> validate(User user) {
        return validator.validate(user);
    }

    @Nested
    @DisplayName("valid user")
    class ValidUser {

        @Test
        @DisplayName("a fully valid user should have no violations")
        void validUserShouldHaveNoViolations() {
            Set<ConstraintViolation<User>> violations = validate(validUser());
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("email validation")
    class EmailValidation {

        @Test
        @DisplayName("blank email should produce violation")
        void blankEmailShouldFail() {
            User user = validUser();
            user.setEmail("");
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("null email should produce violation")
        void nullEmailShouldFail() {
            User user = validUser();
            user.setEmail(null);
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("invalid email format should produce violation")
        void invalidEmailFormatShouldFail() {
            User user = validUser();
            user.setEmail("not-an-email");
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("email")
                    && v.getMessage().contains("valid email"));
        }

        @Test
        @DisplayName("email exceeding 255 chars should produce violation")
        void emailTooLongShouldFail() {
            User user = validUser();
            user.setEmail("a".repeat(250) + "@x.co");
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
    }

    @Nested
    @DisplayName("password validation")
    class PasswordValidation {

        @Test
        @DisplayName("blank password should produce violation")
        void blankPasswordShouldFail() {
            User user = validUser();
            user.setPassword("");
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("null password should produce violation")
        void nullPasswordShouldFail() {
            User user = validUser();
            user.setPassword(null);
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }
    }

    @Nested
    @DisplayName("name validation")
    class NameValidation {

        @Test
        @DisplayName("blank name should produce violation")
        void blankNameShouldFail() {
            User user = validUser();
            user.setName("");
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("name exceeding 255 chars should produce violation")
        void nameTooLongShouldFail() {
            User user = validUser();
            user.setName("A".repeat(256));
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }
    }

    @Nested
    @DisplayName("rewardPoints validation")
    class RewardPointsValidation {

        @Test
        @DisplayName("rewardPoints below zero should produce violation")
        void negativeRewardPointsShouldFail() {
            User user = validUser();
            user.setRewardPoints(-1);
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rewardPoints"));
        }

        @Test
        @DisplayName("rewardPoints of zero should be valid (floor rule)")
        void zeroRewardPointsShouldBeValid() {
            User user = validUser();
            user.setRewardPoints(0);
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("rewardPoints"));
        }
    }

    @Nested
    @DisplayName("loginAttempts validation")
    class LoginAttemptsValidation {

        @Test
        @DisplayName("negative loginAttempts should produce violation")
        void negativeLoginAttemptsShouldFail() {
            User user = validUser();
            user.setLoginAttempts(-1);
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("loginAttempts"));
        }
    }

    @Nested
    @DisplayName("phone size validation")
    class PhoneValidation {

        @Test
        @DisplayName("phone number exceeding 20 chars should produce violation")
        void phoneTooLongShouldFail() {
            User user = validUser();
            user.setPhone("+91" + "9".repeat(20)); // 23 chars
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
        }

        @Test
        @DisplayName("null phone should be valid (phone is optional)")
        void nullPhoneShouldBeValid() {
            User user = validUser();
            user.setPhone(null);
            Set<ConstraintViolation<User>> violations = validate(user);
            assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("phone"));
        }
    }

    @Nested
    @DisplayName("domain helpers")
    class DomainHelpers {

        @Test
        @DisplayName("isAccountLocked should return false when lockedUntil is null")
        void notLockedWhenLockedUntilIsNull() {
            User user = validUser();
            user.setLockedUntil(null);
            assertThat(user.isAccountLocked()).isFalse();
        }

        @Test
        @DisplayName("isAccountLocked should return false when lockedUntil is in the past")
        void notLockedWhenLockedUntilIsPast() {
            User user = validUser();
            user.setLockedUntil(java.time.LocalDateTime.now().minusMinutes(1));
            assertThat(user.isAccountLocked()).isFalse();
        }

        @Test
        @DisplayName("isAccountLocked should return true when lockedUntil is in the future")
        void lockedWhenLockedUntilIsFuture() {
            User user = validUser();
            user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(30));
            assertThat(user.isAccountLocked()).isTrue();
        }

        @Test
        @DisplayName("softDelete should set deleted=true and status=INACTIVE")
        void softDeleteShouldMarkUser() {
            User user = validUser();
            user.softDelete();
            assertThat(user.isDeleted()).isTrue();
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        }
    }
}
