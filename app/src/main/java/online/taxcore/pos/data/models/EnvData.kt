package online.taxcore.pos.data.models

data class EnvData(
    val uid: String,
    val name: String,
    val esdcEnvName: String,
    val esdcEndpoint: String,
    val esdcApiEndpoint: String,
    val apiEndpoint: String,
    val country: String,
    val logo: String
)
