package online.taxcore.pos.ui.settings.cashiers

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.vicpin.krealmextensions.count
import com.vicpin.krealmextensions.queryFirst
import com.vicpin.krealmextensions.save
import kotlinx.android.synthetic.main.cashiers_fragment.*
import kotlinx.android.synthetic.main.dialog_add_cashier.view.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.models.Cashier

class CashiersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.cashiers_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initView()
        setOnClickListeners()
    }

    private fun initView() {

        val activeCashier = Cashier().queryFirst { equalTo("isChecked", true) }
        val hasCashiers = Cashier().count() > 0

        val foregroundColor = if (hasCashiers) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")

        cashiersViewButton.isEnabled = hasCashiers
        cashiersViewButton.foreground = ColorDrawable(foregroundColor)

        activeCashier?.let {
            cashierCurrentLayout.visible = true

            cashierCurrentLabel.text = it.name
            cashierCurrentID.text = "ID: ${it.id}"
        }
    }

    private fun setOnClickListeners() {
        cashiersViewButton.setOnClickListener {
            replaceFragment(R.id.baseFragment, CashiersListFragment(), addToBackStack = true)
        }

        cashiersAddButton.setOnClickListener {
            openAddCashierDialog()
        }
    }

    private fun openAddCashierDialog() {

        MaterialDialog(requireContext()).show {
            title(R.string.dialog_title_add_cashier)
            customView(R.layout.dialog_add_cashier)
            setActionButtonEnabled(WhichButton.POSITIVE, false)

            getCustomView().addCashierNameInput.onTextChanged { cashierName ->
                val cashierId = getCustomView().addCashierIDInput.text.toString()
                setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    cashierName.isNotEmpty() && cashierId.isNotEmpty()
                )
            }

            // Add input listener
            getCustomView().addCashierIDInput.onTextChanged { cashierId ->
                val cashierName = getCustomView().addCashierNameInput.text.toString()
                setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    cashierId.isNotEmpty() && cashierName.isNotEmpty()
                )
            }

            negativeButton(R.string.btn_close)
            positiveButton(R.string.btn_add_cashier) {
                val cashierName = it.getCustomView().addCashierNameInput.text.toString()
                val cashierId = it.getCustomView().addCashierIDInput.text.toString()

                val cashier = Cashier()
                with(cashier) {
                    name = cashierName
                    id = cashierId
                    isChecked = this.count() == 0L
                    save()
                }

                initView()
            }
        }
    }

}
