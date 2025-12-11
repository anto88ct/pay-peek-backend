package com.paypeek.backend.repository;

import com.paypeek.backend.model.Payslip;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PayslipRepository extends MongoRepository<Payslip, String> {
    List<Payslip> findByUserId(String userId);
}
