package com.bugkiller_cityu.code_diagnostician.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "analyses")
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId; // 关联项目ID

    private Integer overallScore; // 总分

    private String grade; // 评级（excellent/good/poor）

    @CreationTimestamp
    private Instant createdAt;

    // 关联维度评分（OneToMany：一个分析包含多个维度）
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dimension> dimensions;

    // 关联问题列表（OneToMany：一个分析包含多个问题）
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Issue> issues;

    // 级联删除：删除项目时删除关联分析
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;
}
