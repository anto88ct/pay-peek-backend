package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.NationalityDto;
import com.paypeek.backend.model.Nationality;
import org.springframework.stereotype.Component;

@Component
public class NationalityMapper {

    /**
     * Convert Nationality entity to NationalityDto
     */
    public NationalityDto toDto(Nationality nationality) {
        if (nationality == null) {
            return null;
        }

        return NationalityDto.builder()
                .id(nationality.getId())
                .codice(nationality.getCodice())
                .descrizione(nationality.getDescrizione())
                .build();
    }

    /**
     * Convert NationalityDto to Nationality entity
     */
    public Nationality toEntity(NationalityDto nationalityDto) {
        if (nationalityDto == null) {
            return null;
        }

        return Nationality.builder()
                .codice(nationalityDto.getCodice())
                .descrizione(nationalityDto.getDescrizione())
                .build();
    }
}
