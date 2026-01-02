package com.paypeek.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderDto {
    private String email;
    private String fullName;
    private String targetMonthName;
    private int targetYear;
    private List<String> foundFiles;
    private List<String> missingFiles;
    private String frontendMessage;
    private boolean isComplete;
    public boolean isAllUploaded() {
        return missingFiles == null || missingFiles.isEmpty();
    }
}
