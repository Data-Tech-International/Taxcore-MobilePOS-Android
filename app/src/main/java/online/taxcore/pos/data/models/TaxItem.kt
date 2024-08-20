package online.taxcore.pos.data.models

data class TaxItem(
    val label: String,
    val rate: Double,
    val name: String,
    var value: String
)
