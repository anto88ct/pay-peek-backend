package com.paypeek.backend.repository;

import com.paypeek.backend.model.UserCredential;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCredentialRepository extends MongoRepository<UserCredential, String> {
    List<UserCredential> findByUserId(String userId);
    Optional<UserCredential> findByCredentialId(String credentialId);
    void deleteByUserIdAndCredentialId(String userId, String credentialId);
}
