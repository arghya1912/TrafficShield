package trafficshield_gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestClientConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}