package com.paypeek.backend.service;

import com.paypeek.backend.dto.PayslipDto;
import com.paypeek.backend.mapper.PayslipMapper;
import com.paypeek.backend.model.Payslip;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.PayslipRepository;
import com.paypeek.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.io.IOException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final MinIOService minIOService;
    private final PayslipRepository payslipRepository;
    private final UserRepository userRepository;
    private final PayslipMapper payslipMapper;

    public PayslipDto uploadPayslip(MultipartFile file, String userEmail) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        try {
            minIOService.uploadPayslip(file.getInputStream(), filename, file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Error uploading file to MinIO", e);
        }

        Payslip payslip = Payslip.builder()
                .userId(user.getId())
                .uploadDate(new Date())
                .fileUrl(filename)
                .processingStatus("PENDING")
                .build();

        Payslip savedPayslip = payslipRepository.save(payslip);
        return payslipMapper.toDto(savedPayslip);
    }

    public List<PayslipDto> getUserPayslips(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Payslip> payslips = payslipRepository.findByUserId(user.getId());
        return payslips.stream()
                .map(payslipMapper::toDto)
                .collect(Collectors.toList());
    }
}
