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
import kotlinx.android.synthetic.main.invoice_favorite_recycler_item.view.*
import online.taxcore.pos.R
import online.taxcore.pos.data.local.InvoiceManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.extensions.roundLocalized

class FavoriteItemsAdapter(private val validTaxes: List<String>, private val onSelectItem: () -> Unit) :
    RecyclerView.Adapter<FavoriteItemViewHolder>() {

    private var favouritesList = mutableListOf<Item>()

    override fun getItemCount() = favouritesList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.invoice_favorite_recycler_item, parent, false)
        return FavoriteItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteItemViewHolder, position: Int) {
        val ctx = holder.itemView.context
        val currentItem = favouritesList[position]

        holder.bind(currentItem, validTaxes)

        holder.itemView.favoriteCard.setOnClickListener { view ->

            (view as MaterialCardView).toggle()

            currentItem.isSelected = currentItem.isSelected.not()

            if (currentItem.isSelected) {
                InvoiceManager.selectedItems.add(currentItem)
            } else {
                // Reset quantity
                currentItem.quantity = 1.0

                // Remove item from list
                InvoiceManager.selectedItems.remove(currentItem)
            }

            notifyDataSetChanged()

            onSelectItem()
        }
    }

    fun addNewItem(invoiceItem: Item) {
        invoiceItem.isSelected = true

        this.favouritesList.add(0, invoiceItem)

        notifyItemInserted(0)
    }

    fun setData(arrayList: MutableList<Item>) {
        this.favouritesList = arrayList
        notifyDataSetChanged()
    }

    fun removeSelection(item: Item) {
        val removedItem = this.favouritesList.find { it.uuid == item.uuid }

        removedItem?.let { it ->
            val itemIndex = favouritesList.indexOf(it)

            it.isSelected = false

            notifyItemChanged(itemIndex, it)
            notifyDataSetChanged()
        }
    }

}

class FavoriteItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(item: Item, validTaxes: List<String>) {

        itemView.favoriteItemTitle.text = item.name
        itemView.favoriteItemUnitPrice.text = item.price.roundLocalized()

        itemView.favoriteCard.isChecked = item.isSelected

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
            taxesSpans.forEach {
                spannableString.append(it)
                spannableString.append(", ")
            }

            spannableString.delete(spannableString.length - 2, spannableString.length)

            itemView.favoriteItemTaxLabels.text = spannableString
        } catch (err: Error) {
            itemView.favoriteItemTaxLabels.text = item.tax.joinToString(",") { it.code }
        }

        val itemEan = item.barcode.ifEmpty { "n/a" }
        itemView.favoriteItemBarcode.text = "EAN: $itemEan"

    }
}
