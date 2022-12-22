package online.taxcore.pos.data.models

data class TaxItem(
//    val categoryType: Int,
    val label: String,
//    val amount: Double,
    val rate: Double,
//    val categoryName: String,
    val name: String,
    var value: String
)
