package com.paypeek.backend.repository;

import com.paypeek.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByEmailAndEnabled(String email, boolean enabled);

    boolean existsByEmail(String email);
    long countByEnabled(boolean enabled);
}
