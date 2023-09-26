package online.taxcore.pos.data.params

import online.taxcore.pos.data.models.Item

class InvoiceRequest {
    var dateAndTimeOfIssue: String? = null
    var cashier: String? = null
    var buyerId: String? = null
    var buyerCostCenterId: String? = null
    var invoiceType: String = ""
    var transactionType: String? = ""
    var paymentType: String = ""
    var payment: List<PaymentItem> = arrayListOf()
    var invoiceNumber: String? = null
    var referentDocumentNumber: String? = null
    var referentDocumentDT: String? = null
    var items: List<Item> = listOf()
    var hash: String? = null
}

class PaymentItem(
    var amount: Double = 0.0,
    var paymentType: String = ""
)
