package com.diagnostician.config;

import com.diagnostician.entity.*;
import com.diagnostician.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final AnalysisRepository analysisRepository;
    private final DimensionRepository dimensionRepository;
    private final AppProperties appProperties;

    @Override
    public void run(String... args) throws Exception {
        // 仅当数据库无数据时初始化
        if (projectRepository.count() == 0) {
            initProjects();
        }
    }

    private void initProjects() throws IOException {
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

        // 3. 初始化项目文件（DB + 物理文件）
        initFiles(highProj.getId(), true);
        initFiles(lowProj.getId(), false);

        // 4. 初始化分析结果
        Analysis highAnalysis = initHighAnalysis(highProj.getId());
        highProj.setLastAnalysisId(highAnalysis.getId());
        projectRepository.save(highProj);

        Analysis lowAnalysis = initLowAnalysis(lowProj.getId());
        lowProj.setLastAnalysisId(lowAnalysis.getId());
        projectRepository.save(lowProj);
    }

    private void initFiles(String projectId, boolean highQuality) throws IOException {
        Path baseDir = appProperties.projectDir(projectId);
        Files.createDirectories(baseDir);

        List<SampleFile> samples = highQuality ? highQualitySamples() : lowQualitySamples();
        List<ProjectFile> files = new ArrayList<>();
        for (SampleFile sample : samples) {
            String storedFilename = UUID.randomUUID() + "_" + sample.filename();
            Path abs = baseDir.resolve(storedFilename);
            byte[] bytes = sample.content().getBytes(StandardCharsets.UTF_8);
            Files.write(abs, bytes);

            ProjectFile file = new ProjectFile();
            file.setProjectId(projectId);
            file.setFilename(sample.filename());
            file.setFilePath("/uploads/" + projectId + "/" + storedFilename);
            file.setFileSize((long) bytes.length);
            file.setUploadTime(Instant.now());
            files.add(file);
        }
        fileRepository.saveAll(files);
    }

    private List<SampleFile> highQualitySamples() {
        return List.of(
                new SampleFile("main.js", """
                        // Entry point for the demo shop
                        import { createApp } from './App.js';
                        import { formatPrice } from './formatters.js';

                        const app = createApp();
                        app.mount('#app');
                        console.log('Shop ready', formatPrice(19.9));
                        """),
                new SampleFile("App.js", """
                        export function createApp() {
                          return {
                            mount(selector) {
                              const el = document.querySelector(selector);
                              if (el) {
                                el.textContent = 'Demo Shop';
                              }
                            }
                          };
                        }
                        """),
                new SampleFile("ProductList.js", """
                        export function renderProducts(products) {
                          return products
                            .filter((p) => p && p.name)
                            .map((p) => ({ id: p.id, name: p.name, price: p.price || 0 }));
                        }
                        """),
                new SampleFile("formatters.js", """
                        export function formatPrice(value) {
                          const n = Number(value);
                          if (Number.isNaN(n)) return '0.00';
                          return n.toFixed(2);
                        }
                        """),
                new SampleFile("products.js", """
                        export async function fetchProducts(api) {
                          try {
                            const res = await api.get('/products');
                            return Array.isArray(res.data) ? res.data : [];
                          } catch (err) {
                            console.error('fetchProducts failed', err);
                            return [];
                          }
                        }
                        """)
        );
    }

    private List<SampleFile> lowQualitySamples() {
        return List.of(
                new SampleFile("main.js", """
                        var x=1;function a(){if(x){if(x){if(x){if(x){return x+1}}}}}
                        function b(){return a()+a()+a()+a()+a()}
                        console.log(b());
                        """),
                new SampleFile("App.js", """
                        function doStuff(d){var r=[];for(var i=0;i<d.length;i++){if(d[i]){r.push(d[i]);}}return r;}
                        function doStuff2(d){var r=[];for(var i=0;i<d.length;i++){if(d[i]){r.push(d[i]);}}return r;}
                        module.exports={doStuff:doStuff,doStuff2:doStuff2};
                        """),
                new SampleFile("student-service.js", """
                        function getStudent(id){var s=null;try{s=db.find(id)}catch(e){}return s}
                        function getStudent2(id){var s=null;try{s=db.find(id)}catch(e){}return s}
                        function save(s){db.save(s);db.save(s);db.save(s)}
                        """),
                new SampleFile("auth.js", """
                        function login(u,p){if(u=='admin'&&p=='123456')return true;return false}
                        function check(t){if(t){if(t.length>0){if(t!='x'){return true}}}return false}
                        """),
                new SampleFile("StudentForm.js", """
                        function render(f){document.body.innerHTML=f.name+f.age+f.score+f.name+f.age}
                        function validate(f){if(!f)return;if(!f.name)return;if(!f.age)return;return true}
                        """)
        );
    }

    private Analysis initHighAnalysis(String projectId) {
        Analysis analysis = new Analysis();
        analysis.setProjectId(projectId);
        analysis.setOverallScore(92);
        analysis.setGrade("excellent");
        analysis.setCreatedAt(Instant.now().minusSeconds(2 * 24 * 3600));

        Analysis saved = analysisRepository.save(analysis);

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
        return saved;
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
        return saved;
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

    private record SampleFile(String filename, String content) {}
}
