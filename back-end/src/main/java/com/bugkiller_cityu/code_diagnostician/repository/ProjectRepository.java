package com.bugkiller_cityu.code_diagnostician.repository;

import com.bugkiller_cityu.code_diagnostician.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
}
