package com.paypeek.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItemDto {
    private String id;
    private String name;
    private String url;
    private String type; // 'pdf'
    private Long size;
    private Instant uploadDate;
}
