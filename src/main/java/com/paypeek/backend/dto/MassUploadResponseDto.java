package com.paypeek.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MassUploadResponseDto {
    private List<PayslipResponseDto> successes;
    private List<ErrorResponseDto> errors;
}