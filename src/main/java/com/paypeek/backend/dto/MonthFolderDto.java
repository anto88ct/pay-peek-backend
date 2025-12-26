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
public class MonthFolderDto {
    private String id;
    private int month; // 1-12
    private String name; // e.g., "January"
    private List<FileItemDto> files;
}
