package com.paypeek.backend.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class UserReminderDto {
    private final String email;
    private final String fullName;
    private final String targetMonthName;
    private final int targetYear;
    private final List<String> foundFiles;
    private final List<String> missingFiles;

    // Campi per il Front-end
    private final String frontendMessage;
    private final boolean isComplete;

    public boolean isAllUploaded() {
        return missingFiles.isEmpty();
    }
}