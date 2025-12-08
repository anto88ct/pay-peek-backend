package main.java.com.paypeek.backend.repository;

import main.java.com.paypeek.backend.model.DatiBustaPaga;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DatiBustaPagaRepository extends MongoRepository<DatiBustaPaga, String> {
    List<DatiBustaPaga> findByUserId(String userId);

    List<DatiBustaPaga> findByPayslipId(String payslipId);
}
