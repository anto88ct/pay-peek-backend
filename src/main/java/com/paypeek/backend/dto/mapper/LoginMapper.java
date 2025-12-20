package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.LoginDto;
import com.paypeek.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class LoginMapper {

    // Usually LoginDto doesn't map directly to a full Entity for persistence,
    // but user requested "toEntity" and "toDto" for every DTO.

    public User toEntity(LoginDto dto) {
        if (dto == null) {
            return null;
        }
        return User.builder()
                .email(dto.getEmail())
                // Password is raw here, usually not set directly to passwordHash without
                // encoding
                .build();
    }

    public LoginDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return LoginDto.builder()
                .email(user.getEmail())
                .build();
    }
}
