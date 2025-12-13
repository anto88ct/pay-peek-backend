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
    private String firstName;
    private String lastName;
    private String job;
    private String nationality;
    private String city;
    private String country;
    private String email;
    private Role role;
    private Theme theme;
    private Language language;
}