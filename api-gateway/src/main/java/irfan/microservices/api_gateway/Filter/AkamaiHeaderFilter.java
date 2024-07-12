package irfan.microservices.api_gateway.Filter;


import irfan.microservices.api_gateway.Config.AkamaiUrlConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

//Used to add a header to ask Akamai to cache the response
@Slf4j
public class AkamaiHeaderFilter extends OncePerRequestFilter {
    @Autowired
    AkamaiUrlConfig akamaiUrlConfig;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    String headerValue = "";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        Map<String, String> config = akamaiUrlConfig.getConfig();
        if (config != null && !config.isEmpty()) {
            boolean toCache = config.containsKey(path);

            if (toCache) {
                headerValue = config.get(path);
                return false;
            }
        }

        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        //TODO - Enhancement
        // Since error code isn't reliable because some errors are returned with
        // resp code 200, check for specific error keywords
//        ContentCachingResponseWrapper wrappedResponse = (ContentCachingResponseWrapper) response;
//        byte[] content = wrappedResponse.getContentAsByteArray();
//        boolean isError = false;

//        if (content.length > 0)
//        {
//            String contentString = new String(content, wrappedResponse.getCharacterEncoding());
//            isError = contentString.contains("");
//            log.debug("");
//        }

        if (response.getStatus() == 200) {
            response.setHeader("Cache-Control", headerValue);
        }
    }
}
