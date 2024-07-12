package irfan.microservices.api_gateway.Filter;

import brave.Span;
import brave.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Component
public class TraceFilter extends GenericFilterBean {

    private final Tracer tracer;

    TraceFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            chain.doFilter(request, response);
            return;
        }

        ((HttpServletResponse) response).addHeader("TRACE-ID", currentSpan.context().traceIdString());
        chain.doFilter(request, response);
    }
}
