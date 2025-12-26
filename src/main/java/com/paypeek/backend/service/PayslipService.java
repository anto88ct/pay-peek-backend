package com.paypeek.backend.service;

import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.MonthFolderDto;
import com.paypeek.backend.dto.YearFolderDto;
import com.paypeek.backend.dto.mapper.YearFolderMapper;
import com.paypeek.backend.model.FileItem;
import com.paypeek.backend.model.MonthFolder;
import com.paypeek.backend.model.User;
import com.paypeek.backend.model.YearFolder;
import com.paypeek.backend.repository.UserRepository;
import com.paypeek.backend.repository.YearFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayslipService {

    private final MinIOService minIOService;
    private final YearFolderRepository yearFolderRepository;
    private final UserRepository userRepository;
    private final YearFolderMapper yearFolderMapper;

    // Helper to get current User
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public List<YearFolderDto> getAllFiles() {
        User user = getCurrentUser();
        List<YearFolder> folders = yearFolderRepository.findByUserIdOrderByYearDesc(user.getId());
        return folders.stream()
                .map(yearFolderMapper::toDto)
                .collect(Collectors.toList());
    }

    public YearFolderDto createYear(int year) {
        User user = getCurrentUser();

        Optional<YearFolder> existing = yearFolderRepository.findByUserIdAndYear(user.getId(), year);
        if (existing.isPresent()) {
            throw new RuntimeException("Year folder already exists for year: " + year);
        }

        YearFolder yearFolder = YearFolder.builder()
                .userId(user.getId())
                .year(year)
                .color(getRandomColor())
                .months(new ArrayList<>())
                .build();

        yearFolder = yearFolderRepository.save(yearFolder);
        return yearFolderMapper.toDto(yearFolder);
    }

    public MonthFolderDto createMonth(String yearId, int month) {
        User user = getCurrentUser();

        YearFolder yearFolder = yearFolderRepository.findById(yearId)
                .orElseThrow(() -> new RuntimeException("Year folder not found"));

        if (!yearFolder.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to year folder");
        }

        boolean monthExists = yearFolder.getMonths().stream().anyMatch(m -> m.getMonth() == month);
        if (monthExists) {
            throw new RuntimeException("Month " + month + " already exists in year " + yearFolder.getYear());
        }

        MonthFolder monthFolder = MonthFolder.builder()
                .id(UUID.randomUUID().toString())
                .month(month)
                .name(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .files(new ArrayList<>())
                .build();

        yearFolder.getMonths().add(monthFolder);
        yearFolder.getMonths().sort(Comparator.comparingInt(MonthFolder::getMonth).reversed());

        yearFolderRepository.save(yearFolder);

        return yearFolderMapper.toDto(monthFolder);
    }

    public FileItemDto uploadFile(String monthFolderId, MultipartFile file) {
        User user = getCurrentUser();

        // Find YearFolder containing the month
        YearFolder yearFolder = yearFolderRepository.findByMonthId(monthFolderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (!yearFolder.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to folder");
        }

        MonthFolder monthFolder = yearFolder.getMonths().stream()
                .filter(m -> m.getId().equals(monthFolderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Month folder not found in year folder"));

        // Upload to MinIO
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        try {
            minIOService.uploadPayslip(file.getInputStream(), filename, file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Error uploading to MinIO", e);
        }

        FileItem fileItem = FileItem.builder()
                .id(UUID.randomUUID().toString())
                .name(file.getOriginalFilename())
                .url(filename)
                .type("pdf") // Assuming PDF
                .size(file.getSize())
                .uploadDate(new Date())
                .build();

        monthFolder.getFiles().add(fileItem);
        yearFolderRepository.save(yearFolder);

        return yearFolderMapper.toDto(fileItem);
    }

    public List<FileItemDto> massUpload(List<MultipartFile> files) {
        User user = getCurrentUser();
        List<FileItemDto> uploadedFiles = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (MultipartFile file : files) {
            int year = today.getYear();
            int month = today.getMonthValue();

            // Ensure Year Exists
            YearFolder yearFolder = yearFolderRepository.findByUserIdAndYear(user.getId(), year)
                    .orElseGet(() -> {
                        YearFolder newYear = YearFolder.builder()
                                .userId(user.getId())
                                .year(year)
                                .color(getRandomColor())
                                .months(new ArrayList<>())
                                .build();
                        return yearFolderRepository.save(newYear);
                    });

            // Ensure Month Exists
            MonthFolder monthFolder = yearFolder.getMonths().stream()
                    .filter(m -> m.getMonth() == month)
                    .findFirst()
                    .orElseGet(() -> {
                        MonthFolder newMonth = MonthFolder.builder()
                                .id(UUID.randomUUID().toString())
                                .month(month)
                                .name(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                                .files(new ArrayList<>())
                                .build();
                        yearFolder.getMonths().add(newMonth);
                        return newMonth;
                    });

            // Upload
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            try {
                minIOService.uploadPayslip(file.getInputStream(), filename, file.getContentType());
            } catch (IOException e) {
                log.error("Failed to upload file " + file.getOriginalFilename(), e);
                continue; // Skip this file
            }

            FileItem fileItem = FileItem.builder()
                    .id(UUID.randomUUID().toString())
                    .name(file.getOriginalFilename())
                    .url(filename)
                    .type("pdf")
                    .size(file.getSize())
                    .uploadDate(new Date())
                    .build();

            monthFolder.getFiles().add(fileItem);
            uploadedFiles.add(yearFolderMapper.toDto(fileItem));

            yearFolderRepository.save(yearFolder);
        }

        return uploadedFiles;
    }

    private String getRandomColor() {
        String[] colors = { "#FF5733", "#33FF57", "#3357FF", "#F1C40F", "#9B59B6", "#E67E22" };
        return colors[new Random().nextInt(colors.length)];
    }
}
