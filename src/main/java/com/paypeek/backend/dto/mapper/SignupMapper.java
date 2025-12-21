package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.dto.SignupDto;
import com.paypeek.backend.dto.UserDto;
import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Theme;
import com.paypeek.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class SignupMapper {

    public User toEntity(SignupDto signupDto) {
        if (signupDto == null) {
            return null;
        }

        return User.builder()
                .firstName(signupDto.getFirstName())
                .lastName(signupDto.getLastName())
                .job(signupDto.getJob())
                .nationality(signupDto.getNationality())
                .city(signupDto.getCity())
                .country(signupDto.getCountry())
                .email(signupDto.getEmail())
                // passwordHash impostato nel Service dopo encode()
                .build();
    }

    public AuthResponse toAuthResponse(User user, String jwtToken) {
        if (user == null) {
            return null;
        }

        return AuthResponse.builder()
                .token(jwtToken)
                .user(toUserDto(user))
                .build();
    }

    public UserDto toUserDto(User user) {
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
}