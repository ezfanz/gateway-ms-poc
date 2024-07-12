package irfan.microservices.api_gateway.Filter;

import com.maybank2u.life.aggregator.constant.AggregatorConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
@Slf4j
public class SimpleFilter implements GlobalFilter, Ordered {

    private static final Log logger = LogFactory.getLog(SimpleFilter.class);
    private static final int PRE_DECORATION_FILTER_ORDER = 5;

    @Resource
    private DiscoveryClient discoveryClient;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        logRequestHeaders(exchange.getRequest().getHeaders(), "Before Mutation");

        ServerHttpRequest request = exchange.getRequest();
        String reqUri = request.getPath().toString();
        final String ONBOARD = "onboard";

        if (reqUri.contains(AggregatorConstants.AGGREGATOR_PREFIX_URI) && !reqUri.contains(ONBOARD)) {
            String adaptedUri = reqUri.replaceAll("/P-API", "/mae");

            // Fetching service instance from discovery client
            String serviceName = "mae-adp-service";
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                throw new IllegalStateException("Target service instance not found!");
            }

            URI serviceUri = instances.get(0).getUri();
            URI newUri;
            try {
                newUri = new URI(serviceUri.toString() + adaptedUri);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Failed to create URI", e);
            }

            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .uri(newUri)
                    .build();

            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUri);
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR, request.getURI());

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        logRequestHeaders(exchange.getRequest().getHeaders(), "After Mutation");

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return PRE_DECORATION_FILTER_ORDER + 1;
    }


    private void logRequestHeaders(HttpHeaders headers, String message) {
        logger.info(message + " - Headers:");
        headers.forEach((name, values) -> values.forEach(value -> logger.info(name + ": " + value)));
    }


}
