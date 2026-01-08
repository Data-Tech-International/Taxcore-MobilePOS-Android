package online.taxcore.pos.ui.invoice

// import online.taxcore.pos.utils.CreatePdf // TODO: Migrate to iText7
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.print.PrintManager
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import online.taxcore.pos.BuildConfig
import online.taxcore.pos.R
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.databinding.InvoicePreviewDialogBinding
import online.taxcore.pos.enums.ExportMimeType
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.utils.CreatePdf
import online.taxcore.pos.utils.toast
import java.io.File

class FiscalInvoiceFragment : DialogFragment() {

    private var _binding: InvoicePreviewDialogBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = InvoicePreviewDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val delimiter = "========================================"
        val typeface = Typeface.createFromAsset(activity?.assets, "fonts/ConsolaMono.ttf")

        val invoiceJournal = arguments?.getString("Invoice") ?: ""
        val invoiceFooter = invoiceJournal.split(delimiter).last()
        val invoiceContent = invoiceJournal.replace(invoiceFooter, "")

        binding.dialogFragmentInvoice.typeface = typeface
        binding.dialogFragmentInvoice.text = invoiceContent
        binding.dialogFragmentInvoice.gravity = Gravity.CENTER_HORIZONTAL

        binding.dialogFragmentInvoiceEnd.text = invoiceFooter
        binding.dialogFragmentInvoiceEnd.typeface = typeface

        showQrCode()

        val invoiceNumber = arguments?.getString("Message")

        binding.fiscalDialogCloseButton.setOnClickListener {
            when (requireArguments().getString("ARG_FRAGMENT_TYPE")) {
                "copy", "refund" -> activity?.finish()
                else -> dismiss()
            }
        }

        binding.mainAppBarShare.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                invoiceNumber?.let { invoiceNo ->
                    createAndSharePdf(
                        invoiceNo,
                        invoiceJournal
                    )

                }
                return@setOnClickListener
            }

            Dexter.withContext(activity)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                        invoiceNumber?.let { it1 ->
                            createAndSharePdf(
                                it1,
                                invoiceJournal
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

        binding.mainAppBarPrint.setOnClickListener {
            printInvoice(invoiceNumber!!, invoiceJournal)
            // createWebPrintJob(getBitmapFromView(dialog_fragment_invoice_container))
        }
    }

    override fun getTheme(): Int {
        return R.style.MyCustomThemeDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun printInvoice(
        invoiceNumber: String,
        invoiceJournal: String = ""
    ) {
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        //Create file path for Pdf
        val fileDirPath = requireActivity().cacheDir.absolutePath
        val fullFilePath = fileDirPath + File.separator + invoiceNumber + ".pdf"

        val content =
            CreatePdf.write(fullFilePath, invoiceJournal, imageByteArray)
        if (content) {
            val act = activity as BaseActivity
            val printManager = act.originalActivityContext()
                .getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = this.getString(R.string.app_name) + " doc"
            printManager.print(jobName, MyPrintDocumentAdapter(invoiceNumber, fileDirPath), null)
        }
    }

    private fun showQrCode() {
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        Glide.with(this)
            .asBitmap()
            .load(imageByteArray)
            .into(binding.dialogFragmentQrCode)
    }

    private fun createAndSharePdf(
        pdfTitle: String? = "",
        invoiceJournal: String = ""
    ) {
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val imageBytes = arguments?.getString("QrCode")
        val imageByteArray = Base64.decode(imageBytes, Base64.DEFAULT)
        //Create file path for Pdf
        val filePath = requireActivity().cacheDir.absolutePath + File.separator + pdfTitle + ".pdf"

        val content =
            CreatePdf.write(filePath, invoiceJournal, imageByteArray)
        if (content) {
            val uriFromFile = context?.let { it1 ->
                FileProvider.getUriForFile(
                    it1,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(filePath)
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
                type = ExportMimeType.PDF.type
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
