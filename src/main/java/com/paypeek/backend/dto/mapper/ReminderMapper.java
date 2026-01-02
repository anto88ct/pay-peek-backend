package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.UserReminderDto;
import com.paypeek.backend.model.Reminders;
import com.paypeek.backend.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderMapper {

    /**
     * Trasforma l'esito dell'elaborazione in un'entità Reminders per il database.
     */
    public Reminders toEntity(User user, UserReminderDto dto, Integer month, Integer year, String status, String error) {
        if (user == null) return null;

        Reminders entity = new Reminders();
        entity.setUserId(user.getId());
        entity.setRefMonth(month);
        entity.setRefYear(year);
        entity.setSentAt(LocalDateTime.now());
        entity.setDeliveryStatus(status);
        entity.setErrorDetails(error);

        if (dto != null) {
            entity.setPresentPayslips(dto.getFoundFiles());
            entity.setMissingPayslips(dto.getMissingFiles());
            entity.setFrontendMessage(dto.getFrontendMessage());
        }

        return entity;
    }

    /**
     * Trasforma un log esistente e i dati utente nel DTO per l'invio email o il frontend.
     */
    public UserReminderDto toDto(User user, Reminders entity, String monthName) {
        if (user == null || entity == null) return null;

        return UserReminderDto.builder()
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .targetMonthName(monthName)
                .targetYear(entity.getRefYear())
                .foundFiles(entity.getPresentPayslips())
                .missingFiles(entity.getMissingPayslips())
                .frontendMessage(entity.getFrontendMessage())
                .isComplete(entity.getMissingPayslips() == null || entity.getMissingPayslips().isEmpty())
                .build();
    }
}