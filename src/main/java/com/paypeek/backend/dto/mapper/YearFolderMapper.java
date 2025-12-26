package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.FileItemDto;
import com.paypeek.backend.dto.MonthFolderDto;
import com.paypeek.backend.dto.YearFolderDto;
import com.paypeek.backend.model.FileItem;
import com.paypeek.backend.model.MonthFolder;
import com.paypeek.backend.model.YearFolder;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class YearFolderMapper {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public YearFolderDto toDto(YearFolder entity) {
        if (entity == null)
            return null;

        return YearFolderDto.builder()
                .id(entity.getId())
                .year(entity.getYear())
                .color(entity.getColor())
                .months(toMonthDtoList(entity.getMonths()))
                .build();
    }

    private List<MonthFolderDto> toMonthDtoList(List<MonthFolder> months) {
        if (months == null)
            return Collections.emptyList();
        return months.stream().map(this::toDto).collect(Collectors.toList());
    }

    public MonthFolderDto toDto(MonthFolder entity) {
        if (entity == null)
            return null;

        return MonthFolderDto.builder()
                .id(entity.getId())
                .month(entity.getMonth())
                .name(entity.getName())
                .files(toFileDtoList(entity.getFiles()))
                .build();
    }

    private List<FileItemDto> toFileDtoList(List<FileItem> files) {
        if (files == null)
            return Collections.emptyList();
        return files.stream().map(this::toDto).collect(Collectors.toList());
    }

    public FileItemDto toDto(FileItem entity) {
        if (entity == null)
            return null;

        return FileItemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .type(entity.getType())
                .size(entity.getSize())
                .uploadDate(entity.getUploadDate() != null ? dateFormat.format(entity.getUploadDate()) : null)
                .build();
    }
}
