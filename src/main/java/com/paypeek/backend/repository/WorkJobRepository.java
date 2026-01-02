package com.paypeek.backend.repository;

import com.paypeek.backend.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WorkJobRepository extends MongoRepository<Job, String> {

    Optional<Job> findByDescrizione(String descrizione);

    boolean existsByDescrizione(String descrizione);
}
