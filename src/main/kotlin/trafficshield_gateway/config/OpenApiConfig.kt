package trafficshield_gateway.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "TrafficShield API",
        version = "1.0.0",
        description = """
            TrafficShield is a resilient API Gateway for microservices.
            
            It demonstrates:
            - Configurable load balancing
            - Redis-backed token bucket rate limiting
            - Custom circuit breaker state management
            - PostgreSQL-backed request metrics
            - AI-style traffic summary generation
            - Swagger-based live API demo
        """,
        contact = Contact(
            name = "Arghyadip Sengupta",
            email = "arghyadipsengupta@gmail.com"
        )
    )
)
class OpenApiConfig