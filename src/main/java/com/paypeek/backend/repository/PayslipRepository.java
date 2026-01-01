package com.paypeek.backend.repository;

import com.paypeek.backend.model.Payslip;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayslipRepository extends MongoRepository<Payslip, String> {
    List<Payslip> findByTemplateId(String templateId);
    List<Payslip> findByUserIdOrderByCreatedAtDesc(String userId);

    // Cerchiamo nei campi annidati della Map extractedData
    @Query("{ 'userId': ?0, 'extractedData.periodo.mese': ?1, 'extractedData.periodo.anno': ?2 }")
    List<Payslip> findByUserAndPeriod(String userId, String mese, String anno);
}