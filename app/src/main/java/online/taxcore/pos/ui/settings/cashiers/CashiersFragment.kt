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
import com.vicpin.krealmextensions.count
import com.vicpin.krealmextensions.queryFirst
import com.vicpin.krealmextensions.save
import online.taxcore.pos.R
import online.taxcore.pos.data.realm.Cashier
import online.taxcore.pos.databinding.CashiersFragmentBinding
import online.taxcore.pos.databinding.DialogAddCashierBinding
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.extensions.visible

class CashiersFragment : Fragment() {

    private var _binding: CashiersFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CashiersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initView()
        setOnClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initView() {

        val activeCashier = Cashier().queryFirst { equalTo("isChecked", true) }
        val hasCashiers = Cashier().count() > 0

        val foregroundColor = if (hasCashiers) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")

        binding.cashiersViewButton.isEnabled = hasCashiers
        binding.cashiersViewButton.foreground = ColorDrawable(foregroundColor)

        activeCashier?.let {
            binding.cashierCurrentLayout.visible = true

            binding.cashierCurrentLabel.text = it.name
            binding.cashierCurrentID.text = "ID: ${it.id}"
        }
    }

    private fun setOnClickListeners() {
        binding.cashiersViewButton.setOnClickListener {
            replaceFragment(R.id.baseFragment, CashiersListFragment(), addToBackStack = true)
        }

        binding.cashiersAddButton.setOnClickListener {
            openAddCashierDialog()
        }
    }

    private fun openAddCashierDialog() {

        MaterialDialog(requireContext()).show {
            title(R.string.dialog_title_add_cashier)
            val dialogBinding = DialogAddCashierBinding.inflate(layoutInflater)
            customView(view = dialogBinding.root)
            setActionButtonEnabled(WhichButton.POSITIVE, false)

            dialogBinding.addCashierNameInput.onTextChanged { cashierName ->
                val cashierId = dialogBinding.addCashierIDInput.text.toString()
                setActionButtonEnabled(WhichButton.POSITIVE, cashierName.isNotEmpty() && cashierId.isNotEmpty())
            }

            // Add input listener
            dialogBinding.addCashierIDInput.onTextChanged { cashierId ->
                val cashierName = dialogBinding.addCashierNameInput.text.toString()
                setActionButtonEnabled(WhichButton.POSITIVE, cashierId.isNotEmpty() && cashierName.isNotEmpty())
            }

            negativeButton(R.string.btn_close)
            positiveButton(R.string.btn_add_cashier) {
                val cashierName = dialogBinding.addCashierNameInput.text.toString()
                val cashierId = dialogBinding.addCashierIDInput.text.toString()

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
