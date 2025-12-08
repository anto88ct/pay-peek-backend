package main.java.com.paypeek.backend.controller;

import com.paypeek.backend.dto.AuthRequest;
import com.paypeek.backend.dto.AuthResponse;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;

        @PostMapping("/register")
        public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        return ResponseEntity.badRequest().build(); // Or improved error handling
                }

                var user = User.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .role("USER")
                                .theme("light")
                                .language("it")
                                .build();

                userRepository.save(user); // Save first to get ID

                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                var jwtToken = jwtService.generateToken(userDetails);

                return ResponseEntity.ok(AuthResponse.builder()
                                .token(jwtToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .build());
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));
                var user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow();
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                var jwtToken = jwtService.generateToken(userDetails);

                return ResponseEntity.ok(AuthResponse.builder()
                                .token(jwtToken)
                                .userId(user.getId())
                                .email(user.getEmail())
                                .build());
        }
}
