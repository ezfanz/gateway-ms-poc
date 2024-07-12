package irfan.microservices.api_gateway.Filter;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.maybank2u.life.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CustomPostFilter implements GlobalFilter, Ordered {

    private static final Log logger = LogFactory.getLog(AuthHeaderFilter.class);

    private final ImmutableList<Integer> codes = ImmutableList.of(500, 502, 503, 504);
    private final String[] blackListUrls;

    public CustomPostFilter(String[] blackListUrls) {
        this.blackListUrls = blackListUrls;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Spring Cloud Gateway: CustomPostFilter executing...");

        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders requestHeaders = request.getHeaders();
        logRequestHeaders(requestHeaders, "Incoming Request");

        String requestPath = request.getURI().getPath();

        Optional<String> blacklist = Arrays.stream(blackListUrls)
                .filter(f -> new AntPathMatcher().match(f, requestPath))
                .findFirst();

        if (blacklist.isPresent()) {
            logger.info("Spring Cloud Gateway: Request is in the blacklist, proceeding without modification.");
            return chain.filter(exchange);
        } else {
            printLog();
        }

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse originalResponse = exchange.getResponse();
            HttpHeaders responseHeaders = originalResponse.getHeaders();
            logRequestHeaders(responseHeaders, "Spring Cloud Gateway: Outgoing Response");

            if (codes.contains(originalResponse.getStatusCode().value())) {
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        return super.writeWith(DataBufferUtils.join(body).flatMap(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            DataBufferUtils.release(dataBuffer);

                            String responseBody = new String(content, StandardCharsets.UTF_8);
                            log.info("Spring Cloud Gateway: POST CTX: " + responseBody);

                            JsonObject error = new JsonObject();
                            JsonObject contentJson = new JsonObject();
                            String message = "Spring Cloud Gateway: We are experiencing communication error. Please try again.";

                            boolean dppErrorCode = false;
                            Map<String, String> map = JsonUtil.getDppErrorCode(responseBody);
                            if (map != null && map.size() > 1) {
                                dppErrorCode = true;
                                contentJson.addProperty("statusCode", map.get("statusCode"));
                                contentJson.addProperty("statusDesc", map.get("statusDesc") != null ? map.get("statusDesc") : message);
                                error.add("error", contentJson);
                            }

                            if (!dppErrorCode) {
                                contentJson.addProperty("code", String.valueOf(getStatusCode().value()));
                                contentJson.addProperty("message", message);
                                error.add("error", contentJson);
                            }

                            byte[] newContent = error.toString().getBytes(StandardCharsets.UTF_8);
                            originalResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            originalResponse.setStatusCode(getStatusCode());

                            DataBuffer buffer = originalResponse.bufferFactory().wrap(newContent);
                            return Mono.just(buffer);
                        }));
                    }
                };
                exchange.mutate().response(decoratedResponse).build();
                logRequestHeaders(decoratedResponse.getHeaders(), "Spring Cloud Gateway: Modified Response");
            }
        }));
    }

    void printLog() {
        log.info("Spring Cloud Gateway: PRE CTX: " + this.toString());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    // Log function, remove after test done
    private void logRequestHeaders(HttpHeaders headers, String message) {
        logger.info(message + " - Headers:");
        headers.forEach((name, values) -> values.forEach(value -> logger.info(name + ": " + value)));
    }
}