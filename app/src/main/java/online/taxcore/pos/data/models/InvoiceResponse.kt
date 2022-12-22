package online.taxcore.pos.data.models

class InvoiceResponse {
    var requestedBy: String? = null
    var sdcDateTime: String? = null
    var invoiceCounter: String? = null
    var taxItems: List<TaxItem> = arrayListOf()
    var invoiceCounterExtension: String? = null
    var invoiceNumber: String? = null
    var verificationUrl: String = ""
    var verificationQRCode: String? = null
    var journal: String? = null
    var messages: String? = null
    var signedBy: String? = null
    var encryptedInternalData: String? = null
    var signature: String? = null
    var totalCounter: Int = 0
    var transactionTypeCounter: Int? = null
    var totalAmount: Double = 0.0
    var taxGroupRevision: Int? = null
    var items: List<InvoiceItem> = arrayListOf()
    var businessName: String = ""
    var tin: String = ""
    var locationName: String = ""
    var address: String = ""
    var district: String = ""
    var mrc: String = ""
}
