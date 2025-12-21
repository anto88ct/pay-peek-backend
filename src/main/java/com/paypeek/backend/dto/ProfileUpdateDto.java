package com.paypeek.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileUpdateDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String passkey;
    private String jobType;
    private String city;
    private String country;
    private String nationality;
    private String profileImageBase64;
    private Integer uploadedDocumentsCount;
}
