package online.taxcore.pos.ui.common

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_checked_taxes.view.*
import online.taxcore.pos.R
import online.taxcore.pos.data.realm.Taxes
import online.taxcore.pos.data.realm.TaxesSettings
import online.taxcore.pos.extensions.roundLocalized

@SuppressLint("NotifyDataSetChanged")
class TaxesCheckedAdapter(private val checkedChangeListener: () -> Unit) :
    RecyclerView.Adapter<TaxesCheckedAdapter.TaxesCheckedViewHolder>() {

    private var taxesList: MutableList<TaxesSettings> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxesCheckedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checked_taxes, parent, false)

        return TaxesCheckedViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaxesCheckedViewHolder, position: Int) {
        val taxItem = taxesList[position]
        holder.bind(taxItem, checkedChangeListener)
    }

    override fun getItemCount() = taxesList.size

    fun setData(taxesList: MutableList<TaxesSettings>) {
        this.taxesList = taxesList
        notifyDataSetChanged()
    }

    fun getAppliedTaxes(): List<Taxes> = taxesList
        .filter { it.isChecked }
        .map {
            val appliedTaxLabel = Taxes()
            appliedTaxLabel.code = it.code
            appliedTaxLabel.name = it.name
            appliedTaxLabel.rate = it.rate
            appliedTaxLabel.value = it.value
            appliedTaxLabel
        }

    class TaxesCheckedViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(item: TaxesSettings, checkedChangeListener: () -> Unit) {

            val taxRate = if (item.value == "%") {
                "${item.rate.roundLocalized(1)} ${item.value}"
            } else {
                "${item.value} ${item.rate.roundLocalized(1)}"
            }

            itemView.item_taxes_checked_title_label.text = "${item.name} (${taxRate})"
            itemView.item_taxes_checked_title.text = item.code
            itemView.item_taxes_checked.isChecked = item.isChecked

            itemView.item_taxes_checked.setOnCheckedChangeListener { _, _ ->
                item.isChecked = !item.isChecked
                checkedChangeListener()
            }
        }
    }
}
