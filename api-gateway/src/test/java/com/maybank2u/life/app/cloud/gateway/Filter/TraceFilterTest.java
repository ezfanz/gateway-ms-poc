package com.maybank2u.life.app.cloud.gateway.Filter;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class TraceFilterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TraceFilter traceFilter;

    private TraceContext traceContext;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a real TraceContext instance
        traceContext = TraceContext.newBuilder()
                .traceId(1L)
                .spanId(1L)
                .sampled(true)
                .build();
    }

    @Test
    public void testDoFilter_WithCurrentSpan() throws IOException, ServletException {
        // Arrange
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);

        // Act
        traceFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response).addHeader("TRACE-ID", traceContext.traceIdString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDoFilter_WithoutCurrentSpan() throws IOException, ServletException {
        // Arrange
        when(tracer.currentSpan()).thenReturn(null);

        // Act
        traceFilter.doFilter(request, response, filterChain);

        // Assert
        verify(response, never()).addHeader(anyString(), anyString());
        verify(filterChain).doFilter(request, response);
    }
}
