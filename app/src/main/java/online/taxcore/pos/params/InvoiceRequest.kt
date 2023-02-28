package online.taxcore.pos.params

class InvoiceRequest {
    var dateAndTimeOfIssue: String = ""
    var cashier: String? = null
    var bD: String? = null
    var buyerCostCenterId: String? = null
    var iT: String = ""
    var tT: String? = ""
    var paymentType: String = ""
    var invoiceNumber: String? = null
    var referentDocumentNumber: String? = null
    var pAC: String? = null
    var items: List<Item> = listOf(Item())
    var hash: String? = null
}
