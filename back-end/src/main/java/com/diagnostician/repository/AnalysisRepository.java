package com.diagnostician.repository;

import com.diagnostician.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, String> {
    List<Analysis> findByProjectIdOrderByCreatedAtDesc(String projectId);
    Optional<Analysis> findTopByProjectIdOrderByCreatedAtDesc(String projectId);
}
