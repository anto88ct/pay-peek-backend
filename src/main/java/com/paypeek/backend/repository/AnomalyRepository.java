package main.java.com.paypeek.backend.repository;

import main.java.com.paypeek.backend.model.Anomaly;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AnomalyRepository extends MongoRepository<Anomaly, String> {
    List<Anomaly> findByUserId(String userId);
}
