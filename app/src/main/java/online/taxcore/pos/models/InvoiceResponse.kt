package online.taxcore.pos.models

import com.google.gson.annotations.SerializedName
import io.realm.annotations.PrimaryKey

class InvoiceItem {

    @PrimaryKey
    @SerializedName("ItemId")
    var itemId: String? = null

    @SerializedName("InvoiceId")
    var invoiceId: String? = null

    @SerializedName("GTIN")
    var barcode: String? = null

    @SerializedName("Name")
    var name: String? = null

    @SerializedName("Quantity")
    var quantity: Double? = null

    @SerializedName("UnitPrice")
    var unitPrice: Double? = null

    @SerializedName("TotalAmount")
    var totalAmount: Double? = null

    @SerializedName("Labels")
    var taxLabels: List<String> = arrayListOf()
}

@Suppress("PropertyName")
class InvoiceResponse {
    var RequestedBy: String? = null
    var DT: String? = null
    var IC: String? = null
    var InvoiceCounterExtension: String? = null
    var IN: String? = null
    var VerificationUrl: String = ""
    var VerificationQRCode: String? = null
    var Journal: String? = null
    var Messages: String? = null
    var SignedBy: String? = null
    var ID: String? = null
    var S: String? = null
    var TotalCounter: Int = 0
    var TransactionTypeCounter: Int? = null
    var TotalAmount: Double = 0.0
    var TaxGroupRevision: Int? = null
    var Items: List<InvoiceItem> = arrayListOf(InvoiceItem())
}
