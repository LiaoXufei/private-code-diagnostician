package com.bugkiller_cityu.code_diagnostician.repository;

import com.bugkiller_cityu.code_diagnostician.entity.FileAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAnalysisResultRepository extends JpaRepository<FileAnalysisResult, String> {
    List<FileAnalysisResult> findByAnalysisId(String analysisId);
}
