package com.paypeek.backend.controller;

import com.paypeek.backend.dto.UserReminderDto;
import com.paypeek.backend.service.BatchReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reminder")
@RequiredArgsConstructor
public class ReminderController {

    private final BatchReminderService reminderService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserReminderDto>> getUserReminders(@PathVariable String userId) {
        List<UserReminderDto> reminders = reminderService.getUserReminders(userId);
        return ResponseEntity.ok(reminders);
    }
}
