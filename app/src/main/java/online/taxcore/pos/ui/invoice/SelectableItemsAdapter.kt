package online.taxcore.pos.ui.invoice

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import online.taxcore.pos.R
import online.taxcore.pos.data.local.InvoiceManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.databinding.InvoiceSelectableRecyclerItemBinding
import online.taxcore.pos.extensions.roundLocalized

class SelectableItemsAdapter(private val validTaxes: List<String>, private val onSelectItem: () -> Unit) : RecyclerView.Adapter<SelectableItemViewHolder>() {

    private var catalogItemsList = mutableListOf<Item>()
    private var filteredItemsList = mutableListOf<Item>()

    override fun getItemCount() = filteredItemsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableItemViewHolder {
        val binding = InvoiceSelectableRecyclerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SelectableItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectableItemViewHolder, position: Int) {
        val currentItem = filteredItemsList[position]

        holder.bind(currentItem, validTaxes)

        holder.binding.itemCard.setOnClickListener {
            currentItem.isSelected = currentItem.isSelected.not()

            if (currentItem.isSelected) {
                InvoiceManager.selectedItems.add(currentItem)
            } else {
                // Reset quantity
                currentItem.quantity = 1.0

                InvoiceManager.selectedItems.remove(currentItem)
            }

            notifyItemChanged(holder.bindingAdapterPosition)
            onSelectItem()
        }
    }

    fun setData(arrayList: MutableList<Item>) {
        this.catalogItemsList = arrayList
        this.filteredItemsList = arrayList.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredItemsList = if (query.isEmpty()) {
            catalogItemsList.toMutableList()
        } else {
            val lowerCaseQuery = query.lowercase()
            catalogItemsList.filter { item ->
                item.name.lowercase().contains(lowerCaseQuery) ||
                        item.barcode.lowercase().contains(lowerCaseQuery)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun addNewItem(invoiceItem: Item) {
        invoiceItem.isSelected = true

        this.catalogItemsList.add(0, invoiceItem)
        this.filteredItemsList.add(0, invoiceItem)

        notifyItemInserted(0)
    }

    fun removeSelection(item: Item) {
        val removedItem = this.catalogItemsList.find { it.uuid == item.uuid }

        removedItem?.let {
            it.isSelected = false

            val filteredIndex = filteredItemsList.indexOf(it)
            if (filteredIndex >= 0) {
                notifyItemChanged(filteredIndex)
            }
        }
    }

}

class SelectableItemViewHolder(val binding: InvoiceSelectableRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: Item, validTaxes: List<String>) {

        binding.selectableItemStartImage.visibility = if (item.isFavorite) View.VISIBLE else View.INVISIBLE

        binding.selectableItemTitle.text = item.name
        binding.selectableItemUnitPrice.text = item.price.roundLocalized()

        binding.itemCard.isChecked = item.isSelected

        try {

            val taxesSpans = item.tax.map {
                val spannable = SpannableString(it.code)
                if (validTaxes.contains(it.code).not()) {
                    spannable.setSpan(ForegroundColorSpan(Color.RED), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                spannable
            }

            val labelPrefix = itemView.context.getString(R.string.taxes)

            val spannableString = SpannableStringBuilder()
            spannableString.append("$labelPrefix: ")
            taxesSpans.forEach { it ->
                spannableString.append(it)
                spannableString.append(", ")
            }

            spannableString.delete(spannableString.length - 2, spannableString.length)

            binding.selectableItemTaxLabels.text = spannableString
        } catch (err: Error) {
            binding.selectableItemTaxLabels.text = item.tax.joinToString(",") { it.code }
        }

        val itemEan = if (item.barcode.isNotEmpty()) item.barcode else "n/a"
        binding.selectableItemBarcode.text = "EAN: $itemEan"

    }
}
