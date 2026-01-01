package com.paypeek.backend.repository;

import com.paypeek.backend.model.MonthlyNotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationLogRepository extends MongoRepository<MonthlyNotificationLog, String> {

    /**
     * Utile per verificare se un log esiste già prima di inviarne uno nuovo,
     * evitando duplicati per lo stesso utente nello stesso mese/anno.
     */
    Optional<MonthlyNotificationLog> findByUserIdAndRefMonthAndRefYear(String userId, int refMonth, int refYear);
}