package online.taxcore.pos.ui.invoice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.preference.PreferenceManager
import android.print.PrintManager
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.pawegio.kandroid.toast
import kotlinx.android.synthetic.main.invoice_preview_dialog.*
import online.taxcore.pos.BuildConfig
import online.taxcore.pos.R
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.constants.StorageConstants
import online.taxcore.pos.utils.CreatePdf
import java.io.File

class FiscalInvoiceFragment : DialogFragment() {

    companion object {
        fun showFiscalDialog(
            supportFragmentManager: FragmentManager?,
            invoice: String,
            qrCode: String,
            message: String,
            verificationUrl: String?,
            fType: String = "normal"
        ) {
            val TAG = FiscalInvoiceFragment::class.java.simpleName
            val dialog = FiscalInvoiceFragment().apply {
                arguments = Bundle().apply {
                    putString("Invoice", invoice)
                    putString("QrCode", qrCode)
                    putString("Message", message)
                    putString("url", verificationUrl)
                    putString("ARG_FRAGMENT_TYPE", fType)
                }
            }
            val ft = supportFragmentManager?.beginTransaction()!!
            dialog.show(ft, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.invoice_preview_dialog, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val typeface = Typeface.createFromAsset(activity?.assets, "fonts/ConsolaMono.ttf")
        dialog_fragment_invoice.typeface = typeface
        val invoice = arguments?.getString("Invoice") ?: ""
        val footer_check = "======== END OF FISCAL INVOICE ========="
        val footer_check2 = "===== THIS IS NOT A FISCAL RECEIPT ====="

        val check1 = invoice.contains(footer_check)

        val pdf_invoice_replace =
            if (check1) invoice
                .replace(footer_check, "")
            else invoice
                .replace(footer_check2, "")

        val footerText = if (check1) footer_check else footer_check2

        if (check1) {
            dialog_fragment_invoice.text = pdf_invoice_replace.replace("\r", "")
        } else {
            dialog_fragment_invoice.text = footer_check2 + pdf_invoice_replace
        }
        dialog_fragment_invoice_end.typeface = typeface
        dialog_fragment_invoice_end.text = footerText

        showQrCode()

        val invoiceNumber = arguments?.getString("Message")

        fiscalDialogCloseButton.setOnClickListener {
            when (requireArguments().getString("ARG_FRAGMENT_TYPE")) {
                "copy", "refund" -> activity?.finish()
                else -> dismiss()
            }
        }

        main_app_bar_share.setOnClickListener {
            Dexter.withContext(activity)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                        invoiceNumber?.let { it1 ->
                            createAndSharePdf(
                                it1,
                                pdf_invoice_replace,
                                footerText
                            )
                        }
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {/* ... */
                        toast(getString(R.string.denied_permission))
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest,
                        token: PermissionToken
                    ) {/* ... */
                        token.continuePermissionRequest()
                    }
                }).check()
        }

        main_app_bar_print.setOnClickListener {
            printInvoice(invoiceNumber, pdf_invoice_replace, footerText)
            // createWebPrintJob(getBitmapFromView(dialog_fragment_invoice_container))
        }
    }

    override fun getTheme(): Int {
        return R.style.MyCustomThemeDialog
    }

    private fun printInvoice(
        invoiceNumber: String?,
        pdf_invoice_replace: String,
        footer_check: String
    ) {
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        val content =
            CreatePdf.write(invoiceNumber, pdf_invoice_replace, imageByteArray, footer_check)
        if (content) {
            val printManager = activity?.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = this.getString(R.string.app_name) + " Document"
            printManager.print(jobName, MyPrintDocumentAdapter(invoiceNumber, ""), null)
        }
    }

    private fun showQrCode() {
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        Glide.with(this)
            .asBitmap()
            .load(imageByteArray)
            .into(dialog_fragment_qr_code)
    }

    private fun createAndSharePdf(
        pdfTitle: String,
        pdf_invoice_replace: String,
        footer_check: String
    ) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        val content = CreatePdf.write(pdfTitle, pdf_invoice_replace, imageByteArray, footer_check)
        if (content) {
            val uriFromFile = context?.let { it1 ->
                FileProvider.getUriForFile(
                    it1,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(StorageConstants.DOWNLOAD_STORAGE_PATH + "/" + pdfTitle + ".pdf")
                )
            }

            val seller = pref.getString(PrefConstants.CASHIER_NAME, "").orEmpty()
            val url = arguments?.getString("url")

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, getTitleSubject(seller, pdfTitle))
                putExtra(Intent.EXTRA_STREAM, uriFromFile)
                putExtra(Intent.EXTRA_TEXT, url)
                type = "application/pdf"
            }

            startActivity(Intent.createChooser(shareIntent, "Select app"))
        }
    }

    private fun getTitleSubject(seller: String, pdfTitle: String) =
        if (seller.isEmpty()) {
            "Invoice $pdfTitle"
        } else {
            "Invoice $pdfTitle from $seller"
        }
}
