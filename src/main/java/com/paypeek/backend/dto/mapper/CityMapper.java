package com.paypeek.backend.dto.mapper;

import com.paypeek.backend.dto.CityDto;
import com.paypeek.backend.model.City;
import org.springframework.stereotype.Component;

@Component
public class CityMapper {

    /**
     * Convert City entity to CityDto
     */
    public CityDto toDto(City city) {
        if (city == null) {
            return null;
        }

        return CityDto.builder()
                .id(city.getId())
                .codice(city.getCodice())
                .descrizione(city.getDescrizione())
                .build();
    }

    /**
     * Convert CityDto to City entity
     */
    public City toEntity(CityDto cityDto) {
        if (cityDto == null) {
            return null;
        }

        return City.builder()
                .codice(cityDto.getCodice())
                .descrizione(cityDto.getDescrizione())
                .build();
    }
}
