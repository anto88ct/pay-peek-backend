package com.paypeek.backend.controller;

import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.MonthFolderDto;
import com.paypeek.backend.dto.YearFolderDto;
import com.paypeek.backend.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    @GetMapping
    public ResponseEntity<List<YearFolderDto>> getAllFiles() {
        return ResponseEntity.ok(payslipService.getAllFiles());
    }

    @PostMapping("/years")
    public ResponseEntity<YearFolderDto> createYear(
            @RequestParam int year) {
        return ResponseEntity.ok(payslipService.createYear(year));
    }

    @PostMapping("/years/{yearId}/months")
    public ResponseEntity<MonthFolderDto> createMonth(
            @PathVariable String yearId,
            @RequestParam int month) {
        return ResponseEntity.ok(payslipService.createMonth(yearId, month));
    }

    @PostMapping("/folders/{folderId}/upload")
    public ResponseEntity<FileItemDto> uploadFile(
            @PathVariable String folderId,
            @RequestParam("file") MultipartFile file) {
        if (!file.getContentType().equals("application/pdf")) {
            // Optional: return bad request loop
        }
        return ResponseEntity.ok(payslipService.uploadFile(folderId, file));
    }

    @PostMapping("/payslips/upload")
    public ResponseEntity<List<FileItemDto>> massUpload(
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(payslipService.massUpload(files));
    }
}
