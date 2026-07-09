package com.diagnostician.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "project_files")
public class ProjectFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId; // 关联项目ID

    @Column(nullable = false)
    private String filename; // 原始文件名

    @Column(nullable = false)
    private String filePath; // 存储路径（如：/uploads/proj-xxx/xxx.jpg）

    private Long fileSize; // 文件大小（字节）

    @CreationTimestamp
    private Instant uploadTime;

    // 级联删除：删除项目时删除关联文件
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectId", referencedColumnName = "id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;
}