package online.taxcore.pos.data.models

class Item {
    var name: String = ""
    var gtin: String? = null
    var quantity: Float = 0.0F
    var discount: Float = 0.0F
    var labels: List<String> = arrayListOf()
    var unitPrice: Float = 0.0F
    var totalAmount: Float = 0.0F
}
