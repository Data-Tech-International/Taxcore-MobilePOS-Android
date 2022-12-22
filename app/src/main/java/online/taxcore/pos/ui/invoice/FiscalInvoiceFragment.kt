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
import android.view.Gravity
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
import online.taxcore.pos.ui.base.BaseActivity
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
        val delimiter = "========================================"
        val typeface = Typeface.createFromAsset(activity?.assets, "fonts/ConsolaMono.ttf")

        val invoiceArg = arguments?.getString("Invoice") ?: ""
        val invoiceFooter = invoiceArg.split(delimiter).last()
        val invoiceText = invoiceArg.replace(invoiceFooter, "")

        dialog_fragment_invoice.typeface = typeface
        dialog_fragment_invoice.text = invoiceText
        dialog_fragment_invoice.gravity = Gravity.CENTER_HORIZONTAL

        dialog_fragment_invoice_end.text = invoiceFooter
        dialog_fragment_invoice_end.typeface = typeface

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
                                invoiceText,
                                invoiceFooter
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
            printInvoice(invoiceNumber!!, invoiceText, invoiceFooter)
            // createWebPrintJob(getBitmapFromView(dialog_fragment_invoice_container))
        }
    }

    override fun getTheme(): Int {
        return R.style.MyCustomThemeDialog
    }

    private fun printInvoice(
        invoiceNumber: String,
        invoiceText: String = "",
        invoiceFooter: String = ""
    ) {
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        val content =
            CreatePdf.write(invoiceNumber, invoiceText, imageByteArray, invoiceFooter)
        if (content) {
            val act = activity as BaseActivity
            val printManager = act.originalActivityContext()
                .getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = this.getString(R.string.app_name) + " doc"
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
        pdfTitle: String? = "",
        pdfInvoice: String? = "",
        pdfInvoiceFooter: String? = ""
    ) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        val content = CreatePdf.write(pdfTitle, pdfInvoice, imageByteArray, pdfInvoiceFooter)
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
                putExtra(Intent.EXTRA_SUBJECT, getTitleSubject(seller, pdfTitle!!))
                putExtra(Intent.EXTRA_STREAM, uriFromFile)
                putExtra(Intent.EXTRA_TEXT, url)
                type = "application/pdf"
            }

            startActivity(Intent.createChooser(shareIntent, getString(R.string.title_select_app)))
        }
    }

    private fun getTitleSubject(seller: String, pdfTitle: String) =
        if (seller.isEmpty()) {
            "${getString(R.string.invoice)} $pdfTitle"
        } else {
            "${getString(R.string.invoice)} $pdfTitle from $seller"
        }
}
