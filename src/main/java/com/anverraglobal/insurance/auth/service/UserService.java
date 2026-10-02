package com.anverraglobal.insurance.auth.service;

import com.anverraglobal.insurance.auth.entity.User;
import com.anverraglobal.insurance.auth.repository.UserRepository;
import com.anverraglobal.insurance.mapper.UserMapper;
import com.anverraglobal.insurance.model.dto.auth.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.userToUserDTO(user);
    }

    @Transactional
    public UserDTO updateUserProfile(Long userId, UserDTO userDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDTO.getName() != null) {
            user.setName(userDTO.getName());
        }
        if (userDTO.getProfileImage() != null) {
            String url = userDTO.getProfileImage();
            if (url.startsWith("/uploads/") || url.startsWith("http://") || url.startsWith("https://")) {
                user.setProfileImage(url);
            } else {
                throw new IllegalArgumentException("Invalid profile image URL format.");
            }
        }
        
        // Update other profile fields as necessary in a full implementation.
        // For this task, we focus on profile image.

        userRepository.save(user);
        return userMapper.userToUserDTO(user);
    }
}
