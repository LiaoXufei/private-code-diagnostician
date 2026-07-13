package com.diagnostician.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatClient agentChatClient;
    private final AiContextService contextService;

    public Flux<String> streamDiagnosis(String projectId, String userPrompt) {
        contextService.requireProject(projectId);
        String prompt = """
                请对项目 %s 执行完整的代码质量诊断。
                用户补充说明：%s

                重要：调用任何工具时，projectId 参数必须使用 "%s"，禁止使用其他值或编造项目ID。
                请主动调用工具获取文件列表、分析结果和问题清单，然后输出诊断报告。
                """.formatted(projectId, userPrompt != null ? userPrompt : "无", projectId);

        return agentChatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
