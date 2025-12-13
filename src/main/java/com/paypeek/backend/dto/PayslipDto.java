package com.paypeek.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayslipDto {
    private String id;
    private String userId;
    private Date uploadDate;
    private String fileUrl;
    private String processingStatus;
}
