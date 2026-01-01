package com.paypeek.backend.service;
import com.paypeek.backend.dto.UserReminderDto;
import com.paypeek.backend.model.MonthlyNotificationLog;
import com.paypeek.backend.model.Payslip;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.NotificationLogRepository;
import com.paypeek.backend.repository.PayslipRepository;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.security.EmailService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BatchNotificationService {

    private final UserRepository userRepository;
    private final PayslipRepository payslipRepository;
    private final NotificationLogRepository logRepository;
    private final EmailService emailService;

    public BatchNotificationService(UserRepository userRepository,
                                    PayslipRepository payslipRepository,
                                    NotificationLogRepository logRepository,
                                    EmailService emailService) {
        this.userRepository = userRepository;
        this.payslipRepository = payslipRepository;
        this.logRepository = logRepository;
        this.emailService = emailService;
    }

    public void processAllUsers() {
        LocalDate targetDate = LocalDate.now().minusMonths(1);

        // Per il LOG (Integer)
        int monthInt = targetDate.getMonthValue();
        int yearInt = targetDate.getYear();

        // Per la QUERY (String come da tuo JSON)
        String monthStr = targetDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN);
        monthStr = monthStr.substring(0, 1).toUpperCase() + monthStr.substring(1); // "Febbraio"
        String yearStr = String.valueOf(yearInt); // "2025"

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                // Cerchiamo con le STRINGHE
                List<Payslip> uploaded = payslipRepository.findByUserAndPeriod(user.getId(), monthStr, yearStr);
                boolean isComplete = !uploaded.isEmpty();

                UserReminderDto dto = UserReminderDto.builder()
                        .email(user.getEmail())
                        .fullName(user.getFirstName() + " " + user.getLastName())
                        .targetMonthName(monthStr)
                        .targetYear(yearInt)
                        .foundFiles(uploaded.stream().map(Payslip::getFileName).toList())
                        .missingFiles(isComplete ? List.of() : List.of("Busta Paga " + monthStr))
                        .isComplete(isComplete)
                        .frontendMessage(isComplete ? "Documento presente." : "Documento mancante.")
                        .build();

                emailService.sendReminder(dto);

                // Salviamo con gli INTEGER
                saveLog(user, monthInt, yearInt, dto, "SUCCESS", null);

            } catch (Exception e) {
                saveLog(user, monthInt, yearInt, null, "FAILED", e.getMessage());
            }
        }
    }

    private void saveLog(User u, Integer m, Integer y, UserReminderDto dto, String status, String error) {
        MonthlyNotificationLog log = new MonthlyNotificationLog();
        log.setUserId(u.getId());
        log.setRefMonth(m); // Ora riceve Integer
        log.setRefYear(y);  // Ora riceve Integer
        log.setSentAt(LocalDateTime.now());
        log.setDeliveryStatus(status);
        log.setErrorDetails(error);
        if (dto != null) {
            log.setPresentPayslips(dto.getFoundFiles());
            log.setMissingPayslips(dto.getMissingFiles());
        }
        logRepository.save(log);
    }
}
