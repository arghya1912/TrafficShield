package trafficshield_gateway.controller.admin

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import trafficshield_gateway.model.ServiceInstance
import trafficshield_gateway.service.ServiceRegistry

@RestController
@RequestMapping("/admin/services")
class ServiceRegistryController(
    private val serviceRegistry: ServiceRegistry
) {

    @GetMapping
    fun getAllServices(): Map<String, List<ServiceInstance>> {
        return serviceRegistry.getAllServices()
    }

    @GetMapping("/{serviceName}")
    fun getServiceInstances(
        @PathVariable serviceName: String
    ): List<ServiceInstance> {
        return serviceRegistry.getInstances(serviceName)
    }
}