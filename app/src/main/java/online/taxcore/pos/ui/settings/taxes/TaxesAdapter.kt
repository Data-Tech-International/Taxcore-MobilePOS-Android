package online.taxcore.pos.ui.settings.taxes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.taxes_list_item.view.*
import online.taxcore.pos.R
import online.taxcore.pos.models.TaxesSettings

class TaxesAdapter : RecyclerView.Adapter<TaxesAdapter.TaxesViewHolder>() {

    private var taxesList = ArrayList<TaxesSettings>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.taxes_list_item, parent, false)
        return TaxesViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaxesViewHolder, position: Int) {
        val item = taxesList[position]
        holder.bind(item)
    }

    override fun getItemCount() = taxesList.size

    fun setData(taxesList: ArrayList<TaxesSettings>) {
        this.taxesList = taxesList
        notifyDataSetChanged()
    }

    inner class TaxesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: TaxesSettings) {
            itemView.taxItemNameLabel.text = item.name

            val labelText = if (item.value == "%") {
                "${item.rate} ${item.value}"
            } else {
                "${item.value} ${item.rate}"
            }

            itemView.taxLabelChip.text = item.code
            itemView.taxItemRateLabel.text = labelText
        }
    }
}
