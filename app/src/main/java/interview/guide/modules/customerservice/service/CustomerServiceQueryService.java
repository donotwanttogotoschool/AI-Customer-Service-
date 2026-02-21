package interview.guide.modules.customerservice.service;

import interview.guide.modules.customerservice.model.CustomerServiceQueryRequest;
import interview.guide.modules.customerservice.model.CustomerServiceQueryResponse;
import interview.guide.modules.knowledgebase.service.KnowledgeBaseVectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客服问答服务
 * 支持纯模型问答和基于知识库的 RAG 问答
 */
@Slf4j
@Service
public class CustomerServiceQueryService {

    private static final int DEFAULT_TOP_K = 5;

    private final ChatClient chatClient;
    private final KnowledgeBaseVectorService vectorService;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;

    public CustomerServiceQueryService(
            ChatClient.Builder chatClientBuilder,
            KnowledgeBaseVectorService vectorService,
            @Value("classpath:prompts/customer-service-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/customer-service-user.st") Resource userPromptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.vectorService = vectorService;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
    }

    public CustomerServiceQueryResponse query(CustomerServiceQueryRequest request) {
        List<Long> knowledgeBaseIds = request.knowledgeBaseIds();
        boolean ragEnabled = knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty();

        List<Document> relevantDocs = ragEnabled
                ? vectorService.similaritySearch(request.question(), knowledgeBaseIds, DEFAULT_TOP_K)
                : List.of();

        String answer = chatClient.prompt()
                .system(buildSystemPrompt())
                .user(buildUserPrompt(request.question(), relevantDocs, ragEnabled))
                .call()
                .content();

        return new CustomerServiceQueryResponse(answer, ragEnabled, relevantDocs.size());
    }

    public Flux<String> queryStream(CustomerServiceQueryRequest request) {
        List<Long> knowledgeBaseIds = request.knowledgeBaseIds();
        boolean ragEnabled = knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty();

        List<Document> relevantDocs = ragEnabled
                ? vectorService.similaritySearch(request.question(), knowledgeBaseIds, DEFAULT_TOP_K)
                : List.of();

        return chatClient.prompt()
                .system(buildSystemPrompt())
                .user(buildUserPrompt(request.question(), relevantDocs, ragEnabled))
                .stream()
                .content()
                .doOnSubscribe(subscription ->
                        log.info("开始客服流式问答: ragEnabled={}, refs={}", ragEnabled, relevantDocs.size()))
                .doOnComplete(() ->
                        log.info("客服流式问答结束: ragEnabled={}, refs={}", ragEnabled, relevantDocs.size()));
    }

    private String buildSystemPrompt() {
        return systemPromptTemplate.render();
    }

    private String buildUserPrompt(String question, List<Document> relevantDocs, boolean ragEnabled) {
        String context = relevantDocs.isEmpty()
                ? "【未提供可用知识库内容】"
                : relevantDocs.stream().map(Document::getText).collect(Collectors.joining("\n\n---\n\n"));

        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);
        variables.put("context", context);
        variables.put("ragEnabled", ragEnabled ? "true" : "false");
        return userPromptTemplate.render(variables);
    }
}

