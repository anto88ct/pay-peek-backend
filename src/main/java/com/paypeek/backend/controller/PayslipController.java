package com.paypeek.backend.controller;

import com.paypeek.backend.dto.PayslipDto;
import com.paypeek.backend.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    @PostMapping("/upload")
    public ResponseEntity<PayslipDto> uploadPayslip(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal(); // Get current user
            PayslipDto payslip = payslipService.uploadPayslip(file, userDetails.getUsername());
            return ResponseEntity.ok(payslip);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PayslipDto>> getUserPayslips(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(payslipService.getUserPayslips(userDetails.getUsername()));
    }
}
