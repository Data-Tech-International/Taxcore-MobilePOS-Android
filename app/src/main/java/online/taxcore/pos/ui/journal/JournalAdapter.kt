package online.taxcore.pos.ui.journal

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.journal_card_item.view.*
import online.taxcore.pos.R
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.enums.InvoiceActivityType
import online.taxcore.pos.extensions.roundToDecimalPlaces
import online.taxcore.pos.helpers.EventBusHelper
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class JournalAdapter : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private var journalList: MutableList<Journal> = mutableListOf()

    override fun getItemCount() = journalList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.journal_card_item, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val item = journalList[position]
        holder.bind(item)
    }

    fun setData(journalList: MutableList<Journal>) {
        this.journalList = journalList
        notifyDataSetChanged()
    }

    class JournalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var item: Journal

        init {
            itemView.journalItemView.setOnClickListener {
                EventBusHelper.showFiscalDialog(item.id, item.qrCode, item.invoiceNumber, item.VerificationUrl)
            }

            itemView.journalItemCopyButton.setOnClickListener {
                EventBusHelper.showInvoiceActivity(InvoiceActivityType.COPY, item.invoiceNumber)
            }

            itemView.journalItemRefundButton.setOnClickListener {
                EventBusHelper.showInvoiceActivity(InvoiceActivityType.REFUND, item.invoiceNumber)
            }

            itemView.journalItemCard.setOnLongClickListener {
                EventBusHelper.copyInvoiceNumber(item.invoiceNumber)
                true
            }
        }

        fun bind(item: Journal) {
            this.item = item
            itemView.item_journal_date.text = showDate(item.date)
            itemView.item_journal_rec.text = item.invoiceNumber
            itemView.item_journal_total.text = (item.total.toString().roundToDecimalPlaces(2)).toString()
        }

        @SuppressLint("SimpleDateFormat")
        private fun showDate(data: String): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val writeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            var date: Date? = null
            try {
                date = dateFormat.parse(data)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return writeFormat.format(date)
        }
    }
}
