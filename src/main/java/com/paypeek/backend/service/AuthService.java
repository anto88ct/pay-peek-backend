package com.paypeek.backend.service;

import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.dto.LoginDto;
import com.paypeek.backend.dto.SignupDto;
import com.paypeek.backend.dto.enums.Language;
import com.paypeek.backend.dto.enums.Role;
import com.paypeek.backend.dto.enums.Theme;
import com.paypeek.backend.exception.EmailAlreadyRegisteredException;
import com.paypeek.backend.mapper.SignupMapper;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SignupMapper signupMapper;

    public AuthResponse register(SignupDto request) {
        // 1. Check duplicati
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException("Email già registrata");
        }

        // 2. Map + Set password + defaults
        var user = signupMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setTheme(Theme.LIGHT);
        user.setLanguage(Language.ITALIAN);

        // 3. Save + LOG
        var savedUser = userRepository.save(user);
        log.info("👤 Utente creato - ID: {}, Email: {}, Nome: {} {}",
                savedUser.getId(), savedUser.getEmail(),
                savedUser.getFirstName(), savedUser.getLastName());

        // 4. JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        // 5. Response con Mapper
        return signupMapper.toAuthResponse(savedUser, jwtToken);
    }

    public AuthResponse login(LoginDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }
}
