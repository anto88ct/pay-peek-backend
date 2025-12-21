package com.paypeek.backend.controller;

import com.paypeek.backend.dto.PasswordResetDto;
import com.paypeek.backend.dto.ProfileUpdateDto;
import com.paypeek.backend.dto.UserDto;
import com.paypeek.backend.exception.ResourceNotFoundException;
import com.paypeek.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(
            @RequestBody PasswordResetDto passwordResetDto,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userService.resetPassword(userDetails.getUsername(), passwordResetDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
