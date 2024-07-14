package com.maybank2u.life.app.cloud.gateway.Filter;

import com.maybank2u.life.app.cloud.gateway.Config.AkamaiUrlConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AkamaiHeaderFilterTest {

    @InjectMocks
    private AkamaiHeaderFilter akamaiHeaderFilter;

    @Mock
    private AkamaiUrlConfig akamaiUrlConfig;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoFilterInternal_WithCacheHeader() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/cached/path");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> config = new HashMap<>();
        config.put("/cached/path", "max-age=3600");

        when(akamaiUrlConfig.getConfig()).thenReturn(config);

        // Act
        akamaiHeaderFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertEquals("max-age=3600", response.getHeader("Cache-Control"));
    }

    @Test
    public void testDoFilterInternal_WithoutCacheHeader() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/non-cached/path");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> config = new HashMap<>();
        config.put("/cached/path", "max-age=3600");

        when(akamaiUrlConfig.getConfig()).thenReturn(config);

        // Act
        akamaiHeaderFilter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertEquals(null, response.getHeader("Cache-Control"));
    }

    @Test
    public void testShouldNotFilter_WithCacheHeader() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/cached/path");

        Map<String, String> config = new HashMap<>();
        config.put("/cached/path", "max-age=3600");

        when(akamaiUrlConfig.getConfig()).thenReturn(config);

        // Act
        boolean result = akamaiHeaderFilter.shouldNotFilter(request);

        // Assert
        assertEquals(false, result);
    }

    @Test
    public void testShouldNotFilter_WithoutCacheHeader() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/non-cached/path");

        Map<String, String> config = new HashMap<>();
        config.put("/cached/path", "max-age=3600");

        when(akamaiUrlConfig.getConfig()).thenReturn(config);

        // Act
        boolean result = akamaiHeaderFilter.shouldNotFilter(request);

        // Assert
        assertEquals(true, result);
    }
}
