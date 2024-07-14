package com.maybank2u.life.app.cloud.gateway.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimpleFilterTest {

    @Mock
    private DiscoveryClient discoveryClient;

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

    @Mock
    private ServiceInstance serviceInstance;

    @InjectMocks
    private SimpleFilter simpleFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getPath()).thenReturn(requestPath);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(requestPath.toString()).thenReturn("/P-API/some/path");
        when(requestPath.contextPath()).thenReturn(mock(org.springframework.http.server.PathContainer.class));

        when(serviceInstance.getUri()).thenReturn(URI.create("http://localhost:8080"));
        when(discoveryClient.getInstances("mae-adp-service")).thenReturn(Collections.singletonList(serviceInstance));
    }

    @Test
    void testFilter_withAggregatorPrefixUri() {
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
            @Override
            public URI getURI() {
                return URI.create("http://localhost:8080/mae/some/path");
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders mutatedHeaders = new HttpHeaders();
                mutatedHeaders.addAll(super.getHeaders());
                mutatedHeaders.add("Authorization", "test-oauth-token");
                mutatedHeaders.add("CONTENT_TYPE", "application/json");
                return mutatedHeaders;
            }

            @Override
            public HttpMethod getMethod() {
                return super.getMethod();
            }
        };

        ServerWebExchange mutatedExchange = new ServerWebExchangeDecorator(exchange) {
            @Override
            public ServerHttpRequest getRequest() {
                return mutatedRequest;
            }
        };

        when(exchange.mutate()).thenReturn(mutatedExchange.mutate());

        Mono<Void> result = simpleFilter.filter(mutatedExchange, chain);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilter_withoutAggregatorPrefixUri() {
        when(requestPath.toString()).thenReturn("/some/other/path");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = simpleFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }
}
