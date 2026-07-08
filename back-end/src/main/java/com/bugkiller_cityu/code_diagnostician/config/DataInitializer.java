package com.bugkiller_cityu.code_diagnostician.config;

import com.bugkiller_cityu.code_diagnostician.entity.*;
import com.bugkiller_cityu.code_diagnostician.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final AnalysisRepository analysisRepository;
    private final DimensionRepository dimensionRepository;
    private final IssueRepository issueRepository;

    @Override
    public void run(String... args) throws Exception {
        // 仅当数据库无数据时初始化
        if (projectRepository.count() == 0) {
            initProjects();
        }
    }

    private void initProjects() {
        // 1. 创建高质量项目
        Project highProj = new Project();
        highProj.setId("proj-high-001");
        highProj.setName("高质量电商项目");
        highProj.setDescription("示例：结构清晰、测试完善、规范良好");
        highProj.setLanguage("javascript");
        highProj.setStatus("completed");
        highProj.setCreatedAt(Instant.now().minusSeconds(2 * 24 * 3600));
        highProj.setUpdatedAt(Instant.now());
        highProj.setFileCount(5);
        projectRepository.save(highProj);

        // 2. 创建低质量项目
        Project lowProj = new Project();
        lowProj.setId("proj-low-001");
        lowProj.setName("低质量教学系统");
        lowProj.setDescription("示例：复杂度高、重复多、规范较差");
        lowProj.setLanguage("javascript");
        lowProj.setStatus("active");
        lowProj.setCreatedAt(Instant.now().minusSeconds(24 * 3600));
        lowProj.setUpdatedAt(Instant.now());
        lowProj.setFileCount(5);
        projectRepository.save(lowProj);

        // 3. 初始化项目文件
        initFiles(highProj.getId());
        initFiles(lowProj.getId());

        // 4. 初始化分析结果
        Analysis highAnalysis = initHighAnalysis(highProj.getId());
        highProj.setLastAnalysisId(highAnalysis.getId());
        projectRepository.save(highProj);

        Analysis lowAnalysis = initLowAnalysis(lowProj.getId());
        lowProj.setLastAnalysisId(lowAnalysis.getId());
        projectRepository.save(lowProj);
    }

    private void initFiles(String projectId) {
        List<ProjectFile> files = switch (projectId) {
            case "proj-high-001" -> List.of(
                    createFile(projectId, "src/main.js", 2456),
                    createFile(projectId, "src/App.vue", 3892),
                    createFile(projectId, "src/components/ProductList.vue", 5634),
                    createFile(projectId, "src/utils/formatters.js", 1234),
                    createFile(projectId, "src/api/products.js", 2876)
            );
            case "proj-low-001" -> List.of(
                    createFile(projectId, "src/main.js", 1823),
                    createFile(projectId, "src/App.vue", 2156),
                    createFile(projectId, "src/student-service.js", 3456),
                    createFile(projectId, "src/auth.js", 1567),
                    createFile(projectId, "components/StudentForm.vue", 4234)
            );
            default -> List.of();
        };
        fileRepository.saveAll(files);
    }

    private ProjectFile createFile(String projectId, String path, long size) {
        ProjectFile file = new ProjectFile();
        file.setProjectId(projectId);
        file.setFilename(path.substring(path.lastIndexOf("/") + 1));
        file.setFilePath("/uploads/" + projectId + "/" + path.replace("\\", "/"));
        file.setFileSize(size);
        file.setUploadTime(Instant.now());
        return file;
    }

    private Analysis initHighAnalysis(String projectId) {
        Analysis analysis = new Analysis();
        analysis.setProjectId(projectId);
        analysis.setOverallScore(92);
        analysis.setGrade("excellent");
        analysis.setCreatedAt(Instant.now().minusSeconds(2 * 24 * 3600));

        // 保存分析结果（获取ID）
        Analysis saved = analysisRepository.save(analysis);

        // 保存维度评分
        List<Dimension> dimensions = List.of(
                createDimension(saved.getId(), "圈复杂度分析", 88, 100, 12),
                createDimension(saved.getId(), "状态管理", 90, 100, 8),
                createDimension(saved.getId(), "代码重复度", 85, 100, 6),
                createDimension(saved.getId(), "错误处理分析", 95, 100, 2),
                createDimension(saved.getId(), "注释覆盖率分析", 89, 100, 5),
                createDimension(saved.getId(), "命名规范", 93, 100, 3),
                createDimension(saved.getId(), "结构分析", 90, 100, 4)
        );
        dimensionRepository.saveAll(dimensions);
        saved.setDimensions(dimensions);

        // 无问题
        saved.setIssues(List.of());
        return analysisRepository.save(saved);
    }

    private Analysis initLowAnalysis(String projectId) {
        Analysis analysis = new Analysis();
        analysis.setProjectId(projectId);
        analysis.setOverallScore(58);
        analysis.setGrade("poor");
        analysis.setCreatedAt(Instant.now().minusSeconds(24 * 3600));

        Analysis saved = analysisRepository.save(analysis);

        List<Dimension> dimensions = List.of(
                createDimension(saved.getId(), "圈复杂度分析", 42, 100, 38),
                createDimension(saved.getId(), "命名规范", 55, 100, 68),
                createDimension(saved.getId(), "代码重复度", 60, 100, 23),
                createDimension(saved.getId(), "错误处理分析", 65, 100, 12),
                createDimension(saved.getId(), "注释覆盖率分析", 25, 100, 45),
                createDimension(saved.getId(), "状态管理", 42, 100, 38),
                createDimension(saved.getId(), "结构分析", 68, 100, 15)
        );
        dimensionRepository.saveAll(dimensions);
        saved.setDimensions(dimensions);

        // 无问题
        saved.setIssues(List.of());
        return analysisRepository.save(saved);
    }

    private Dimension createDimension(String analysisId, String name, int score, int maxScore, int issues) {
        Dimension dim = new Dimension();
        dim.setAnalysisId(analysisId);
        dim.setName(name);
        dim.setScore(score);
        dim.setMaxScore(maxScore);
        dim.setIssues(issues);
        return dim;
    }
}