package online.taxcore.pos.ui.common

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import online.taxcore.pos.data.realm.Taxes
import online.taxcore.pos.data.realm.TaxesSettings
import online.taxcore.pos.databinding.ItemCheckedTaxesBinding
import online.taxcore.pos.extensions.roundLocalized

@SuppressLint("NotifyDataSetChanged")
class TaxesCheckedAdapter(private val checkedChangeListener: () -> Unit) :
    RecyclerView.Adapter<TaxesCheckedAdapter.TaxesCheckedViewHolder>() {

    private var taxesList: MutableList<TaxesSettings> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaxesCheckedViewHolder {
        val binding = ItemCheckedTaxesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TaxesCheckedViewHolder(binding)
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

    class TaxesCheckedViewHolder(private val binding: ItemCheckedTaxesBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaxesSettings, checkedChangeListener: () -> Unit) {

            val taxRate = if (item.value == "%") {
                "${item.rate.roundLocalized(1)} ${item.value}"
            } else {
                "${item.value} ${item.rate.roundLocalized(1)}"
            }

            binding.itemTaxesCheckedTitleLabel.text = "${item.name} (${taxRate})"
            binding.itemTaxesCheckedTitle.text = item.code
            binding.itemTaxesChecked.isChecked = item.isChecked

            binding.itemTaxesChecked.setOnCheckedChangeListener { _, _ ->
                item.isChecked = !item.isChecked
                checkedChangeListener()
            }
        }
    }
}
