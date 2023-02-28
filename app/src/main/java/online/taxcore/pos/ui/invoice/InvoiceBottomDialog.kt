package online.taxcore.pos.ui.invoice

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottom_fragment_invoice_type.*
import kotlinx.android.synthetic.main.bottom_fragment_payment.*
import kotlinx.android.synthetic.main.bottom_fragment_transaction_type.*
import online.taxcore.pos.R
import online.taxcore.pos.enums.InvoiceOption
import online.taxcore.pos.enums.InvoiceType
import online.taxcore.pos.enums.PaymentType
import online.taxcore.pos.enums.TransactionType

interface OnInvoiceOptionResult {
    fun setTitle(result: String, option: InvoiceOption)

    fun onInvoiceTypeChanged(invoiceType: InvoiceType, selectedValue: String)
    fun onTransactionTypeChanged(transactionType: TransactionType, selectedValue: String)
    fun onPaymentChanged(paymentType: PaymentType, selectedValue: String)
}

class InvoiceBottomDialog : BottomSheetDialogFragment() {

    private var typeValue: Int? = null

    companion object {
        var result: OnInvoiceOptionResult? = null

        fun showBottomDialog(
            supportFragmentManager: FragmentManager?,
            i: Int,
            invoiceFragment: InvoiceFragment
        ) {
            val TAG = InvoiceBottomDialog::class.java.simpleName

            val dialog = InvoiceBottomDialog().apply {
                arguments = Bundle().apply {
                    putInt("Value", i)
                }
            }
            result = invoiceFragment

            val ft = supportFragmentManager?.beginTransaction()
            dialog.show(ft!!, TAG)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view: View? = null
        typeValue = arguments?.getInt("Value")
        when (typeValue) {
            InvoiceOption.INVOICE.value -> view =
                inflater.inflate(R.layout.bottom_fragment_invoice_type, null)
            InvoiceOption.TRANSACTION.value -> view =
                inflater.inflate(R.layout.bottom_fragment_transaction_type, null)
            InvoiceOption.PAYMENT.value -> view =
                inflater.inflate(R.layout.bottom_fragment_payment, null)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        when (typeValue) {
            InvoiceOption.INVOICE.value -> initListenerInvoice()
            InvoiceOption.TRANSACTION.value -> initListenerTransaction()
            InvoiceOption.PAYMENT.value -> initListenerPayment()
        }
    }

    private fun initListenerPayment() {

        bottom_fragment_card.setOnClickListener {
            result?.setTitle(bottom_fragment_card.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }

        bottom_fragment_cash.setOnClickListener {
            result?.setTitle(bottom_fragment_cash.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }

        bottom_fragment_other.setOnClickListener {
            result?.setTitle(bottom_fragment_other.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }

        bottom_fragment_check.setOnClickListener {
            result?.setTitle(bottom_fragment_check.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }

        bottom_fragment_wire_transfer.setOnClickListener {
            result?.setTitle(bottom_fragment_wire_transfer.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }

        bottom_fragment_voucher.setOnClickListener {
            result?.setTitle(bottom_fragment_voucher.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }

        bottom_fragment_mobile.setOnClickListener {
            result?.setTitle(bottom_fragment_mobile.text as String, InvoiceOption.PAYMENT)
            dismiss()
        }
    }

    private fun initListenerTransaction() {
        bottom_fragment_invoice.setOnClickListener {
            val selectedValue = bottom_fragment_invoice.text as String

            result?.setTitle(selectedValue, InvoiceOption.TRANSACTION)
            result?.onTransactionTypeChanged(TransactionType.SALE, selectedValue)

            dismiss()
        }
        bottom_fragment_refund.setOnClickListener {
            val refundText = bottom_fragment_refund.text as String

            result?.setTitle(refundText, InvoiceOption.TRANSACTION)
            result?.onTransactionTypeChanged(TransactionType.REFUND, refundText)

            dismiss()
        }
    }

    private fun initListenerInvoice() {
        bottom_fragment_normal.setOnClickListener {
            val normalText = bottom_fragment_normal.text as String

            result?.setTitle(normalText, InvoiceOption.INVOICE)
            result?.onInvoiceTypeChanged(InvoiceType.NORMAL, normalText)

            dismiss()
        }

        bottom_fragment_proforma.setOnClickListener {
            val selectedText = bottom_fragment_proforma.text as String

            result?.setTitle(bottom_fragment_proforma.text as String, InvoiceOption.INVOICE)
            result?.onInvoiceTypeChanged(InvoiceType.PROFORMA, selectedText)

            dismiss()
        }

        bottom_fragment_copy.setOnClickListener {
            val selectedText = bottom_fragment_copy.text as String

            result?.setTitle(selectedText, InvoiceOption.INVOICE)
            result?.onInvoiceTypeChanged(InvoiceType.COPY, selectedText)

            dismiss()
        }

        bottom_fragment_training.setOnClickListener {
            val selectedText = bottom_fragment_training.text as String

            result?.setTitle(bottom_fragment_training.text as String, InvoiceOption.INVOICE)
            result?.onInvoiceTypeChanged(InvoiceType.TRAINING, selectedText)

            dismiss()
        }
    }
}
