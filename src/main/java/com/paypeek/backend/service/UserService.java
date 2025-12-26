// src/main/java/com/paypeek/backend/service/UserService.java
package com.paypeek.backend.service;

import com.paypeek.backend.dto.UserDto;
import com.paypeek.backend.dto.ProfileUpdateDto;
import com.paypeek.backend.dto.PasswordResetDto;
import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Theme;
import com.paypeek.backend.dto.mapper.UserMapper;
import com.paypeek.backend.exception.ResourceNotFoundException;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MinIOService minIOService;

    /**
     * Get user profile by email
     */
    public UserDto getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserDto userDto = userMapper.toDto(user);

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            try {
                String objectName = minIOService.extractObjectName(user.getProfileImageUrl());
                String freshPresignedUrl = minIOService.generatePresignedUrl(objectName);
                userDto.setProfileImageUrl(freshPresignedUrl);
            } catch (Exception e) {
                log.warn("Could not regenerate URL", e);
            }
        }

        return userDto;
    }

    /**
     * Get user by ID
     */
    public UserDto getUserById(String userId) {
        log.info("Fetching user by ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return userMapper.toDto(user);
    }

    /**
     * Update user profile
     */
    public UserDto updateUserProfile(String userId, ProfileUpdateDto profileUpdateDto) {
        log.info("Updating profile for userId: {}", userId);

        //ricaviamo l'utente corrente
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Update firstName
        if (profileUpdateDto.getFirstName() != null) {
            user.setFirstName(profileUpdateDto.getFirstName());
        }

        // Update lastName
        if (profileUpdateDto.getLastName() != null) {
            user.setLastName(profileUpdateDto.getLastName());
        }

        // Update email (check for duplicates)
        if (profileUpdateDto.getEmail() != null && !profileUpdateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(profileUpdateDto.getEmail())) {
                throw new RuntimeException("Email already in use: " + profileUpdateDto.getEmail());
            }
            user.setEmail(profileUpdateDto.getEmail());
        }

        // Update jobType
        if (profileUpdateDto.getJobType() != null) {
            user.setJob(profileUpdateDto.getJobType());
        }

        // Update city
        if (profileUpdateDto.getCity() != null) {
            user.setCity(profileUpdateDto.getCity());
        }

        // Update country
        if (profileUpdateDto.getCountry() != null) {
            user.setCountry(profileUpdateDto.getCountry());
        }

        // Update nationality
        if (profileUpdateDto.getNationality() != null) {
            user.setNationality(profileUpdateDto.getNationality());
        }

        // Update password (hash it)
        if (profileUpdateDto.getPassword() != null && !profileUpdateDto.getPassword().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(profileUpdateDto.getPassword());
            user.setPasswordHash(hashedPassword);
            log.info("Password updated for userId: {}", userId);
        }

        // Update passkey
        if (profileUpdateDto.getPasskey() != null) {
            user.setPassKeyEnabled(true);
        }

        // Update profileImageBase64
        if (profileUpdateDto.getProfileImageBase64() != null) {
            String imageUrl = saveProfileImageFromBase64(profileUpdateDto.getProfileImageBase64(), user.getId());
            user.setProfileImageUrl(imageUrl);
        }

        // Update uploadedDocumentsCount
        if (profileUpdateDto.getUploadedDocumentsCount() != null) {
            user.setUploadedDocumentsCount(profileUpdateDto.getUploadedDocumentsCount());
        }

        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for userId: {}", userId);

        return userMapper.toDto(savedUser);
    }


    /**
     * Upload profile image from MultipartFile
     */
    public UserDto uploadProfileImage(String userId, MultipartFile file) {
        log.info("Uploading profile image for user: {}", userId);

        if (file.isEmpty() || file.getContentType() == null) {
            throw new IllegalArgumentException("File non valido");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        try {
            // Upload a MinIO
            String imageUrl = minIOService.uploadFile(file, "users/" + user.getId() + "/profile");

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                minIOService.deleteFile(user.getProfileImageUrl());
            }

            user.setProfileImageUrl(imageUrl);
            user.setUpdatedAt(Instant.now());

            User savedUser = userRepository.save(user);
            log.info("Profile image uploaded successfully for user: {}", userId);

            return userMapper.toDto(savedUser);
        } catch (Exception e) {
            log.error("Error uploading profile image for user: {}", userId, e);
            throw new RuntimeException("Failed to upload profile image: " + e.getMessage());
        }
    }

    /**
     * Remove profile image
     */
    public UserDto removeProfileImage(String email) {
        log.info("Removing profile image for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        try {
            // Delete from MinIO if URL exists
            if (user.getProfileImageUrl() != null) {
                minIOService.deleteFile(user.getProfileImageUrl());
            }

            user.setProfileImageUrl(null);
            user.setUpdatedAt(Instant.now());

            User savedUser = userRepository.save(user);
            log.info("Profile image removed successfully for user: {}", email);

            return userMapper.toDto(savedUser);
        } catch (Exception e) {
            log.error("Error removing profile image for user: {}", email, e);
            throw new RuntimeException("Failed to remove profile image: " + e.getMessage());
        }
    }

    /**
     * Reset password
     */
    public void resetPassword(String email, PasswordResetDto passwordResetDto) {
        log.info("Resetting password for user: {}", email);

        // Validate passwords match
        if (!passwordResetDto.getNewPassword().equals(passwordResetDto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Validate password strength
        if (!isPasswordStrong(passwordResetDto.getNewPassword())) {
            throw new RuntimeException("Password does not meet security requirements");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        String hashedPassword = passwordEncoder.encode(passwordResetDto.getNewPassword());
        user.setPasswordHash(hashedPassword);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Password reset successfully for user: {}", email);
    }

    /**
     * Delete user account
     */
    public void deleteUserAccount(String email, String password) {
        log.info("Deleting account for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // Verify password before deletion
        if (password != null && !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        try {
            // Delete profile image from MinIO if exists
            if (user.getProfileImageUrl() != null) {
                minIOService.deleteFile(user.getProfileImageUrl());
            }

            // Delete user from database
            userRepository.deleteById(user.getId());
            log.info("Account deleted successfully for user: {}", email);
        } catch (Exception e) {
            log.error("Error deleting account for user: {}", email, e);
            throw new RuntimeException("Failed to delete account: " + e.getMessage());
        }
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin(String email) {
        log.info("Updating last login for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.setLastLogin(Instant.now());
        userRepository.save(user);
    }

    /**
     * Save profile image from Base64 string
     */
    private String saveProfileImageFromBase64(String base64Image, String userId) {
        try {
            // Remove data:image/jpeg;base64, prefix if present
            String base64Data = base64Image.contains(",")
                    ? base64Image.split(",")[1]
                    : base64Image;

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Upload to MinIO
            String fileName = "users/" + userId + "/profile-" + System.currentTimeMillis() + ".jpg";
            return minIOService.uploadByteArray(imageBytes, fileName);
        } catch (Exception e) {
            log.error("Error saving profile image from base64 for user: {}", userId, e);
            throw new RuntimeException("Failed to save profile image: " + e.getMessage());
        }
    }
    public UserDto updateLanguage(String userId, Language language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setLanguage(language);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    public UserDto updateTheme(String userId, Theme theme) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setTheme(theme);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * Validate password strength
     * Requirements: min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        return hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
    }

    /**
     * Check if user exists by email
     */
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
