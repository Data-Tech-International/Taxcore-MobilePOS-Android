package online.taxcore.pos.data.models

class InvoiceItem {
    var itemId: String? = null
    var invoiceId: String? = null
    var barcode: String? = null
    var name: String? = null
    var quantity: Double? = null
    var unitPrice: Double? = null
    var totalAmount: Double? = null
    var taxLabels: List<String> = arrayListOf()
}

