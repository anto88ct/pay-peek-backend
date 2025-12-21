package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.ProfileUpdateDto;
import com.paypeek.backend.dto.SignupDto;
import com.paypeek.backend.dto.UserDto;
import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Theme;
import com.paypeek.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /**
     * Convert User entity to UserDto
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto.Preferences prefs = UserDto.Preferences.builder()
                .language(user.getLanguage() != null ? user.getLanguage() : Language.IT)
                .theme(user.getTheme() != null ? user.getTheme() : Theme.SYSTEM)
                .biometric(user.isBiometricEnabled())
                .emailNotifications(user.isEmailNotifications())
                .build();

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .jobType(user.getJob())
                .nationality(user.getNationality())
                .city(user.getCity())
                .country(user.getCountry())
                .profileImageUrl(user.getProfileImageUrl())
                .preferences(prefs)
                .emailVerified(user.isEmailVerified())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .passKeyEnabled(user.isPassKeyEnabled())
                .biometricEnabled(user.isBiometricEnabled())
                .lastLogin(user.getLastLogin())
                .uploadedDocumentsCount(user.getUploadedDocumentsCount())
                .build();
    }

    /**
     * Convert SignupDto to User entity
     */
    public User toEntity(SignupDto dto) {
        if (dto == null) {
            return null;
        }

        return User.builder()
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .job(dto.getJob())
                .nationality(dto.getNationality())
                .city(dto.getCity())
                .country(dto.getCountry())
                // passwordHash handled in Service with PasswordEncoder
                // Language and Theme default values set in User entity @Builder.Default
                .build();
    }

    /**
     * Map ProfileUpdateDto fields to existing User entity (partial update)
     */
    public void updateEntityFromDto(ProfileUpdateDto dto, User user) {
        if (dto == null || user == null) {
            return;
        }

        if (dto.getFirstName() != null && !dto.getFirstName().isEmpty()) {
            user.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null && !dto.getLastName().isEmpty()) {
            user.setLastName(dto.getLastName());
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            user.setEmail(dto.getEmail());
        }

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            // Password should be hashed in service before this call
            user.setPasswordHash(dto.getPassword());
        }

        if (dto.getPasskey() != null && !dto.getPasskey().isEmpty()) {
            user.setPassKeyEnabled(true);
        }

        if (dto.getJobType() != null && !dto.getJobType().isEmpty()) {
            user.setJob(dto.getJobType());
        }

        if (dto.getCity() != null && !dto.getCity().isEmpty()) {
            user.setCity(dto.getCity());
        }

        if (dto.getCountry() != null && !dto.getCountry().isEmpty()) {
            user.setCountry(dto.getCountry());
        }

        if (dto.getNationality() != null && !dto.getNationality().isEmpty()) {
            user.setNationality(dto.getNationality());
        }

        if (dto.getProfileImageBase64() != null && !dto.getProfileImageBase64().isEmpty()) {
            user.setProfileImageUrl(dto.getProfileImageBase64());
        }

        if (dto.getUploadedDocumentsCount() != null) {
            user.setUploadedDocumentsCount(dto.getUploadedDocumentsCount());
        }
    }
}
