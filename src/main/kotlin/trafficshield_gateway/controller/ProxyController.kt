package trafficshield_gateway.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.ProxyResponse
import trafficshield_gateway.service.ProxyService

@RestController
@RequestMapping("/proxy")
class ProxyController(
    private val proxyService: ProxyService
) {

    @GetMapping("/{serviceName}/**")
    fun proxyGet(
        @PathVariable serviceName: String,
        @RequestHeader(value = "client-id", defaultValue = "anonymous") clientId: String,
        request: HttpServletRequest
    ): ProxyResponse {
        val fullPath = request.requestURI
        val pathAfterServiceName = fullPath.substringAfter("/proxy/$serviceName/")

        return proxyService.forwardGetRequest(
            clientId = clientId,
            serviceName = serviceName,
            path = pathAfterServiceName
        )
    }
}