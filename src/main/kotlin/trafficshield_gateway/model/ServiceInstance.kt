package trafficshield_gateway.model

data class ServiceInstance(
    var instanceId: String = "",
    var baseUrl: String = "",
    var weight: Int = 1
)