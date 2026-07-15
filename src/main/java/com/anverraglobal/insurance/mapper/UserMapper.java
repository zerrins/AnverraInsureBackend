package com.anverraglobal.insurance.mapper;

import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.entity.UserProfile;
import com.anverraglobal.insurance.model.dto.auth.UserDTO;
import com.anverraglobal.insurance.model.dto.auth.UserProfileDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO userToUserDTO(User user) {
        if (user == null) {
            return null;
        }

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getRole().name())
                .collect(Collectors.toList());

        return UserDTO.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .roles(roles)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .profile(profileToProfileDTO(user.getProfile()))
                .build();
    }

    public UserProfileDTO profileToProfileDTO(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        return UserProfileDTO.builder()
                .agentCode(profile.getAgentCode())
                .brokerCode(profile.getBrokerCode())
                .city(profile.getCity())
                .state(profile.getState())
                .build();
    }
}
