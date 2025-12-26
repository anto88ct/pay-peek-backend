package com.paypeek.backend.controller;

import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    @PostMapping("/folders/{folderId}/upload")
    public ResponseEntity<FileItemDto> uploadFile(
            @PathVariable String folderId,
            @RequestParam("file") MultipartFile file) {

        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(payslipService.uploadFile(folderId, file));
    }

    @PostMapping("/payslips/upload")
    public ResponseEntity<List<FileItemDto>> massUpload(
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(payslipService.massUpload(files));
    }
}
