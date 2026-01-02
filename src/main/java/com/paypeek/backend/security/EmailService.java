package com.paypeek.backend.security;

import com.paypeek.backend.dto.UserReminderDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendResetLink(String toEmail, String resetLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Preparazione del contesto Thymeleaf (le variabili per l'HTML)
            Context context = new Context();
            context.setVariable("resetLink", resetLink);

            // Trasforma il template HTML in una stringa compilata
            String htmlContent = templateEngine.process("password-reset-email", context);

            helper.setTo(toEmail);
            helper.setSubject("Reset della Password - PayPeek");
            helper.setText(htmlContent, true); // 'true' indica che è HTML
            helper.setFrom("no-reply@paypeek.com");

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Errore durante l'invio dell'email", e);
        }
    }

    public void sendReminder(UserReminderDto dto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // Configurazione helper per email HTML
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            // 1. Prepariamo i dati per il template HTML
            Context context = new Context();
            context.setVariable("fullName", dto.getFullName());
            context.setVariable("targetMonthName", dto.getTargetMonthName());
            context.setVariable("targetYear", dto.getTargetYear());
            context.setVariable("foundFiles", dto.getFoundFiles());     // Lista storica
            context.setVariable("missingFiles", dto.getMissingFiles()); // Lista storica
            context.setVariable("frontendMessage", dto.getFrontendMessage());
            context.setVariable("isComplete", dto.isComplete());

            // 2. Elaborazione del file src/main/resources/templates/reminder.html
            String htmlContent = templateEngine.process("reminder", context);

            // 3. Configurazione parametri email
            helper.setTo(dto.getEmail());
            helper.setSubject("Riepilogo Documenti PayPeek - " + dto.getTargetMonthName() + " " + dto.getTargetYear());
            helper.setText(htmlContent, true); // true indica che il contenuto è HTML
            helper.setFrom("noreply@paypeek.com");

            mailSender.send(message);

        } catch (MessagingException e) {
            // Logghiamo l'errore per il batch
            throw new RuntimeException("Errore durante l'invio dell'email a " + dto.getEmail(), e);
        }
    }
}