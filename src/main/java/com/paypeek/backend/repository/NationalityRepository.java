package com.paypeek.backend.repository;

import com.paypeek.backend.model.Nationality;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NationalityRepository extends MongoRepository<Nationality, String> {

    Optional<Nationality> findByCodice(String codice);

    boolean existsByCodice(String codice);
}