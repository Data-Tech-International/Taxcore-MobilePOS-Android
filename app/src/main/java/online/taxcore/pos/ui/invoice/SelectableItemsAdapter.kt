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
import kotlinx.android.synthetic.main.invoice_selectable_recycler_item.view.*
import online.taxcore.pos.R
import online.taxcore.pos.data_managers.InvoiceManager
import online.taxcore.pos.models.Item

class SelectableItemsAdapter(
    private val validTaxes: List<String>,
    private val onSelectItem: () -> Unit
) : RecyclerView.Adapter<SelectableItemViewHolder>() {

    private var catalogItemsList = mutableListOf<Item>()

    override fun getItemCount() = catalogItemsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.invoice_selectable_recycler_item, parent, false)
        return SelectableItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectableItemViewHolder, position: Int) {
        val ctx = holder.itemView.context
        val currentItem = catalogItemsList[position]

        holder.bind(currentItem, validTaxes)

        holder.itemView.itemCard.setOnClickListener { view ->

            (view as MaterialCardView).toggle()

            currentItem.isSelected = currentItem.isSelected.not()

            if (currentItem.isSelected) {
                InvoiceManager.selectedItems.add(currentItem)
            } else {
                // Reset quantity
                currentItem.quantity = 1.0

                InvoiceManager.selectedItems.remove(currentItem)
            }

            notifyDataSetChanged()
            onSelectItem()
        }
    }

    fun setData(arrayList: MutableList<Item>) {
        this.catalogItemsList = arrayList
        notifyDataSetChanged()
    }

    fun addNewItem(invoiceItem: Item) {
        invoiceItem.isSelected = true

        this.catalogItemsList.add(0, invoiceItem)

        notifyItemInserted(0)
    }

    fun removeSelection(item: Item) {
        val removedItem = this.catalogItemsList.find { it.uuid == item.uuid }

        removedItem?.let { it ->
            val itemIndex = catalogItemsList.indexOf(it)

            it.isSelected = false

            notifyItemChanged(itemIndex, it)
            notifyDataSetChanged()
        }
    }

}

class SelectableItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(item: Item, validTaxes: List<String>) {

        itemView.selectableItemStartImage.visibility =
            if (item.isFavorite) View.VISIBLE else View.INVISIBLE

        itemView.selectableItemTitle.text = item.name
        itemView.selectableItemUnitPrice.text = item.price.toString()

        itemView.itemCard.isChecked = item.isSelected

        try {

            val taxesSpans = item.tax.map {
                val spannable = SpannableString(it.code)
                if (validTaxes.contains(it.code).not()) {
                    spannable.setSpan(
                        ForegroundColorSpan(Color.RED),
                        0,
                        1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                spannable
            }

            val spannableString = SpannableStringBuilder()
            spannableString.append("Tax: ")
            taxesSpans.forEach { it ->
                spannableString.append(it)
                spannableString.append(", ")
            }

            spannableString.delete(spannableString.length - 2, spannableString.length)

            itemView.selectableItemTaxLabels.text = spannableString
        } catch (err: Error) {
            itemView.selectableItemTaxLabels.text = item.tax.joinToString(",") { it.code }
        }

        val itemEan = if (item.barcode.isNotEmpty()) item.barcode else "n/a"
        itemView.selectableItemBarcode.text = "EAN: $itemEan"

    }
}
