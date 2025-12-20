package com.paypeek.backend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@Slf4j
public class AuditLogger {

    /**
     * Logga eventi biometrici per audit trail
     */
    public void log(String email, String action, String status) {
        log.info("AUDIT: email={}, action={}, status={}, timestamp={}",
                email, action, status, LocalDateTime.now());

        // ✅ TODO: Salvare su MongoDB collection 'audit_logs'
        // auditRepository.save(new AuditLog(email, action, status));
    }
}
