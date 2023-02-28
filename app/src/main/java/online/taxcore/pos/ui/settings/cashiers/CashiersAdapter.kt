package online.taxcore.pos.ui.settings.cashiers

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.google.android.material.card.MaterialCardView
import com.vicpin.krealmextensions.createOrUpdate
import com.vicpin.krealmextensions.delete
import com.vicpin.krealmextensions.query
import com.vicpin.krealmextensions.queryFirst
import io.realm.Case
import kotlinx.android.synthetic.main.cashiers_recycler_item.view.*
import kotlinx.android.synthetic.main.dialog_add_cashier.view.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.models.Cashier

class CashiersAdapter : RecyclerView.Adapter<CashierViewHolder>() {

    private var cashiersList = mutableListOf<Cashier>()

    override fun getItemCount() = cashiersList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashierViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cashiers_recycler_item, parent, false)
        return CashierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashierViewHolder, position: Int) {
        val ctx = holder.itemView.context
        val currentCashier = cashiersList[position]

        holder.bind(currentCashier)
        holder.itemView.cashierCard.setOnClickListener { view ->

            var prevSelected = cashiersList.find { it.isChecked }
            if (prevSelected?.uuid === currentCashier.uuid) {
                return@setOnClickListener
            }

            (view as MaterialCardView).toggle()

            if (prevSelected == null) {
                prevSelected = Cashier().queryFirst { equalTo("isChecked", true) }
            }

            prevSelected?.isChecked = prevSelected?.isChecked?.not() ?: false
            prevSelected?.createOrUpdate()

            currentCashier.isChecked = currentCashier.isChecked.not()
            currentCashier.createOrUpdate()
            notifyDataSetChanged()
        }

        holder.itemView.cashierDeleteButton.setOnClickListener {

            MaterialDialog(ctx).show {
                title(R.string.dialog_title_delete_cashier)
                message(R.string.dialog_message_confirm_cashier_delete)
                setActionButtonEnabled(WhichButton.POSITIVE, currentCashier.isChecked.not())

                positiveButton(R.string.btn_delete_cashier) {
                    currentCashier.delete {
                        equalTo("id", currentCashier.id)
                    }

                    cashiersList.removeAt(position)

                    notifyItemRangeChanged(position, cashiersList.size)
                    notifyItemRemoved(position)

                    Toast.makeText(context, "Cashier deleted", Toast.LENGTH_SHORT).show()
                }

                negativeButton(R.string.btn_close)
            }

        }

        holder.itemView.cashierEditButton.setOnClickListener {

            MaterialDialog(ctx).show {
                title(R.string.dialog_title_edit_cashier)
                customView(R.layout.dialog_add_cashier)
                setActionButtonEnabled(WhichButton.POSITIVE, false)

                getCustomView().addCashierNameInput.setText(currentCashier.name)
                getCustomView().addCashierIDInput.setText(currentCashier.id)

                // Add input listener
                getCustomView().addCashierNameInput.onTextChanged { inputText ->
                    val inputChanged = inputText != currentCashier.name
                    setActionButtonEnabled(
                        WhichButton.POSITIVE,
                        inputText.isNotEmpty() and inputChanged
                    )
                }

                getCustomView().addCashierIDInput.onTextChanged { inputText ->
                    val inputChanged = inputText != currentCashier.id
                    setActionButtonEnabled(
                        WhichButton.POSITIVE,
                        inputText.isNotEmpty() and inputChanged
                    )
                }

                negativeButton(R.string.btn_close)
                positiveButton(R.string.dialog_button_save) {
                    val cashierName = it.getCustomView().addCashierNameInput.text.toString()
                    val cashierId = it.getCustomView().addCashierIDInput.text.toString()

                    with(currentCashier) {
                        name = cashierName
                        id = cashierId
                        createOrUpdate()
                    }

                    notifyItemChanged(position, currentCashier)

                    Toast.makeText(context, "Cashier updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setData(arrayList: MutableList<Cashier>) {
        this.cashiersList = arrayList
        notifyDataSetChanged()
    }

    fun changeDataByFilter(pattern: String) {
        cashiersList = Cashier().query {
            contains("name", pattern, Case.INSENSITIVE).or()
            contains("id", pattern, Case.INSENSITIVE)
        }.toMutableList()
        notifyDataSetChanged()
    }
}

class CashierViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(cashier: Cashier) {
        itemView.cashierNameLabel.text = cashier.name
        itemView.cashierIDLabel.text = "ID: ${cashier.id}"

        itemView.cashierCard.isChecked = cashier.isChecked
        itemView.cashierDeleteButton.isEnabled = cashier.isChecked.not()

        val tintColor = if (cashier.isChecked) R.color.disabled else R.color.colorRed
        val buttonTint = ContextCompat.getColor(itemView.context, tintColor)

        ImageViewCompat.setImageTintList(
            itemView.cashierDeleteButton,
            ColorStateList.valueOf(buttonTint)
        )

    }
}
