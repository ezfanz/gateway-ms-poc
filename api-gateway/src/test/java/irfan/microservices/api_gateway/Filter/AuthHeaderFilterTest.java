package irfan.microservices.api_gateway.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthHeaderFilterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders headers;

    @Mock
    private RequestPath requestPath;

    @InjectMocks
    private AuthHeaderFilter authHeaderFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authHeaderFilter.m2uGateway = "test-oauth-token";

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.contextPath()).thenReturn(mock(PathContainer.class));
    }

    @Test
    void testFilter() {
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders mutatedHeaders = new HttpHeaders();
                mutatedHeaders.addAll(super.getHeaders());
                mutatedHeaders.add("Authorization", "test-oauth-token");
                mutatedHeaders.add("CONTENT_TYPE", "application/json");
                return mutatedHeaders;
            }
        };

        ServerWebExchange mutatedExchange = new ServerWebExchangeDecorator(exchange) {
            @Override
            public ServerHttpRequest getRequest() {
                return mutatedRequest;
            }
        };

        Mono<Void> result = authHeaderFilter.filter(mutatedExchange, chain);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }
}
