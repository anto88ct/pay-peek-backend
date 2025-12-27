package com.paypeek.backend.security;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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
}