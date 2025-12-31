package com.paypeek.backend.controller;

import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.PayslipResponseDto;
import com.paypeek.backend.model.PayrollTemplate;
import com.paypeek.backend.model.Payslip;
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
    public ResponseEntity<List<PayslipResponseDto>> massUpload(
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(payslipService.massUpload(files));
    }

    /**
     * Step 1 & 2: Carica un file per generare un nuovo Template e la relativa Payslip.
     * Questo endpoint attiva lo script Python con Gemini Vision.
     */
    @PostMapping("/build-template")
    public ResponseEntity<Payslip> buildPayslipTemplate(@RequestParam("file") MultipartFile file) {

        // Validazione formati consentiti
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Chiamata al service che orchestra Python + MongoDB
            Payslip result = payslipService.buildPayslipTemplate(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Gestione errore (es. quota API finita o errore script)
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/confirm-template")
    public ResponseEntity<Payslip> confirmTemplate(@RequestBody PayslipResponseDto dto) {
        return ResponseEntity.ok(payslipService.confirmAndSave(dto));
    }

    /**
     * API per ottenere i template di un utente specifico.
     */
    @GetMapping("/templates/{userId}")
    public ResponseEntity<List<PayrollTemplate>> getUserFileTemplate(@PathVariable String userId) {
        List<PayrollTemplate> templates = payslipService.getUserTemplates(userId);

        if (templates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Payslip>> getAllPayslips() {
        List<Payslip> payslips = payslipService.getAllUserPayslips();
        return ResponseEntity.ok(payslips);
    }
}
