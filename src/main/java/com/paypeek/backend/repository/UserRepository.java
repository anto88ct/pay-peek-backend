package com.paypeek.backend.repository;

import com.paypeek.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);

    /**
     * Find user by email and enabled status
     */
    Optional<User> findByEmailAndEnabled(String email, boolean enabled);

    /**
     * Count total users
     */
    long countByEnabled(boolean enabled);
}
