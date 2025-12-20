package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.SignupDto;
import com.paypeek.backend.dto.UserDto;
import com.paypeek.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto.Preferences prefs = UserDto.Preferences.builder()
                .language(user.getLanguage() != null ? user.getLanguage().name().toLowerCase() : "it")
                .theme(user.getTheme() != null ? user.getTheme().name().toLowerCase() : "system") // Default to system
                                                                                                  // or light? User
                                                                                                  // entity defaults to
                                                                                                  // LIGHT.
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

    public User toEntity(SignupDto dto) {
        if (dto == null) {
            return null;
        }
        return User.builder()
                .email(dto.getEmail())
                // Password should be handled by service before setting/building if encryption
                // involves here,
                // but usually handled in service. Here we just map raw data.
                .build();
    }
}
