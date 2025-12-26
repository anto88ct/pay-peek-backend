package com.paypeek.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItem {
    private String id;
    private String name;
    private String url;
    private String type; // "pdf"
    private Long size;
    private Date uploadDate;
}
