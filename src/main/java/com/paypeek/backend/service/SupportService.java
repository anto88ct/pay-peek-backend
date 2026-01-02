// src/main/java/com/paypeek/backend/service/SupportService.java
package com.paypeek.backend.service;

import com.paypeek.backend.dto.JobDto;
import com.paypeek.backend.dto.CityDto;
import com.paypeek.backend.dto.NationalityDto;
import com.paypeek.backend.dto.mapper.JobMapper;
import com.paypeek.backend.dto.mapper.CityMapper;
import com.paypeek.backend.dto.mapper.NationalityMapper;
import com.paypeek.backend.repository.WorkJobRepository;
import com.paypeek.backend.repository.CityRepository;
import com.paypeek.backend.repository.NationalityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final WorkJobRepository jobRepository;
    private final CityRepository cityRepository;
    private final NationalityRepository nationalityRepository;
    private final JobMapper jobMapper;
    private final CityMapper cityMapper;
    private final NationalityMapper nationalityMapper;

    /**
     * Get all jobs
     */
    public List<JobDto> getAllJobs() {
        log.info("Fetching all jobs from database");
        return jobRepository.findAll()
                .stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all cities
     */
    public List<CityDto> getAllCities() {
        log.info("Fetching all cities from database");
        return cityRepository.findAll()
                .stream()
                .map(cityMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all nationalities
     */
    public List<NationalityDto> getAllNationalities() {
        log.info("Fetching all nationalities from database");
        return nationalityRepository.findAll()
                .stream()
                .map(nationalityMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get city by codice
     */
    public CityDto getCityByCodice(String codice) {
        log.info("Fetching city with codice: {}", codice);
        return cityRepository.findByCodice(codice)
                .map(cityMapper::toDto)
                .orElseThrow(() -> new RuntimeException("City not found: " + codice));
    }

    /**
     * Get nationality by codice
     */
    public NationalityDto getNationalityByCodice(String codice) {
        log.info("Fetching nationality with codice: {}", codice);
        return nationalityRepository.findByCodice(codice)
                .map(nationalityMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Nationality not found: " + codice));
    }

    /**
     * Get job by descrizione
     */
    public JobDto getJobByDescrizione(String descrizione) {
        log.info("Fetching job with descrizione: {}", descrizione);
        return jobRepository.findByDescrizione(descrizione)
                .map(jobMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Job not found: " + descrizione));
    }

    /**
     * Check if city exists
     */
    public boolean cityExists(String codice) {
        return cityRepository.existsByCodice(codice);
    }

    /**
     * Check if nationality exists
     */
    public boolean nationalityExists(String codice) {
        return nationalityRepository.existsByCodice(codice);
    }

    /**
     * Check if job exists
     */
    public boolean jobExists(String descrizione) {
        return jobRepository.existsByDescrizione(descrizione);
    }
}
