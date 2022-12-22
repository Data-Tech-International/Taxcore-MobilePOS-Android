package online.taxcore.pos.data.models

data class EnvResponse(
    val organizationName: String,
    val serverTimeZone: String,
    val street: String,
    val city: String,
    val country: String,
    val endpoints: Endpoints,
    val environmentName: String,
    val logo: String,
    val ntpServer: String,
    val supportedLanguages: List<String>
)

data class Endpoints(
    val taxpayerAdminPortal: String,
    val taxCoreApi: String,
    val vsdc: String,
    val root: String
)
