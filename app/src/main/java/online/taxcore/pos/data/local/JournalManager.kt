package online.taxcore.pos.data.local

import android.annotation.SuppressLint
import com.google.gson.GsonBuilder
import com.vicpin.krealmextensions.count
import com.vicpin.krealmextensions.querySorted
import com.vicpin.krealmextensions.save
import io.realm.Case
import io.realm.Sort
import online.taxcore.pos.data.models.InvoiceResponse
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.data.realm.Journal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object JournalManager {

    var invoice = ""
    var buyerTin = ""
    var transactionType = ""
    var transactionTypePosition = 0
    var paymentType = ""
    var invoiceTypePosition = 0
    var invoiceType = ""
    var dateFrom: Date? = null
    var dateTo: Date? = null

    fun hasJournalItems(): Boolean {
        return Journal().count() > 0L
    }

    fun loadJournalItems(sort: Sort = Sort.DESCENDING): MutableList<Journal> {
        return Journal().querySorted("date", sort).toMutableList()
    }

    fun saveItem(
            body: InvoiceResponse,
            buyerId: String,
            paymentType: String,
            transactionType: String,
            invoiceType: String,
            buyerCostCenter: String,
            items: MutableList<Item>
    ) {

        val gson = GsonBuilder().create()
        val invoiceItemsJson = gson.toJson(items)

        // Add a journal
        val journal = Journal()
        with(journal) {
            date = body.sdcDateTime.toString()
            rec = 1
            total = body.totalAmount
            id = body.journal.toString()
            qrCode = body.verificationQRCode.toString()
            message = body.messages.toString()
            invoiceNumber = body.invoiceNumber.toString()
            RequestedBy = body.requestedBy
            IC = body.invoiceCounter
            InvoiceCounterExtension = body.invoiceCounterExtension
            VerificationUrl = body.verificationUrl
            SignedBy = body.signedBy
            ID = body.encryptedInternalData
            S = body.signature
            TotalCounter = body.totalCounter
            TransactionTypeCounter = body.transactionTypeCounter
            TaxGroupRevision = body.taxGroupRevision

            buyerTin = buyerId

            this.paymentType = paymentType
            this.transactionType = transactionType
            this.invoiceType = invoiceType
            this.buyerCostCenter = buyerCostCenter

            this.invoiceItemsData = invoiceItemsJson

            save()
        }
    }

    fun loadFilteredItems(sort: Sort = Sort.DESCENDING): MutableList<Journal> {
        val journalsList = Journal().querySorted("date", sort) {

            if (invoice.isNotEmpty()) {
                contains("invoiceNumber", invoice, Case.INSENSITIVE)
            }
            if (buyerTin.isNotEmpty()) {
                contains("buyerTin", buyerTin, Case.INSENSITIVE)
            }
            if (invoiceType.isNotEmpty()) {
                contains("invoiceType", invoiceType, Case.INSENSITIVE)
            }
            if (transactionType.isNotEmpty()) {
                contains("transactionType", transactionType, Case.INSENSITIVE)
            }
        }

        return if (dateFrom != null || dateTo != null) {
            getDateRange(journalsList).toMutableList()
        } else {
            journalsList.toMutableList()
        }
    }

    private fun getDateRange(filterList: List<Journal>): MutableList<Journal> {
        val list: ArrayList<Journal> = arrayListOf()
        for (item in filterList) {
            val currentDate = parseDateAndTime(item.date)

            if (dateFrom != null && dateTo != null) {
                if (dateFrom?.before(currentDate)!! && dateTo?.after(currentDate)!!) {
                    list.add(item)
                }
            } else if (dateFrom != null && dateTo == null) {
                if (dateFrom?.before(currentDate)!!) {
                    list.add(item)
                }
            } else if (dateFrom == null && dateTo != null) {
                if (dateTo?.after(currentDate)!!) {
                    list.add(item)
                }
            }
        }
        return list
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseDateAndTime(date: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(date)
        } catch (e: ParseException) {
            Date()
        }
    }

    fun resetFilter() {
        buyerTin = ""
        invoice = ""
        transactionType = ""
        transactionTypePosition = 0
        paymentType = ""
        invoiceTypePosition = 0
        invoiceType = ""
        dateFrom = null
        dateTo = null
    }

}
