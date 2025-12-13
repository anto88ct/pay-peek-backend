package com.paypeek.backend.mapper;

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
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .theme(user.getTheme())
                .language(user.getLanguage())
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
