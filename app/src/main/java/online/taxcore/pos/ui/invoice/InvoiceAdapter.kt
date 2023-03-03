package online.taxcore.pos.ui.invoice

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.dialog_qty_input.view.*
import kotlinx.android.synthetic.main.invoice_card_item.view.*
import online.taxcore.pos.R
import online.taxcore.pos.data.local.InvoiceManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.roundLocalized
import online.taxcore.pos.extensions.roundToDecimal
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.helpers.DecimalDigitsInputFilter

class InvoiceAdapter(
    private val invoiceType: String,
    private val validTaxes: List<String>,
    private val onItemRemoved: (item: Item) -> Unit,
    private val onItemUpdated: (item: Item) -> Unit?
) : RecyclerView.Adapter<InvoiceItemViewHolder>() {

    private var invoiceList = mutableListOf<Item>()

    override fun getItemCount() = invoiceList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.invoice_card_item, parent, false)

        return InvoiceItemViewHolder(view)
    }

    override fun onBindViewHolder(holderItem: InvoiceItemViewHolder, position: Int) {
        val ctx = holderItem.itemView.context
        val currentItem = invoiceList[position]

        holderItem.bind(currentItem, validTaxes)

        holderItem.itemView.invoiceItemRemoveButton.visible = invoiceType != "copy"
        holderItem.itemView.invoiceEditQtyButton.visible = invoiceType != "copy"

        holderItem.itemView.invoiceItemRemoveButton.setOnClickListener {
            openConfirmRemoveItemDialog(ctx, currentItem, position, onItemRemoved)
        }

        holderItem.itemView.invoiceEditQtyButton.setOnClickListener {
            openEditQuantityDialog(ctx, currentItem, position)
        }

        holderItem.itemView.invoiceItemTitleLabel.setOnClickListener {
            openItemName(ctx, currentItem)
        }
    }

    fun setData(invoiceItems: MutableList<Item>) {
        this.invoiceList = invoiceItems
        notifyDataSetChanged()
    }

    fun addNewItem(invoiceItem: Item) {
        this.invoiceList.add(0, invoiceItem)

        notifyDataSetChanged()
    }

    fun getInvoiceItems(): List<Item> = invoiceList

    private fun openItemName(ctx: Context, item: Item) {
        MaterialDialog(ctx).show {
            message(text = item.name)
            negativeButton(R.string.btn_close)
        }
    }

    private fun openEditQuantityDialog(ctx: Context, currentItem: Item, position: Int) {
        val quantityText = currentItem.quantity.roundToDecimal(3)

        MaterialDialog(ctx).show {
            title(text = context.getString(R.string.title_enter_quantity))

            customView(R.layout.dialog_qty_input)

            val qtyLayout = getCustomView().dialogQtyInputLayout
            val qtyInput = getCustomView().dialogQtyInput

            qtyInput.setText(quantityText)
            qtyInput.setSelection(quantityText.length)
            qtyInput.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(11, 3))

            setActionButtonEnabled(WhichButton.POSITIVE, false)

            // Add input listener
            getCustomView().dialogQtyInput.onTextChanged { inputText ->
                val isInputValid = try {
                    inputText.toDouble() >= 0.001
                } catch (e: NumberFormatException) {
                    false
                }

                val enabledInput = inputText.isNotEmpty() and isInputValid

                if (isInputValid) {
                    qtyLayout.isErrorEnabled = false
                } else {
                    qtyLayout.error = context.getString(R.string.error_minimum_quantity)
                }

                setActionButtonEnabled(WhichButton.POSITIVE, enabledInput)
            }

            negativeButton(R.string.btn_close)
            positiveButton(R.string.dialog_button_save) {

                val quantity = getCustomView().dialogQtyInput.text.toString().toDouble()

                // reset quantity
                currentItem.quantity = quantity

                notifyItemChanged(position)

                onItemUpdated(currentItem)
            }
        }

    }

    private fun openConfirmRemoveItemDialog(ctx: Context, item: Item, position: Int, onItemRemoved: (item: Item) -> Unit) {

        MaterialDialog(ctx).show {
            title(text = context.getString(R.string.title_confirm_remove_item))

            negativeButton(R.string.btn_close)
            positiveButton(R.string.btn_remove_item) {

                // reset quantity
                item.quantity = 1.00

                InvoiceManager.selectedItems.remove(item)

                notifyItemRemoved(position)
                notifyItemRangeChanged(position, invoiceList.size)

                onItemRemoved(item)
            }
        }

    }

}

class InvoiceItemViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {

    fun bind(item: Item, validTaxes: List<String>) {

        val inflater = itemView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        itemView.invoiceItemTaxLabelsChipGroup.removeAllViews()

        item.tax.forEach { tax ->
            val chipView = inflater.inflate(R.layout.chip_tax_label, itemView.invoiceItemTaxLabelsChipGroup, false) as Chip
            chipView.text = tax.code
            if (validTaxes.contains(tax.code).not()) {
                chipView.setChipBackgroundColorResource(R.color.colorRedis)
            }

            itemView.invoiceItemTaxLabelsChipGroup.addView(chipView)
            itemView.invoiceItemTaxLabelsChipGroup.chipSpacingHorizontal = 0
        }

        val totalPrice = (item.price * item.quantity).roundLocalized()
        val quantity = item.quantity.roundLocalized(3)
        val itemEan = item.barcode.ifEmpty { "n/a" }

        itemView.invoiceItemTitleLabel.text = item.name
        itemView.invoiceItemBarcodeLabel.text = "EAN: $itemEan"
        itemView.invoiceItemQtyLabel.text = quantity
        itemView.invoiceItemTotalPriceLabel.text = totalPrice
    }

}
