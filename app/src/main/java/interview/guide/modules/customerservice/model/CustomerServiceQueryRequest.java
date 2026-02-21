package interview.guide.modules.customerservice.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 客服问答请求
 */
public record CustomerServiceQueryRequest(
    @NotBlank(message = "问题不能为空")
    String question,
    List<Long> knowledgeBaseIds
) {}

