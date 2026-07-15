package com.anverraglobal.insurance.auth.service;

import com.anverraglobal.insurance.auth.dto.*;
import com.anverraglobal.insurance.auth.entity.*;
import com.anverraglobal.insurance.auth.repository.RefreshTokenRepository;
import com.anverraglobal.insurance.auth.repository.UserRepository;
import com.anverraglobal.insurance.exception.BadRequestException;
import com.anverraglobal.insurance.exception.ResourceNotFoundException;
import com.anverraglobal.insurance.exception.UnauthorizedException;
import com.anverraglobal.insurance.model.enums.OtpPurpose;
import com.anverraglobal.insurance.model.enums.RoleName;
import com.anverraglobal.insurance.model.enums.UserStatus;
import com.anverraglobal.insurance.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            return generateTokensAndResponse(user);

        } catch (AuthenticationException e) {
            log.warn("Login failed for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        otpService.validateVerifiedOtpExists(request.getPhone(), OtpPurpose.REGISTRATION);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String normalizedPhone = otpService.normalizePhone(request.getPhone());
        if (userRepository.findByPhoneAndDeletedFalse(normalizedPhone).isPresent()) {
            throw new BadRequestException("Phone number already registered");
        }

        User user = new User();
        user.setName(request.getFirstName() + " " + request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        Set<UserRole> userRoles = new HashSet<>();
        for (RoleName roleName : request.getRoles()) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(roleName);
            userRoles.add(userRole);
        }
        user.setRoles(userRoles);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setAddressLine1(request.getAddressLine1());
        profile.setAddressLine2(request.getAddressLine2());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setPostalCode(request.getPincode());
        profile.setCountry(request.getCountry());
        profile.setAgentCode(request.getAgentCode());
        profile.setBrokerCode(request.getBrokerCode());
        
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        
        return generateTokensAndResponse(savedUser);
    }
    
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String normalizedPhone = otpService.normalizePhone(request.getPhone());
        if (userRepository.findByPhoneAndDeletedFalse(normalizedPhone).isPresent()) {
            throw new BadRequestException("Phone number already registered");
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE); 

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(request.getRole());
        
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setRoles(userRoles);
        
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        
        return generateTokensAndResponse(savedUser);
    }

    @Transactional
    public AuthResponse loginWithOtp(OtpVerifyRequest request) {
        otpService.verifyOtp(request.getPhone(), request.getOtp(), OtpPurpose.LOGIN);
        
        String normalizedPhone = otpService.normalizePhone(request.getPhone());
        User user = userRepository.findByPhoneAndDeletedFalse(normalizedPhone)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this phone number"));
                
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("Account is suspended");
        }
        
        return generateTokensAndResponse(user);
    }
    
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
                
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token has expired");
        }
        
        if (token.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        
        User user = token.getUser();
        com.anverraglobal.insurance.security.UserPrincipal principal = com.anverraglobal.insurance.security.UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String newAccessToken = jwtService.generateToken(auth);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .expiresIn(86400000L) // Default 1 day for now
                .user(mapToDto(user))
                .build();
    }
    
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null && !refreshTokenValue.isEmpty()) {
            refreshTokenRepository.findByToken(refreshTokenValue)
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    private AuthResponse generateTokensAndResponse(User user) {
        com.anverraglobal.insurance.security.UserPrincipal principal = com.anverraglobal.insurance.security.UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String accessToken = jwtService.generateToken(auth);
        String refreshTokenString = UUID.randomUUID().toString();
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenString);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7)); 
        refreshTokenRepository.save(refreshToken);
        
        user.setLastLogin(LocalDateTime.now());
        user.setLoginAttempts(0);
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .expiresIn(86400000L) // Default 1 day for now
                .user(mapToDto(user))
                .build();
    }
    
    private AuthResponse.UserDto mapToDto(User user) {
        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(ur -> ur.getRole()).collect(Collectors.toSet()))
                .build();
    }
}
