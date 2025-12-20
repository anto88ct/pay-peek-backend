package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.PayslipDto;
import com.paypeek.backend.model.Payslip;
import org.springframework.stereotype.Component;

@Component
public class PayslipMapper {

    public PayslipDto toDto(Payslip entity) {
        if (entity == null) {
            return null;
        }
        return PayslipDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .uploadDate(entity.getUploadDate())
                .fileUrl(entity.getFileUrl())
                .processingStatus(entity.getProcessingStatus())
                .build();
    }

    public Payslip toEntity(PayslipDto dto) {
        if (dto == null) {
            return null;
        }
        return Payslip.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .uploadDate(dto.getUploadDate())
                .fileUrl(dto.getFileUrl())
                .processingStatus(dto.getProcessingStatus())
                .build();
    }
}
