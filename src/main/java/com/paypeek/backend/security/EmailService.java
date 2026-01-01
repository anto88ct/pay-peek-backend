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

    public void sendReminder(UserReminderDto dto) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(dto.getEmail());
        helper.setSubject("Riepilogo Documenti: " + dto.getTargetMonthName() + " " + dto.getTargetYear());

        // Costruzione del testo (Puoi usare un template HTML se preferisci)
        StringBuilder body = new StringBuilder();
        body.append("<h1>Ciao ").append(dto.getFullName()).append("</h1>");
        body.append("<p>").append(dto.getFrontendMessage()).append("</p>");

        if (!dto.getFoundFiles().isEmpty()) {
            body.append("<p><b>File caricati:</b> ").append(String.join(", ", dto.getFoundFiles())).append("</p>");
        }

        if (!dto.isComplete()) {
            body.append("<p style='color:red;'><b>Documenti mancanti:</b> ")
                    .append(String.join(", ", dto.getMissingFiles())).append("</p>");
            body.append("<p>Ti preghiamo di caricarli al più presto sul portale.</p>");
        }

        helper.setText(body.toString(), true); // 'true' abilita l'HTML

        mailSender.send(message);
    }
}