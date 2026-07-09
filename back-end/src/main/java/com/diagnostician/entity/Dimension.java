package com.diagnostician.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Data
@Entity
@Table(name = "dimensions")
public class Dimension {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String analysisId; // 关联分析结果ID

    @Column(nullable = false)
    private String name; // 维度名称（如：代码复杂度）

    private Integer score; // 得分

    private Integer maxScore = 100; // 满分

    private Integer issues; // 问题数量

    // 级联删除：删除分析时删除关联维度
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysisId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Analysis analysis;
}