package com.paypeek.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthFolder {
    private String id;
    private int month; // 1-12
    private String name; // e.g., "January"

    @Builder.Default
    private List<FileItem> files = new ArrayList<>();
}
