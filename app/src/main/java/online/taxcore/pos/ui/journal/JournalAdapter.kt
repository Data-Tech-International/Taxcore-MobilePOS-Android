package online.taxcore.pos.ui.journal

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.databinding.JournalCardItemBinding
import online.taxcore.pos.enums.InvoiceActivityType
import online.taxcore.pos.extensions.roundToDecimalPlaces
import online.taxcore.pos.helpers.EventBusHelper
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("NotifyDataSetChanged")
class JournalAdapter : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private var journalList: List<Journal> = emptyList()

    override fun getItemCount() = journalList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = JournalCardItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val item = journalList[position]
        holder.bind(item)
    }

    fun setData(journalList: List<Journal>) {
        this.journalList = journalList
        notifyDataSetChanged()
    }

    class JournalViewHolder(val binding: JournalCardItemBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var item: Journal

        init {
            binding.journalItemView.setOnClickListener {
                EventBusHelper.showFiscalDialog(item.id, item.qrCode, item.invoiceNumber, item.VerificationUrl)
            }

            binding.journalItemCopyButton.setOnClickListener {
                EventBusHelper.showInvoiceActivity(InvoiceActivityType.COPY, item.invoiceNumber)
            }

            binding.journalItemRefundButton.setOnClickListener {
                EventBusHelper.showInvoiceActivity(InvoiceActivityType.REFUND, item.invoiceNumber)
            }

            binding.journalItemCard.setOnLongClickListener {
                EventBusHelper.copyInvoiceNumber(item.invoiceNumber)
                true
            }
        }

        fun bind(item: Journal) {
            this.item = item
            binding.itemJournalDate.text = showDate(item.date)
            binding.itemJournalRec.text = item.invoiceNumber
            binding.itemJournalTotal.text = (item.total.toString().roundToDecimalPlaces(2)).toString()
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
