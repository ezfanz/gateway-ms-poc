package irfan.microservices.api_gateway.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
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

    @InjectMocks
    private CustomPostFilter customPostFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getURI()).thenReturn(URI.create("/normal/url")); // Assume default URL to prevent null pointers.

        HttpHeaders headers = new HttpHeaders(); // Ensure HttpHeaders is never null.
        when(response.getHeaders()).thenReturn(headers);
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
