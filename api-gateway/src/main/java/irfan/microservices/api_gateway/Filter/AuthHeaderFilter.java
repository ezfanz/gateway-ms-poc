package irfan.microservices.api_gateway.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthHeaderFilter implements GlobalFilter, Ordered {

    private static final Log logger = LogFactory.getLog(AuthHeaderFilter.class);

    private static final int PRE_DECORATION_FILTER_ORDER = 5;

    @Value("${oauthClient.M2UGATEWAY}")
    String m2uGateway;

    private final ObjectMapper objectMapper;

    @Autowired
    public AuthHeaderFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Spring Cloud Gateway: AuthHeaderFilter executing...");

        // Log headers before mutation
        logRequestHeaders(exchange.getRequest().getHeaders(), "Before Mutation");

        exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    logger.info("Login From M2UGATEWAY: " + m2uGateway);
                    httpHeaders.add("Authorization", m2uGateway);
                    httpHeaders.add("CONTENT_TYPE", "application/json");
                });

        // Log headers after mutation
        logRequestHeaders(exchange.getRequest().getHeaders(), "After Mutation");

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return PRE_DECORATION_FILTER_ORDER + 1;
    }

    private boolean isJson(String input) {
        try {
            objectMapper.readTree(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void logRequestHeaders(HttpHeaders headers, String message) {
        logger.info(message + " - Headers:");
        headers.forEach((name, values) -> values.forEach(value -> logger.info(name + ": " + value)));
    }
}
