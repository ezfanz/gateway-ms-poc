package irfan.microservices.api_gateway.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "akamai")
public class AkamaiUrlConfig {
    @Value("${akamai.config:#{null}}")
    private Map<String, String> config;
}
