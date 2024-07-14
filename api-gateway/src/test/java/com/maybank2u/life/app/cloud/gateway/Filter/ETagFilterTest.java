package com.maybank2u.life.app.cloud.gateway.Filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ETagFilterTest {

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ETagFilter eTagFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoFilterInternal_EligibleForETag() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        request.addHeader(HttpHeaders.IF_NONE_MATCH, "someETag");
        doAnswer(invocation -> {
            HttpServletResponse responseArg = invocation.getArgument(1);
            responseArg.setStatus(HttpStatus.OK.value());
            StreamUtils.copy("Response Body", StandardCharsets.UTF_8, responseArg.getOutputStream());
            responseArg.getOutputStream().flush();
            responseArg.flushBuffer(); // Ensure the content is fully written
            return null;
        }).when(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));

        // Act
        eTagFilter.doFilterInternal(wrappedRequest, wrappedResponse, filterChain);

        // Copy the content to the response after the filter chain
        wrappedResponse.copyBodyToResponse();

        // Assert
        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).isEqualTo("Response Body");
    }

    @Test
    public void testDoFilterInternal_NotEligibleForETag() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        doAnswer(invocation -> {
            HttpServletResponse responseArg = invocation.getArgument(1);
            responseArg.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            StreamUtils.copy("Error Response", StandardCharsets.UTF_8, responseArg.getOutputStream());
            responseArg.getOutputStream().flush();
            responseArg.flushBuffer(); // Ensure the content is fully written
            return null;
        }).when(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));

        // Act
        eTagFilter.doFilterInternal(wrappedRequest, wrappedResponse, filterChain);

        // Copy the content to the response after the filter chain
        wrappedResponse.copyBodyToResponse();

        // Assert
        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).isEqualTo("Error Response");
    }

    @Test
    public void testShouldNotFilter_WithETagHeaders() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.IF_NONE_MATCH, "someETag");

        // Act
        boolean result = eTagFilter.shouldNotFilter(request);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testShouldNotFilter_WithoutETagHeaders() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Act
        boolean result = eTagFilter.shouldNotFilter(request);

        // Assert
        assertThat(result).isTrue();
    }
}
