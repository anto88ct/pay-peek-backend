package com.paypeek.backend.repository;

import com.paypeek.backend.model.PayrollTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollTemplateRepository extends MongoRepository<PayrollTemplate, String> {

    Optional<PayrollTemplate> findBySignature(String signature);
    Optional<PayrollTemplate> findBySignatureAndUserId(String signature, String userId);
    List<PayrollTemplate> findByUserId(String userId);
}