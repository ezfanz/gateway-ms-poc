package com.maybank2u.life.app.cloud.gateway.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchange.Builder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomPostFilterTest {

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private Builder exchangeBuilder;

    @InjectMocks
    private CustomPostFilter customPostFilter = new CustomPostFilter(new String[]{"/blacklisted/url"}); // Initialize with non-null blacklist array

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.mutate()).thenReturn(exchangeBuilder); // Mock the mutate method
        when(exchangeBuilder.response(any(ServerHttpResponse.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(request.getURI()).thenReturn(URI.create("/normal/url")); // Assume default URL to prevent null pointers.

        HttpHeaders requestHeaders = new HttpHeaders(); // Ensure HttpHeaders is never null for request.
        HttpHeaders responseHeaders = new HttpHeaders(); // Ensure HttpHeaders is never null for response.

        when(request.getHeaders()).thenReturn(requestHeaders);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
    }

    @Test
    void testFilter_withBlacklistedUrl() {
        when(request.getURI()).thenReturn(URI.create("/blacklisted/url"));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = customPostFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
        verify(response, never()).writeWith(any());
    }

    @Test
    void testFilter_withErrorResponse() {
        when(request.getURI()).thenReturn(URI.create("/normal/url"));
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = customPostFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
        verify(response, never()).writeWith(any());
    }
}
