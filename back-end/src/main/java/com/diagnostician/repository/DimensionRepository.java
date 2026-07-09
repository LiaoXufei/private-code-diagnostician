package com.diagnostician.repository;

import com.diagnostician.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimensionRepository extends JpaRepository<Dimension, String> {
    List<Dimension> findByAnalysisId(String analysisId);
}
