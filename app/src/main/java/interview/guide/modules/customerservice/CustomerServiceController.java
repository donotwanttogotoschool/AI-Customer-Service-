package interview.guide.modules.customerservice;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.customerservice.model.CustomerServiceQueryRequest;
import interview.guide.modules.customerservice.model.CustomerServiceQueryResponse;
import interview.guide.modules.customerservice.service.CustomerServiceQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 客服问答控制器
 * 提供本地调试友好的测试接口
 */
@RestController
@RequiredArgsConstructor
public class CustomerServiceController {

    private final CustomerServiceQueryService queryService;

    @PostMapping("/api/customer-service/query")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 10)
    public Result<CustomerServiceQueryResponse> query(@Valid @RequestBody CustomerServiceQueryRequest request) {
        return Result.success(queryService.query(request));
    }

    @PostMapping(value = "/api/customer-service/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Flux<String> queryStream(@Valid @RequestBody CustomerServiceQueryRequest request) {
        return queryService.queryStream(request);
    }
}

