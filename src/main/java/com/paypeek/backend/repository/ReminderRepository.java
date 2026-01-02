package com.paypeek.backend.repository;

import com.paypeek.backend.model.Reminders;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends MongoRepository<Reminders, String> {

    /**
     * Utile per verificare se un log esiste già prima di inviarne uno nuovo,
     * evitando duplicati per lo stesso utente nello stesso mese/anno.
     */
    Optional<Reminders> findByUserIdAndRefMonthAndRefYear(String userId, int refMonth, int refYear);

    List<Reminders> findByUserIdOrderBySentAtDesc(String userId);
}