package trafficshield_gateway.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "TrafficShield API",
        version = "1.0.0",
        description = "Resilient API Gateway with load balancing, rate limiting, circuit breaking, and traffic summaries"
    )
)
class OpenApiConfig