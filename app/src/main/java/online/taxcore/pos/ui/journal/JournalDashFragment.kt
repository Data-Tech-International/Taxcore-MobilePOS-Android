package online.taxcore.pos.ui.journal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.google.gson.GsonBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.runOnUiThread
import com.pawegio.kandroid.toast
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.journal_dashboard_fragment.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.data.local.JournalManager
import online.taxcore.pos.enums.ExportMimeType
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.helpers.StorageHelper
import online.taxcore.pos.utils.JsonFileManager
import java.io.*

@Suppress("PrivatePropertyName")
class JournalDashFragment : Fragment() {

    // Request code for selecting a PDF document.
    private val IMPORT_JOURNAL_FILE = 10
    private val EXPORT_JOURNAL = 100

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.journal_dashboard_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setClickListeners()
    }

    override fun onResume() {
        super.onResume()
        setDashboardButtons()
    }

    private fun setDashboardButtons() {
        val hasInvoiceItems = JournalManager.hasJournalItems()
        val isAppConfigured = AppSession.isAppConfigured

        val configuredColor =
            if (isAppConfigured) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")
        val configuredWithItemsColor =
            if (isAppConfigured and hasInvoiceItems) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")

        journalViewItemsButton.isEnabled = hasInvoiceItems and isAppConfigured
        journalViewItemsButton.foreground = ColorDrawable(configuredWithItemsColor)

        journalSearchItemsButton.isEnabled = hasInvoiceItems and isAppConfigured
        journalSearchItemsButton.foreground = ColorDrawable(configuredWithItemsColor)

        journalImportButton.isEnabled = isAppConfigured
        journalImportButton.foreground = ColorDrawable(configuredColor)

        journalExportButton.isEnabled = hasInvoiceItems and isAppConfigured
        journalExportButton.foreground = ColorDrawable(configuredWithItemsColor)

    }

    private fun setClickListeners() {
        journalViewItemsButton.setOnClickListener {
            baseActivity()?.let { activity ->
                JournalDetailsActivity.start(activity)
            }
        }

        journalSearchItemsButton.setOnClickListener {
            baseActivity()?.let {
                JournalDetailsActivity.start(it, "JOURNAL_SEARCH")
            }
        }

        journalExportButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startJournalExport()
            } else {
                attemptJournalExport()
            }
        }

        journalImportButton.setOnClickListener {
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

                    longToast("Storage unavailable")
                }
            }
            EXPORT_JOURNAL -> {
                try {
                    resultData?.data?.also { uri ->
                        requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use {
                            FileOutputStream(it.fileDescriptor).use { fileOS ->
                                val journalItems = JournalManager.loadJournalItems()

                                val gson = GsonBuilder().setPrettyPrinting().create()
                                val itemsJson = gson.toJson(journalItems)

                                fileOS.write(itemsJson.toByteArray(Charsets.UTF_8))
                                runOnUiThread {
                                    toast(R.string.toast_journal_exported)
                                }
                            }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
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

                longToast(R.string.toast_file_is_empty)
            }
            positiveButton(R.string.title_import)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun importJournalFrom(file: File) {
        val journalList = JsonFileManager.importJournals(activity, file)

        if (journalList.isNotEmpty()) {

            // Update UI
            setDashboardButtons()

            longToast(R.string.toast_journal_imported)
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
}
