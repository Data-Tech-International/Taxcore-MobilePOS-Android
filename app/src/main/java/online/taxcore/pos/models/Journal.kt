package online.taxcore.pos.models

import io.realm.RealmObject

open class Journal : RealmObject() {

    var id: String = ""
    var date: String = ""
    var rec: Int = 0
    var total: Double = 0.0
    var qrCode: String = ""
    var message: String = ""
    var invoiceNumber: String = ""

    var RequestedBy: String? = null
    var IC: String? = null
    var InvoiceCounterExtension: String? = null
    var VerificationUrl: String = ""
    var SignedBy: String? = null
    var ID: String? = null
    var S: String? = null
    var TotalCounter: Int = 0
    var TransactionTypeCounter: Int? = null
    var TaxGroupRevision: Int? = null

    //for search
    var buyerTin = ""
    var transactionType = ""
    var paymentType = ""
    var invoiceType = ""
    var buyerCostCenter = ""

    var invoiceItemsData: String = "[]"

    var type = "Journal"

    override fun toString(): String =
        "Journal [id=$id, date=$date, rec=$rec, total=$total, qrCode=$qrCode,message=$message,invoiceNumber=$invoiceNumber"

}


