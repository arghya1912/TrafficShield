package trafficshield_gateway.controller.mock

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.MockServiceResponse

@RestController
@RequestMapping("/mock")
class MockPaymentServiceController {

    @GetMapping("/payment-service-1/api/payments/{paymentId}")
    fun getPaymentFromInstanceOne(@PathVariable paymentId: String): MockServiceResponse {
        return MockServiceResponse(
            serviceName = "payment-service",
            instanceId = "payment-service-1",
            status = "SUCCESS",
            message = "Fetched payment $paymentId from payment-service-1"
        )
    }

    @GetMapping("/payment-service-2/api/payments/{paymentId}")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun getPaymentFromInstanceTwo(@PathVariable paymentId: String): MockServiceResponse {
        return MockServiceResponse(
            serviceName = "payment-service",
            instanceId = "payment-service-2",
            status = "FAILED",
            message = "Simulated failure from payment-service-2"
        )
    }
}