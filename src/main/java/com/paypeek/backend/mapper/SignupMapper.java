package com.paypeek.backend.mapper;

import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.dto.SignupDto;
import com.paypeek.backend.dto.UserDto;
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
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .job(user.getJob())
                .nationality(user.getNationality())
                .city(user.getCity())
                .country(user.getCountry())
                .role(user.getRole())
                .theme(user.getTheme())
                .language(user.getLanguage())
                .build();
    }
}