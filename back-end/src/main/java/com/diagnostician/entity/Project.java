package com.diagnostician.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description = "";

    @Column(nullable = false)
    private String language = "javascript";

    @Column(nullable = false)
    private String status = "active"; // active/archived/completed/cancelled

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Integer fileCount = 0;

    private String lastAnalysisId; // 关联最新分析结果ID
}
