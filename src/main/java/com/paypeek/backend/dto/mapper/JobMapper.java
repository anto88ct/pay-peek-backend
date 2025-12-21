package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.JobDto;
import com.paypeek.backend.model.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    /**
     * Convert Job entity to JobDto
     */
    public JobDto toDto(Job job) {
        if (job == null) {
            return null;
        }

        return JobDto.builder()
                .id(job.getId())
                .descrizione(job.getDescrizione())
                .build();
    }

    /**
     * Convert JobDto to Job entity
     */
    public Job toEntity(JobDto jobDto) {
        if (jobDto == null) {
            return null;
        }

        return Job.builder()
                .descrizione(jobDto.getDescrizione())
                .build();
    }
}
