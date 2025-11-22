package com.bugkiller_cityu.code_diagnostician.repository;

import com.bugkiller_cityu.code_diagnostician.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    List<Issue> findByAnalysisId(String analysisId);
}
