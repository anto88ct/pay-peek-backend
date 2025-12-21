package com.paypeek.backend.controller;

import com.paypeek.backend.dto.JobDto;
import com.paypeek.backend.dto.CityDto;
import com.paypeek.backend.dto.NationalityDto;
import com.paypeek.backend.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    /**
     * Get all jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<JobDto>> getJobs() {
        try {
            List<JobDto> jobs = supportService.getAllJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all cities
     */
    @GetMapping("/cities")
    public ResponseEntity<List<CityDto>> getCities() {
        try {
            List<CityDto> cities = supportService.getAllCities();
            return ResponseEntity.ok(cities);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all nationalities
     */
    @GetMapping("/nationalities")
    public ResponseEntity<List<NationalityDto>> getNationalities() {
        try {
            List<NationalityDto> nationalities = supportService.getAllNationalities();
            return ResponseEntity.ok(nationalities);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get city by codice
     */
    @GetMapping("/cities/{codice}")
    public ResponseEntity<CityDto> getCityByCodice(@PathVariable String codice) {
        try {
            CityDto city = supportService.getCityByCodice(codice);
            return ResponseEntity.ok(city);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get nationality by codice
     */
    @GetMapping("/nationalities/{codice}")
    public ResponseEntity<NationalityDto> getNationalityByCodice(@PathVariable String codice) {
        try {
            NationalityDto nationality = supportService.getNationalityByCodice(codice);
            return ResponseEntity.ok(nationality);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get job by descrizione
     */
    @GetMapping("/jobs/{descrizione}")
    public ResponseEntity<JobDto> getJobByDescrizione(@PathVariable String descrizione) {
        try {
            JobDto job = supportService.getJobByDescrizione(descrizione);
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}
