package online.taxcore.pos.data.local

import android.annotation.SuppressLint
import com.google.gson.GsonBuilder
import com.vicpin.krealmextensions.count
import com.vicpin.krealmextensions.querySorted
import com.vicpin.krealmextensions.save
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import online.taxcore.pos.data.models.InvoiceResponse
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.data.realm.Journal
import java.text.SimpleDateFormat
import java.util.Date

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

    /**
     * Returns a set of existing invoice numbers without loading full Journal objects.
     * This is memory-efficient as it only extracts the invoiceNumber field.
     */
    fun getExistingInvoiceNumbers(): Set<String> {
        val realm = Realm.getDefaultInstance()
        return try {
            val results = realm.where<Journal>().findAll()
            // Extract only invoiceNumber field - Realm will NOT load other fields
            results.mapTo(HashSet(results.size)) { it.invoiceNumber }
        } finally {
            realm.close()
        }
    }

    /**
     * Returns managed RealmResults for lazy loading (memory efficient).
     * Use this for displaying in RecyclerView.
     * The caller must manage the Realm instance lifecycle.
     */
    fun queryJournalItems(realm: Realm, sort: Sort = Sort.DESCENDING): RealmResults<Journal> {
        return realm.where<Journal>()
            .sort("date", sort)
            .findAll()
    }

    /**
     * Returns managed RealmResults with filters applied (memory efficient).
     * Use this for displaying filtered results in RecyclerView.
     * The caller must manage the Realm instance lifecycle.
     *
     * Note: Date filtering is done in memory since Realm doesn't support
     * greaterThan/lessThan on String fields.
     */
    fun queryFilteredItems(realm: Realm, sort: Sort = Sort.DESCENDING): List<Journal> {
        var query = realm.where<Journal>()

        if (invoice.isNotEmpty()) {
            query = query.contains("invoiceNumber", invoice, Case.INSENSITIVE)
        }
        if (buyerTin.isNotEmpty()) {
            query = query.contains("buyerTin", buyerTin, Case.INSENSITIVE)
        }
        if (invoiceType.isNotEmpty()) {
            query = query.contains("invoiceType", invoiceType, Case.INSENSITIVE)
        }
        if (transactionType.isNotEmpty()) {
            query = query.contains("transactionType", transactionType, Case.INSENSITIVE)
        }

        val results = query.sort("date", sort).findAll()

        // Date range filtering in memory (Realm doesn't support greaterThan/lessThan on String fields)
        return if (dateFrom != null || dateTo != null) {
            results.filter { journal ->
                val currentDate = parseDateAndTime(journal.date)
                val afterFrom = dateFrom?.let { currentDate.after(it) } ?: true
                val beforeTo = dateTo?.let { currentDate.before(it) } ?: true
                afterFrom && beforeTo
            }
        } else {
            results
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseDateAndTime(date: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    /**
     * Returns detached copies of all journal items.
     * Use this only for export operations where you need all data in memory.
     * WARNING: Can cause OOM with large datasets.
     */
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
