package com.paypeek.backend.dto;

import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Role;
import com.paypeek.backend.dto.enums.Theme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String jobType;
    private String city;
    private String country;
    private String profileImageUrl;
    private Preferences preferences;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private boolean passKeyEnabled;
    private boolean biometricEnabled;
    private java.util.Date lastLogin;

    // UserDto extensions
    private String password;
    private String passkey;
    private Integer uploadedDocumentsCount;
    private String nationality;
    private String profileImageBase64;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Preferences {
        private String language;
        private String theme;
        private boolean emailNotifications;
    }
}