package com.bugkiller_cityu.code_diagnostician.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Data
@Entity
@Table(name = "file_analysis_results")
public class FileAnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String analysisId; // 关联分析结果ID

    @Column(nullable = false)
    private String filename; // 原始文件名（前端展示用）

    private Integer score; // 文件质量分（0-100，越高越好）

    private Integer linesOfCode; // 代码行数

    private Integer issues; // 问题数量

    private Integer complexity; // 圈复杂度问题数

    // 级联删除：删除分析时删除关联文件结果
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysisId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Analysis analysis;
}
