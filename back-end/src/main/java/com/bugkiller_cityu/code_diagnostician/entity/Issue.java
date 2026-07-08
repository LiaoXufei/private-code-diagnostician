package com.bugkiller_cityu.code_diagnostician.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Data
@Entity
@Table(name = "issues")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String analysisId; // 关联分析结果ID

    private String type; // 问题类型（如：complexity）

    private String severity; // 严重程度（critical/major/medium/minor）

    private String file; // 关联文件路径

    private Integer line; // 问题行号

    private String message; // 问题描述

    private String suggestion; // 优化建议

    // 级联删除：删除分析时删除关联问题
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysisId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Analysis analysis;
}