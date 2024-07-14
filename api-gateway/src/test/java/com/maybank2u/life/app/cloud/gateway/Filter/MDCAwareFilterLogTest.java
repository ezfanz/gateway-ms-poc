package com.maybank2u.life.app.cloud.gateway.Filter;

import com.maybank2u.life.util.HeaderUtil;
import com.maybank2u.life.util.StringUtil;
import com.maybank2u.life.util.URLUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HeaderUtil.class, StringUtil.class, URLUtil.class})
public class MDCAwareFilterLogTest {

    @InjectMocks
    private MDCAwareFilterLog filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Before
    public void setUp() {
        // Initialize PowerMockito mocks for static methods
        PowerMockito.mockStatic(HeaderUtil.class);
        PowerMockito.mockStatic(StringUtil.class);
        PowerMockito.mockStatic(URLUtil.class);

        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoFilterInternal_WithBearerToken() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("authorization")).thenReturn("bearer token");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        PowerMockito.when(URLUtil.isValidUrl("http://localhost/test")).thenReturn(true);
        PowerMockito.when(HeaderUtil.getUserIdBearerToken("bearer token")).thenReturn(123L);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertEquals("123", MDC.get("req.userId"));
        assertEquals("/test", MDC.get("req.requestURI"));
        assertEquals("http://localhost/test", MDC.get("req.requestURL"));
    }

    @Test
    public void testDoFilterInternal_WithoutAuthorizationHeader() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("authorization")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get("req.userId"));
    }

    @Test
    public void testDoFilterInternal_WithInvalidBearerToken() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("authorization")).thenReturn("invalid token");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get("req.userId"));
    }

    @Test
    public void testDoFilterInternal_WithInvalidUrl() throws IOException, ServletException {
        // Arrange
        when(request.getHeader("authorization")).thenReturn("bearer token");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        PowerMockito.when(URLUtil.isValidUrl("http://localhost/test")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response);
        assertNull(MDC.get("req.userId"));
    }

    @Test
    public void testClearMDC() {
        // Arrange
        MDC.put("req.userId", "123");

        // Act
        filter.clearMDC();

        // Assert
        assertNull(MDC.get("req.userId"));
    }
}
