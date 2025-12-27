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
import com.paypeek.backend.security.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MinIOService minIOService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;


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

    public UserDto getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return userMapper.toDto(user);
    }

    public UserDto updateUserProfile(String userId, ProfileUpdateDto profileUpdateDto) {

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

        return userMapper.toDto(savedUser);
    }


    /**
     * Upload profile image from MultipartFile
     */
    public UserDto uploadProfileImage(String userId, MultipartFile file) {

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

            return userMapper.toDto(savedUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image: " + e.getMessage());
        }
    }

    /**
     * Remove profile image
     */
    public UserDto removeProfileImage(String email) {

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

            return userMapper.toDto(savedUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove profile image: " + e.getMessage());
        }
    }

    public void createPasswordResetToken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();

            user.setResetToken(token);
            user.setResetTokenExpiry(Instant.now().plus(30, ChronoUnit.MINUTES));
            userRepository.save(user);

            // Costruiamo il link dinamicamente
            // Assicurati che il path (/reset-password) sia lo stesso definito in Angular
            String resetLink = String.format("%s/auth/reset-password?token=%s", frontendUrl, token);

            emailService.sendResetLink(user.getEmail(), resetLink);
        });
    }

    public void resetPasswordWithToken(String token, String newPassword) {
        // Cerchiamo l'utente che ha quel token E non è scaduto
        User user = userRepository.findByResetToken(token)
                .filter(u -> u.getResetTokenExpiry() != null && u.getResetTokenExpiry().isAfter(Instant.now()))
                .orElseThrow(() -> new RuntimeException("Token non valido o scaduto"));

        // Aggiorniamo la password
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Pulizia token per renderlo monouso
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }

    /**
     * Delete user account
     */
    public void deleteUserAccount(String email, String password) {

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
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete account: " + e.getMessage());
        }
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin(String email) {
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
