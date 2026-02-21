package interview.guide.modules.customerservice.model;

/**
 * 客服问答响应
 */
public record CustomerServiceQueryResponse(
    String answer,
    boolean ragEnabled,
    int referencedDocumentCount
) {}

