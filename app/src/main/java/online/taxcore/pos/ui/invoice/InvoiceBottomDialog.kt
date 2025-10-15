package online.taxcore.pos.ui.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import online.taxcore.pos.databinding.BottomFragmentInvoiceTypeBinding
import online.taxcore.pos.databinding.BottomFragmentPaymentBinding
import online.taxcore.pos.databinding.BottomFragmentTransactionTypeBinding
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
    private var invoiceBinding: BottomFragmentInvoiceTypeBinding? = null
    private var transactionBinding: BottomFragmentTransactionTypeBinding? = null
    private var paymentBinding: BottomFragmentPaymentBinding? = null

    companion object {
        var result: OnInvoiceOptionResult? = null

        fun showBottomDialog(supportFragmentManager: FragmentManager?, i: Int, invoiceFragment: InvoiceFragment) {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        typeValue = arguments?.getInt("Value")
        return when (typeValue) {
            InvoiceOption.INVOICE.value -> {
                invoiceBinding = BottomFragmentInvoiceTypeBinding.inflate(inflater, container, false)
                invoiceBinding?.root
            }
            InvoiceOption.TRANSACTION.value -> {
                transactionBinding = BottomFragmentTransactionTypeBinding.inflate(inflater, container, false)
                transactionBinding?.root
            }
            InvoiceOption.PAYMENT.value -> {
                paymentBinding = BottomFragmentPaymentBinding.inflate(inflater, container, false)
                paymentBinding?.root
            }
            else -> null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        when (typeValue) {
            InvoiceOption.INVOICE.value -> initListenerInvoice()
            InvoiceOption.TRANSACTION.value -> initListenerTransaction()
            InvoiceOption.PAYMENT.value -> initListenerPayment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        invoiceBinding = null
        transactionBinding = null
        paymentBinding = null
    }

    private fun initListenerPayment() {
        paymentBinding?.let { binding ->
            binding.bottomFragmentCard.setOnClickListener {
                result?.setTitle(binding.bottomFragmentCard.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.CARD, "")
                dismiss()
            }

            binding.bottomFragmentCash.setOnClickListener {
                result?.setTitle(binding.bottomFragmentCash.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.CASH, "")
                dismiss()
            }

            binding.bottomFragmentOther.setOnClickListener {
                result?.setTitle(binding.bottomFragmentOther.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.OTHER, "")
                dismiss()
            }

            binding.bottomFragmentCheck.setOnClickListener {
                result?.setTitle(binding.bottomFragmentCheck.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.CHECK, "")
                dismiss()
            }

            binding.bottomFragmentWireTransfer.setOnClickListener {
                result?.setTitle(binding.bottomFragmentWireTransfer.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.WIRE_TRANSFER, "")
                dismiss()
            }

            binding.bottomFragmentVoucher.setOnClickListener {
                result?.setTitle(binding.bottomFragmentVoucher.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.VOUCHER, "")
                dismiss()
            }

            binding.bottomFragmentMobile.setOnClickListener {
                result?.setTitle(binding.bottomFragmentMobile.text as String, InvoiceOption.PAYMENT)
                result?.onPaymentChanged(PaymentType.MOBILE_MONEY, "")
                dismiss()
            }
        }
    }

    private fun initListenerTransaction() {
        transactionBinding?.let { binding ->
            binding.bottomFragmentInvoice.setOnClickListener {
                val selectedValue = binding.bottomFragmentInvoice.text as String

                result?.setTitle(selectedValue, InvoiceOption.TRANSACTION)
                result?.onTransactionTypeChanged(TransactionType.SALE, selectedValue)

                dismiss()
            }

            binding.bottomFragmentRefund.setOnClickListener {
                val refundText = binding.bottomFragmentRefund.text as String

                result?.setTitle(refundText, InvoiceOption.TRANSACTION)
                result?.onTransactionTypeChanged(TransactionType.REFUND, refundText)

                dismiss()
            }
        }
    }

    private fun initListenerInvoice() {
        invoiceBinding?.let { binding ->
            binding.bottomFragmentNormal.setOnClickListener {
                val normalText = binding.bottomFragmentNormal.text as String

                result?.setTitle(normalText, InvoiceOption.INVOICE)
                result?.onInvoiceTypeChanged(InvoiceType.NORMAL, normalText)

                dismiss()
            }

            binding.bottomFragmentProforma.setOnClickListener {
                val selectedText = binding.bottomFragmentProforma.text as String

                result?.setTitle(binding.bottomFragmentProforma.text as String, InvoiceOption.INVOICE)
                result?.onInvoiceTypeChanged(InvoiceType.PROFORMA, selectedText)

                dismiss()
            }

            binding.bottomFragmentCopy.setOnClickListener {
                val selectedText = binding.bottomFragmentCopy.text as String

                result?.setTitle(selectedText, InvoiceOption.INVOICE)
                result?.onInvoiceTypeChanged(InvoiceType.COPY, selectedText)

                dismiss()
            }

//        binding.bottomFragmentAdvance.setOnClickListener {
//            val selectedText = binding.bottomFragmentAdvance.text as String
//
//            result?.setTitle(selectedText, InvoiceOption.INVOICE)
//            result?.onInvoiceTypeChanged(InvoiceType.ADVANCE, selectedText)
//
//            dismiss()
//        }

            binding.bottomFragmentTraining.setOnClickListener {
                val selectedText = binding.bottomFragmentTraining.text as String

                result?.setTitle(binding.bottomFragmentTraining.text as String, InvoiceOption.INVOICE)
                result?.onInvoiceTypeChanged(InvoiceType.TRAINING, selectedText)

                dismiss()
            }
        }
    }
}
