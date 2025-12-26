package com.paypeek.backend.repository;

import com.paypeek.backend.model.YearFolder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YearFolderRepository extends MongoRepository<YearFolder, String> {

    List<YearFolder> findByUserIdOrderByYearDesc(String userId);

    Optional<YearFolder> findByUserIdAndYear(String userId, int year);

    @Query("{ 'months._id': ?0 }")
    Optional<YearFolder> findByMonthId(String monthId);
}
