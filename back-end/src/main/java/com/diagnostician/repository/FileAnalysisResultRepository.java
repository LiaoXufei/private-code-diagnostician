package com.diagnostician.repository;

import com.diagnostician.entity.FileAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAnalysisResultRepository extends JpaRepository<FileAnalysisResult, String> {
    List<FileAnalysisResult> findByAnalysisId(String analysisId);

    @Query("SELECT COALESCE(SUM(f.linesOfCode), 0) FROM FileAnalysisResult f WHERE f.analysisId = :analysisId")
    long sumLinesOfCodeByAnalysisId(@Param("analysisId") String analysisId);
}
