package com.paypeek.backend.repository;

import com.paypeek.backend.model.City;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CityRepository extends MongoRepository<City, String> {

    Optional<City> findByCodice(String codice);

    boolean existsByCodice(String codice);
}
