package trafficshield_gateway.controller.mock

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.MockServiceResponse

@RestController
@RequestMapping("/mock")
class MockUserServiceController {

    @GetMapping("/user-service-1/api/users/{userId}")
    fun getUserFromInstanceOne(@PathVariable userId: String): MockServiceResponse {
        return MockServiceResponse(
            serviceName = "user-service",
            instanceId = "user-service-1",
            status = "SUCCESS",
            message = "Fetched user $userId from user-service-1"
        )
    }

    @GetMapping("/user-service-2/api/users/{userId}")
    fun getUserFromInstanceTwo(@PathVariable userId: String): MockServiceResponse {
        return MockServiceResponse(
            serviceName = "user-service",
            instanceId = "user-service-2",
            status = "SUCCESS",
            message = "Fetched user $userId from user-service-2"
        )
    }
}