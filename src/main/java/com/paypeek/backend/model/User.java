package com.paypeek.backend.model;

import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Role;
import com.paypeek.backend.dto.enums.Theme;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User extends BaseEntity {

    private String firstName;
    private String lastName;
    private String job;
    private String nationality;
    private String city;
    private String country;
    private String email;
    private String passwordHash;
    private Role role;
    private Language language;
    private Theme theme;
    @lombok.Builder.Default
    private boolean enabled = true;
    private String profileImageUrl;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private boolean passKeyEnabled;
    private boolean biometricEnabled;
    private Instant lastLogin;
    @lombok.Builder.Default
    private Integer uploadedDocumentsCount = 0;
    private boolean emailNotifications;
}
