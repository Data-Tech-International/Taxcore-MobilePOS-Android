package online.taxcore.pos.ui.journal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.google.gson.stream.JsonWriter
import com.karumi.dexter.Dexter
import com.vicpin.krealmextensions.deleteAll
import io.realm.Realm
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.data.local.JournalManager
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.databinding.JournalDashboardFragmentBinding
import online.taxcore.pos.enums.ExportMimeType
import online.taxcore.pos.enums.JournalError
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.helpers.StorageHelper
import online.taxcore.pos.utils.JsonFileManager
import online.taxcore.pos.utils.longToast
import online.taxcore.pos.utils.toast
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

@Suppress("PrivatePropertyName")
@android.annotation.SuppressLint("ClickableViewAccessibility")
class JournalDashFragment : Fragment() {

    private var _binding: JournalDashboardFragmentBinding? = null
    private val binding get() = _binding!!

    // Request code for selecting a PDF document.
    private val IMPORT_JOURNAL_FILE = 10
    private val EXPORT_JOURNAL = 100
    private val EXPORT_AND_CLEAR_JOURNAL = 101

    // Secret long press duration (5 seconds)
    private val SECRET_HOLD_DURATION = 5000L
    private val secretExportHandler = Handler(Looper.getMainLooper())
    private var secretExportRunnable: Runnable? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = JournalDashboardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setClickListeners()
    }

    override fun onResume() {
        super.onResume()
        setDashboardButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        secretExportRunnable?.let { secretExportHandler.removeCallbacks(it) }
        _binding = null
    }

    private fun setDashboardButtons() {
        val hasInvoiceItems = JournalManager.hasJournalItems()
        val isAppConfigured = AppSession.isAppConfigured

        val configuredColor =
            if (isAppConfigured) Color.TRANSPARENT else "#90EEEEEE".toColorInt()
        val configuredWithItemsColor =
            if (isAppConfigured and hasInvoiceItems) Color.TRANSPARENT else "#90EEEEEE".toColorInt()

        binding.journalViewItemsButton.isEnabled = hasInvoiceItems and isAppConfigured
        binding.journalViewItemsButton.foreground = configuredWithItemsColor.toDrawable()

        binding.journalSearchItemsButton.isEnabled = hasInvoiceItems and isAppConfigured
        binding.journalSearchItemsButton.foreground = configuredWithItemsColor.toDrawable()

        binding.journalImportButton.isEnabled = isAppConfigured
        binding.journalImportButton.foreground = configuredColor.toDrawable()

        binding.journalExportButton.isEnabled = hasInvoiceItems and isAppConfigured
        binding.journalExportButton.foreground = configuredWithItemsColor.toDrawable()

    }

    private fun setClickListeners() {
        binding.journalViewItemsButton.setOnClickListener {
            baseActivity()?.let { activity ->
                JournalDetailsActivity.start(activity)
            }
        }

        binding.journalSearchItemsButton.setOnClickListener {
            baseActivity()?.let {
                JournalDetailsActivity.start(it, "JOURNAL_SEARCH")
            }
        }

        binding.journalExportButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startJournalExport()
            } else {
                attemptJournalExport()
            }
        }

        // Secret long press (5 seconds) to export and clear journal
        binding.journalExportButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    secretExportRunnable = Runnable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startJournalExportAndClear()
                        } else {
                            attemptJournalExportAndClear()
                        }
                    }
                    secretExportHandler.postDelayed(secretExportRunnable!!, SECRET_HOLD_DURATION)
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    secretExportRunnable?.let { secretExportHandler.removeCallbacks(it) }
                    secretExportRunnable = null
                    false
                }
                else -> false
            }
        }

        binding.journalImportButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                openFile(ExportMimeType.JSON)
            } else {
                attemptJournalImport()
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        // If the selection didn't work
        if (resultCode != Activity.RESULT_OK) {
            // Exit without doing anything else
            return
        }

        when (requestCode) {
            IMPORT_JOURNAL_FILE -> {
                resultData?.data?.also { uri ->
                    if (StorageHelper.isStorageAvailable()) {
                        val jsonFile = StorageHelper.fileFromContentUri(requireContext(), uri)
                        importJournalFrom(jsonFile)
                        return
                    }

                    longToast(getString(R.string.toast_storage_unavailable))
                }
            }

            EXPORT_JOURNAL -> {
                resultData?.data?.also { uri ->
                    exportJournalWithProgress(uri)
                }
            }

            EXPORT_AND_CLEAR_JOURNAL -> {
                resultData?.data?.also { uri ->
                    exportJournalWithProgress(uri, clearAfterExport = true)
                }
            }
        }
    }

    private fun attemptJournalExport() {
        Dexter.withContext(activity).withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                    startJournalExport()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {/* ... */
                    toast(getString(R.string.denied_permission))
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest, token: PermissionToken
                ) {/* ... */
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun startJournalExport() {
        MaterialDialog(requireContext()).show {
            title(R.string.title_export_journal)
            message(text = getString(R.string.title_enter_file_name))

            val dialog = input { _, fileName ->
                createFile(fileName.toString())
            }

            getInputField().onTextChanged { input ->
                val exportPath = if (input.isEmpty()) "" else "$input.json"
                dialog.getInputLayout().hint = exportPath
            }

            positiveButton(R.string.title_export)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun attemptJournalExportAndClear() {
        Dexter.withContext(activity).withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    startJournalExportAndClear()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    toast(getString(R.string.denied_permission))
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest, token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun startJournalExportAndClear() {
        MaterialDialog(requireContext()).show {
            title(R.string.dialog_title_export_and_clear)
            message(R.string.dialog_message_export_and_clear_warning)
            positiveButton(R.string.title_export) {
                showExportAndClearFileNameDialog()
            }
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun showExportAndClearFileNameDialog() {
        MaterialDialog(requireContext()).show {
            title(R.string.dialog_title_export_and_clear)
            message(text = getString(R.string.title_enter_file_name))

            val dialog = input { _, fileName ->
                createFileForExportAndClear(fileName.toString())
            }

            getInputField().onTextChanged { input ->
                val exportPath = if (input.isEmpty()) "" else "$input.json"
                dialog.getInputLayout().hint = exportPath
            }

            positiveButton(R.string.title_export)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun attemptJournalImport() {
        Dexter.withContext(activity).withPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                startJournalImport()
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?, token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
                toast(getString(R.string.denied_permission))
            }
        }).check()
    }

    private fun startJournalImport() {
        val jsonFilter: FileFilter = {
            it.isDirectory or it.extension.endsWith("json", true)
        }

        MaterialDialog(requireContext()).show {
            title(R.string.title_import)
            message(R.string.msg_select_json)
            fileChooser(
                context = context,
                filter = jsonFilter,
                initialDirectory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                )
            ) { _, file ->

                if (file.length() > 0) {
                    importJournalFrom(file)
                    return@fileChooser
                }

                longToast(getString(R.string.toast_file_is_empty))
            }
            positiveButton(R.string.title_import)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun importJournalFrom(file: File) {
        val progressDialog = MaterialDialog(requireContext()).show {
            title(R.string.title_import)
            message(R.string.msg_reading_file)
            cancelable(false)
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                JsonFileManager.importJournalsWithProgress(
                    context = context,
                    sourceFile = file,
                    onProgress = { current, total ->
                        launch(Dispatchers.Main) {
                            progressDialog.message(
                                text = getString(R.string.msg_import_progress, current, total)
                            )
                        }
                    },
                    onSuccess = { journalList ->
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            if (journalList.isNotEmpty()) {
                                setDashboardButtons()
                                longToast(getString(R.string.toast_journal_imported))
                            } else {
                                longToast(getString(R.string.toast_nothing_to_import))
                            }
                        }
                    },
                    onError = { error ->
                        launch(Dispatchers.Main) {
                            progressDialog.dismiss()
                            longToast(getString(error.messageResId))
                        }
                    }
                )
            }
        }
    }

    private fun openFile(exportFileType: ExportMimeType) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Intent.normalizeMimeType(exportFileType.type)

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
            }
        }

        startActivityForResult(intent, IMPORT_JOURNAL_FILE)
    }

    private fun createFile(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)

            type = "application/json"

            putExtra(Intent.EXTRA_TITLE, "$fileName.json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
            }
        }

        startActivityForResult(intent, EXPORT_JOURNAL)
    }

    private fun createFileForExportAndClear(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)

            type = "application/json"

            putExtra(Intent.EXTRA_TITLE, "$fileName.json")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
            }
        }

        startActivityForResult(intent, EXPORT_AND_CLEAR_JOURNAL)
    }

    /**
     * Exports journals with a progress dialog.
     * Shows progress to the user while exporting large datasets.
     * @param clearAfterExport If true, clears all journal entries after successful export
     */
    private fun exportJournalWithProgress(uri: android.net.Uri, clearAfterExport: Boolean = false) {
        val progressDialog = MaterialDialog(requireContext()).show {
            title(R.string.title_export_journal)
            message(R.string.msg_exporting_journal)
            cancelable(false)
        }

        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fileOS ->
                            exportJournalsStreaming(fileOS) { current, total ->
                                launch(Dispatchers.Main) {
                                    progressDialog.message(
                                        text = getString(R.string.msg_export_progress, current, total)
                                    )
                                }
                            }
                        }
                        true
                    } ?: false
                }

                progressDialog.dismiss()
                if (success) {
                    if (clearAfterExport) {
                        Journal().deleteAll()
                        setDashboardButtons()
                        toast(getString(R.string.toast_journal_exported))
                    } else {
                        toast(getString(R.string.toast_journal_exported))
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                progressDialog.dismiss()
            } catch (e: IOException) {
                e.printStackTrace()
                progressDialog.dismiss()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                progressDialog.dismiss()
                longToast(getString(R.string.toast_export_failed))
            }
        }
    }

    /**
     * Exports journals using streaming JSON to avoid OOM errors.
     * Writes each journal item directly to the output stream without
     * loading all items into memory at once.
     *
     * @param onProgress Callback to report progress (current, total)
     */
    private fun exportJournalsStreaming(
        fileOS: FileOutputStream,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        val realm = Realm.getDefaultInstance()
        try {
            val journalResults = JournalManager.queryJournalItems(realm)
            val totalCount = journalResults.size
            var currentCount = 0

            OutputStreamWriter(fileOS, Charsets.UTF_8).use { writer ->
                JsonWriter(writer).use { jsonWriter ->
                    jsonWriter.setIndent("  ")
                    jsonWriter.beginArray()

                    for (journal in journalResults) {
                        jsonWriter.beginObject()
                        jsonWriter.name("id").value(journal.id)
                        jsonWriter.name("date").value(journal.date)
                        jsonWriter.name("rec").value(journal.rec)
                        jsonWriter.name("total").value(journal.total)
                        jsonWriter.name("qrCode").value(journal.qrCode)
                        jsonWriter.name("message").value(journal.message)
                        jsonWriter.name("invoiceNumber").value(journal.invoiceNumber)
                        jsonWriter.name("RequestedBy").value(journal.RequestedBy)
                        jsonWriter.name("IC").value(journal.IC)
                        jsonWriter.name("InvoiceCounterExtension").value(journal.InvoiceCounterExtension)
                        jsonWriter.name("VerificationUrl").value(journal.VerificationUrl)
                        jsonWriter.name("SignedBy").value(journal.SignedBy)
                        jsonWriter.name("ID").value(journal.ID)
                        jsonWriter.name("S").value(journal.S)
                        jsonWriter.name("TotalCounter").value(journal.TotalCounter)
                        jsonWriter.name("TransactionTypeCounter").value(journal.TransactionTypeCounter)
                        jsonWriter.name("TaxGroupRevision").value(journal.TaxGroupRevision)
                        jsonWriter.name("buyerTin").value(journal.buyerTin)
                        jsonWriter.name("transactionType").value(journal.transactionType)
                        jsonWriter.name("paymentType").value(journal.paymentType)
                        jsonWriter.name("invoiceType").value(journal.invoiceType)
                        jsonWriter.name("buyerCostCenter").value(journal.buyerCostCenter)
                        jsonWriter.name("invoiceItemsData").value(journal.invoiceItemsData)
                        jsonWriter.name("type").value(journal.type)
                        jsonWriter.endObject()

                        currentCount++
                        // Update progress every 100 items to avoid too frequent UI updates
                        if (currentCount % 100 == 0 || currentCount == totalCount) {
                            onProgress?.invoke(currentCount, totalCount)
                        }
                    }

                    jsonWriter.endArray()
                }
            }
        } finally {
            realm.close()
        }
    }
}
