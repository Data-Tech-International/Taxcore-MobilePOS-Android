package online.taxcore.pos.params

class Item {
    var name: String = ""
    var GTIN: String? = null
    var quantity: Float = 0.0F
    var totalAmount: Float = 0.0F
    var unitPrice: Float = 0.0F
    var labels: List<String> = arrayListOf("")
}
