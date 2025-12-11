package com.paypeek.backend.controller;

import com.paypeek.backend.model.Payslip;
import com.paypeek.backend.repository.PayslipRepository;
import com.paypeek.backend.service.MinIOService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final MinIOService minIOService;
    private final PayslipRepository payslipRepository;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<Payslip> uploadPayslip(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal(); // Get current user
            // In a real app we might want to store user ID in the JWT or fetch user by
            // email
            // For now, let's fetch user to get ID
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            minIOService.uploadPayslip(file.getInputStream(), filename, file.getContentType());

            Payslip payslip = Payslip.builder()
                    .userId(user.getId())
                    .uploadDate(new Date())
                    .fileUrl(filename)
                    .processingStatus("PENDING")
                    .build();

            Payslip savedPayslip = payslipRepository.save(payslip);

            return ResponseEntity.ok(savedPayslip);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Payslip>> getUserPayslips(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return ResponseEntity.ok(payslipRepository.findByUserId(user.getId()));
    }
}
