package online.taxcore.pos.data.models

class Item {
    var name: String = ""
    var gtin: String? = null
    var quantity: Double = 0.000
    var discount: Double = 0.00
    var labels: List<String> = arrayListOf()
    var unitPrice: Double = 0.00
    var totalAmount: Double = 0.00
}
