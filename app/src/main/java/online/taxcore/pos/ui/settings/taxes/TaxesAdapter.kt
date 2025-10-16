package online.taxcore.pos.ui.settings.taxes

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import online.taxcore.pos.data.realm.TaxesSettings
import online.taxcore.pos.databinding.TaxesListItemBinding
import online.taxcore.pos.extensions.roundLocalized

@SuppressLint("NotifyDataSetChanged")
class TaxesAdapter : RecyclerView.Adapter<TaxesAdapter.TaxesViewHolder>() {

    private var taxesList = ArrayList<TaxesSettings>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxesViewHolder {
        val binding = TaxesListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaxesViewHolder(binding)
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

    inner class TaxesViewHolder(private val binding: TaxesListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TaxesSettings) {
            binding.taxItemNameLabel.text = item.name

            val labelText = if (item.value == "%") {
                "${item.rate.roundLocalized(1)} ${item.value}"
            } else {
                "${item.value} ${item.rate.roundLocalized(1)}"
            }

            binding.taxLabelChip.text = item.code
            binding.taxItemRateLabel.text = labelText
        }
    }
}
