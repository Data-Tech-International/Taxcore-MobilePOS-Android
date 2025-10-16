package online.taxcore.pos.ui.catalog

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.chip.Chip
import com.vicpin.krealmextensions.delete
import online.taxcore.pos.R
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.databinding.CatalogCardItemBinding
import online.taxcore.pos.extensions.fromHtml
import java.util.Locale

class CatalogAdapter(
    private var context: Context,
    private val validTaxes: List<String>,
    private val update: () -> Unit
) : RecyclerView.Adapter<CatalogAdapter.PluViewHolder>() {

    private var items: MutableList<Item> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PluViewHolder(
            CatalogCardItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: PluViewHolder, position: Int) {
        val catalogItem = items[position]

        holder.bind(catalogItem, validTaxes)
        holder.binding.itemPluDelete?.setOnClickListener {
            confirmRemoveItem(position, catalogItem, update)
        }

        holder.binding.itemPluFavorite.setOnClickListener {
            catalogItem.isFavorite = !catalogItem.isFavorite

            CatalogManager.toggleFavoriteItem(catalogItem) {
                notifyItemChanged(position, catalogItem)
            }

            val toastMsg = if (catalogItem.isFavorite) {
                context.getString(R.string.toast_item_added_to_fav)
            } else {
                context.getString(R.string.toast_item_removed_from_fav)
            }

            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = items.size

    fun setData(items: MutableList<Item>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun changeDataByFilter(pattern: String) {
        items = CatalogManager.getFilterItemsByNameAndBarcode(pattern)
        notifyDataSetChanged()
    }

    private fun confirmRemoveItem(position: Int, item: Item, update: () -> Unit) {
        MaterialDialog(context).show {
            title(text = context.getString(R.string.title_confirm_remove_item_name, item.name))

            negativeButton(R.string.btn_close)
            positiveButton(R.string.btn_remove_item) {
                remove(position, item, update)
            }
        }
    }

    private fun remove(position: Int, item: Item, update: () -> Unit) {
        items.removeAt(position)

        notifyItemRangeChanged(position, items.size)
        notifyItemRemoved(position)
        notifyDataSetChanged()

        item.delete { equalTo("uuid", item.uuid) }

        Toast.makeText(
            context,
            context.getString(R.string.toast_item_with_name_removed, item.name),
            Toast.LENGTH_SHORT
        ).show()

        items.isEmpty().let { update() }
    }

    inner class PluViewHolder(val binding: CatalogCardItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item, validTaxes: List<String>) {
            binding.itemPluEdit.setOnClickListener {
                ItemDetailActivity.start(it.context, item.uuid)
            }

            // Title
            val itemTitle = "<b>${item.name}</b>".fromHtml()

            // Price
            val itemPrice = String.format(
                "%.2f",
                item.price
            )

            binding.itemPluTitle.text = itemTitle

            val priceText =
                "${context.getString(R.string.price)} <font color='#FF5722'><b>${itemPrice}</b></font>".fromHtml()

            val inflater =
                binding.root.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding.itemTaxLabelsChipGroup.removeAllViews()

            item.tax.forEach { tax ->
                val chipView = inflater.inflate(
                    R.layout.chip_tax_label,
                    binding.itemTaxLabelsChipGroup,
                    false
                ) as Chip
                chipView.text = tax.code

                if (validTaxes.contains(tax.code).not()) {
                    chipView.setChipBackgroundColorResource(R.color.colorRedis)
                }

                binding.itemTaxLabelsChipGroup.addView(chipView)
                binding.itemTaxLabelsChipGroup.chipSpacingHorizontal = 0
            }

            val itemEan = item.barcode.ifEmpty { "n/a" }
            binding.itemPluBarcode.text = "EAN: $itemEan"
            binding.itemPluPrice.text = priceText

            // start
            val startTintColor = if (item.isFavorite) {
                ContextCompat.getColor(binding.root.context, R.color.accent)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.primaryDark)
            }

            val starDrawable = if (item.isFavorite) {
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_star_full)
            } else {
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_star_empty)
            }

            ImageViewCompat.setImageTintList(
                binding.itemPluFavorite,
                ColorStateList.valueOf(startTintColor)
            )
            binding.itemPluFavorite.setImageDrawable(starDrawable)
        }

    }

}
