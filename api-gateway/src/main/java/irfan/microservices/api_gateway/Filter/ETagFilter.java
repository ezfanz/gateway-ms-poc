package irfan.microservices.api_gateway.Filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class ETagFilter extends ShallowEtagHeaderFilter {

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private static final List<MediaType> VISIBLE_TYPES = Arrays.asList(MediaType.valueOf("text/*"), MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.valueOf("application/*+json"),
            MediaType.valueOf("application/*+xml"), MediaType.MULTIPART_FORM_DATA);
    private static final String DIRECTIVE_NO_STORE = "no-store";
    private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";
    private final boolean writeWeakETag = false;
    @Value("${etag.enabled}")
    private Boolean etagEnabledFlag;

    private static boolean isContentCachingDisabled(ContentCachingRequestWrapper request) {
        return (request.getAttribute(STREAMING_ATTRIBUTE) != null);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        String eTag = request.getHeader("X-ETAG");

        return ifNoneMatch == null && eTag == null;
    }

    @Override
    protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response, int responseStatusCode, InputStream inputStream) {

        if (!response.isCommitted() && responseStatusCode >= 200 && responseStatusCode < 300) {

            String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
            return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequests = wrapRequest(request);
        ContentCachingResponseWrapper responseToUse = wrapResponse(response);

        if (!isAsyncDispatch(wrappedRequests) && !(response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new ConditionalContentCachingResponseWrapper(response, wrappedRequests);
        }

        filterChain.doFilter(wrappedRequests, responseToUse);

        if (!isAsyncStarted(wrappedRequests) && !isContentCachingDisabled(wrappedRequests)) {
            updateResponse(wrappedRequests, responseToUse);
        }
    }

    private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper wrapper =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        Assert.notNull(wrapper, "ContentCachingResponseWrapper not found");
        HttpServletResponse rawResponse = (HttpServletResponse) wrapper.getResponse();

        if (isEligibleForEtag(request, wrapper, wrapper.getStatus(), wrapper.getContentInputStream())) {
            String eTag = wrapper.getHeader(HttpHeaders.ETAG);
            if (!StringUtils.hasText(eTag)) {
                eTag = generateETagHeaderValue(wrapper.getContentInputStream(), this.writeWeakETag);
                rawResponse.setHeader(HttpHeaders.ETAG, eTag);
            }
            if (new ServletWebRequest(request, rawResponse).checkNotModified(eTag)) {
                return;
            }
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length > 0) {
            logContent(content, response.getContentType(), response.getCharacterEncoding());
        }

        wrapper.copyBodyToResponse();


    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            return wrapper;
        } else {
            return new ContentCachingRequestWrapper(request);
        }
    }

    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            return wrapper;
        } else {
            return new ContentCachingResponseWrapper(response);
        }
    }

    private void logContent(byte[] content, String contentType, String contentEncoding) {
        if (contentType != null) {
            MediaType mediaType = MediaType.valueOf(contentType);
            boolean visible = VISIBLE_TYPES.stream().anyMatch(visibleType -> visibleType.includes(mediaType));
            if (visible) {
                try {
                    String contentString = new String(content, contentEncoding);
                    Stream.of(contentString.split("\r\n|\r|\n")).forEach(line -> log.info("{} {}", "[Response]  : ", line));
                } catch (UnsupportedEncodingException e) {
                    log.info("{} [{} bytes content]", "[Response]  : ", content.length);
                }
            } else {
                log.info("{} [{} bytes content]", "[Response]  : ", content.length);
            }
        }
    }

    private static class ConditionalContentCachingResponseWrapper extends ContentCachingResponseWrapper {

        private final HttpServletRequest request;

        ConditionalContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return (isContentCachingDisabled((ContentCachingRequestWrapper) this.request) || hasETag() ?
                    getResponse().getOutputStream() : super.getOutputStream());
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return (isContentCachingDisabled((ContentCachingRequestWrapper) this.request) || hasETag() ?
                    getResponse().getWriter() : super.getWriter());
        }

        private boolean hasETag() {
            return StringUtils.hasText(getHeader(HttpHeaders.ETAG));
        }
    }
}

