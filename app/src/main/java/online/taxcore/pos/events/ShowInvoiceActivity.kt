package online.taxcore.pos.events

import online.taxcore.pos.enums.InvoiceActivityType

class ShowInvoiceActivity {
    var invoiceActivityType: InvoiceActivityType = InvoiceActivityType.NORMAL
    var invoiceId: String = ""
}
