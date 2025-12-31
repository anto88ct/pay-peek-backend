package com.paypeek.backend.repository;

import com.paypeek.backend.model.Payslip;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayslipRepository extends MongoRepository<Payslip, String> {
    List<Payslip> findByTemplateId(String templateId);
    List<Payslip> findByUserIdOrderByCreatedAtDesc(String userId);
}