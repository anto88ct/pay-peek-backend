package com.paypeek.backend.controller;

import com.paypeek.backend.dto.PasswordResetDto;
import com.paypeek.backend.dto.PasswordResetRequestDto;
import com.paypeek.backend.dto.ProfileUpdateDto;
import com.paypeek.backend.dto.UserDto;
import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Theme;
import com.paypeek.backend.exception.ResourceNotFoundException;
import com.paypeek.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserDto> getProfile(@PathVariable String userId) {
        try {
            UserDto user = userService.getProfile(userId);
            return ResponseEntity.ok(user);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    @PutMapping("/update/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestBody ProfileUpdateDto profileUpdateDto) {
        try {
            UserDto updatedUser = userService.updateUserProfile(profileUpdateDto.getUserId(), profileUpdateDto);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/profile/image")
    public ResponseEntity<UserDto> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        UserDto updatedUser = userService.uploadProfileImage(userId, file);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        try {
            UserDto user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/profile")
    public ResponseEntity<Void> deleteAccount(
            @RequestParam(required = false) String password,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userService.deleteUserAccount(userDetails.getUsername(), password);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/password-reset-request")
    public ResponseEntity<Void> requestReset(@RequestBody @Valid PasswordResetRequestDto dto) {
        userService.createPasswordResetToken(dto.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset-complete")
    public ResponseEntity<Void> completeReset(@RequestBody @Valid PasswordResetDto dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().build();
        }
        userService.resetPasswordWithToken(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/preferences/{userId}/language")
    public ResponseEntity<UserDto> updateLanguage(
            @RequestParam Language language,
            @PathVariable String userId) {
        try {
            UserDto updatedUser = userService.updateLanguage(userId, language);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/preferences/{userId}/theme")
    public ResponseEntity<UserDto> updateTheme(
            @RequestParam Theme theme,
            @PathVariable String userId) {
        try {
            UserDto updatedUser = userService.updateTheme(userId, theme);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
