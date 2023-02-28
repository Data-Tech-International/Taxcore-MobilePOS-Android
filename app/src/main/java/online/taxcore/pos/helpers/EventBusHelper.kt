package online.taxcore.pos.helpers

import online.taxcore.pos.enums.InvoiceActivityType
import online.taxcore.pos.events.ShowFiscalInvoicedialog
import online.taxcore.pos.events.ShowInvoiceActivity
import org.greenrobot.eventbus.EventBus

class EventBusHelper {
    companion object {

        fun showFiscalDialog(id: String, qrCode: String, message: String, verificationUrl: String) {
            val event = ShowFiscalInvoicedialog()
            event.id = id
            event.qrCode = qrCode
            event.message = message
            event.url = verificationUrl
            EventBus.getDefault().post(event)
        }

        fun showInvoiceActivity(invoiceActivityType: InvoiceActivityType, refId: String) {

            val event = ShowInvoiceActivity()
            event.invoiceActivityType = invoiceActivityType
            event.invoiceId = refId

            EventBus.getDefault().post(event)
        }

        fun copyInvoiceNumber(invoiceNumber: String) {
            EventBus.getDefault().post(invoiceNumber)
        }

    }
}
