package com.paypeek.backend.service;
import com.paypeek.backend.dto.UserReminderDto;
import com.paypeek.backend.dto.mapper.ReminderMapper;
import com.paypeek.backend.model.Reminders;
import com.paypeek.backend.model.Payslip;
import com.paypeek.backend.model.User;
import com.paypeek.backend.repository.ReminderRepository;
import com.paypeek.backend.repository.PayslipRepository;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.security.EmailService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BatchReminderService {

    private final UserRepository userRepository;
    private final PayslipRepository payslipRepository;
    private final ReminderRepository reminderRepository; // Repository per l'entità Reminders
    private final EmailService emailService;
    private final ReminderMapper reminderMapper; // <--- Iniettato

    private final List<String> ITALIAN_MONTHS = List.of(
            "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
    );

    public BatchReminderService(UserRepository userRepository,
                                PayslipRepository payslipRepository,
                                ReminderRepository logRepository,
                                EmailService emailService,
                                ReminderMapper reminderMapper) {
        this.userRepository = userRepository;
        this.payslipRepository = payslipRepository;
        this.reminderRepository = logRepository;
        this.emailService = emailService;
        this.reminderMapper = reminderMapper;
    }

    public void processAllUsers() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.minusMonths(1);
        int targetYear = targetDate.getYear();
        String targetMonthName = ITALIAN_MONTHS.get(targetDate.getMonthValue() - 1);

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if (!user.isEmailNotifications()) continue;

            try {
                int startYear = (user.getNotificationStartYear() != null)
                        ? user.getNotificationStartYear()
                        : targetYear;

                List<String> allFoundFiles = new ArrayList<>();
                List<String> allMissingFiles = new ArrayList<>();

                // 1. LOGICA DI CALCOLO STORICO
                calculateHistory(user, startYear, today, allFoundFiles, allMissingFiles);

                // 2. COSTRUZIONE FRONTEND MESSAGE (HTML Table)
                String frontendMsg = buildHtmlMessage(allMissingFiles, startYear);

                // 3. INVIO DTO
                UserReminderDto dto = UserReminderDto.builder()
                        .email(user.getEmail())
                        .fullName(user.getFirstName() + " " + user.getLastName())
                        .targetMonthName(targetMonthName)
                        .targetYear(targetYear)
                        .foundFiles(allFoundFiles)
                        .missingFiles(allMissingFiles)
                        .isComplete(allMissingFiles.isEmpty())
                        .frontendMessage(frontendMsg)
                        .build();

                emailService.sendReminder(dto);

                // 4. SALVATAGGIO LOG VIA MAPPER
                saveLog(user, targetDate.getMonthValue(), targetYear, dto, "SUCCESS", null);

            } catch (Exception e) {
                saveLog(user, today.getMonthValue(), today.getYear(), null, "FAILED", e.getMessage());
            }
        }
    }

    private void calculateHistory(User user, int startYear, LocalDate today, List<String> found, List<String> missing) {
        for (int y = startYear; y <= today.getYear(); y++) {
            int lastMonth = (y == today.getYear()) ? today.getMonthValue() - 1 : 12;
            List<Payslip> yearPayslips = payslipRepository.findByUserIdAndYear(user.getId(), String.valueOf(y));

            List<String> uploadedMonthNames = yearPayslips.stream()
                    .map(p -> (String) ((Map<String, Object>) p.getExtractedData().get("periodo")).get("mese"))
                    .toList();

            for (int m = 0; m < lastMonth; m++) {
                String monthName = ITALIAN_MONTHS.get(m);
                String label = monthName + " " + y;
                if (uploadedMonthNames.stream().anyMatch(n -> n.equalsIgnoreCase(monthName))) {
                    found.add(label);
                } else {
                    missing.add(label);
                }
            }
        }
    }

    private String buildHtmlMessage(List<String> missingFiles, int startYear) {
        if (missingFiles.isEmpty()) {
            return "<div style='text-align: center;'><span style='font-size: 30px;'>✅</span><br>" +
                    "<strong>Tutto in ordine!</strong><p>Documenti dal " + startYear + " ad oggi caricati.</p></div>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin-bottom: 15px;'><strong>Attenzione!</strong> Documenti mancanti:</div>")
                .append("<table style='width: 100%; border-collapse: collapse; background-color: white; border-radius: 8px; overflow: hidden; font-size: 14px;'>")
                .append("<tr style='background-color: #f1f5f9; color: #475569; text-align: left;'>")
                .append("<th style='padding: 12px; border-bottom: 2px solid #e2e8f0;'>Periodo</th>")
                .append("<th style='padding: 12px; border-bottom: 2px solid #e2e8f0; text-align: center;'>Stato</th></tr>");

        for (String m : missingFiles) {
            sb.append("<tr><td style='padding: 10px 12px; border-bottom: 1px solid #f1f5f9;'>").append(m).append("</td>")
                    .append("<td style='padding: 10px 12px; border-bottom: 1px solid #f1f5f9; text-align: center;'>")
                    .append("<span style='color: #ef4444; font-weight: bold; background-color: #fee2e2; padding: 2px 8px; border-radius: 12px; font-size: 11px;'>DA CARICARE</span></td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private void saveLog(User u, Integer m, Integer y, UserReminderDto dto, String status, String error) {
        // Uso del Mapper per creare l'entità Reminders
        Reminders log = reminderMapper.toEntity(u, dto, m, y, status, error);
        reminderRepository.save(log);
    }

    /**
     * Recupera lo storico dei reminder per un utente specifico
     */
    public List<UserReminderDto> getUserReminders(String userId) {
        // 1. Recupero utente
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente con ID " + userId + " non trovato"));

        // 2. Recupero log ordinati per data decrescente
        List<Reminders> logs = reminderRepository.findByUserIdOrderBySentAtDesc(userId);

        // 3. Mapping dei risultati
        return logs.stream()
                .map(log -> {
                    String monthName = ITALIAN_MONTHS.get(log.getRefMonth() - 1);
                    return reminderMapper.toDto(user, log, monthName);
                })
                .collect(Collectors.toList());
    }
}