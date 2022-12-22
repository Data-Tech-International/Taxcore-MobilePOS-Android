package online.taxcore.pos.ui.journal

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
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
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.utils.Helpers
import online.taxcore.pos.utils.JsonFileManager
import java.io.File

class JournalDashFragment : Fragment() {

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.journal_dashboard_fragment, container, false)

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
            attemptJournalExport()
        }

        journalImportButton.setOnClickListener {
            attemptJournalImport()
        }

    }

    private fun attemptJournalExport() {
        Dexter.withContext(activity)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                    startJournalExport()
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

    private fun startJournalExport() {
        MaterialDialog(requireContext()).show {
            title(R.string.title_export_journal)
            message(text = getString(R.string.title_enter_file_name))

            val dialog = input { _, fileName ->
                // Text submitted with the action button, might be an empty string`
                val exportFilePath = Helpers.createDocumentsFilePath("$fileName.json")
                exportJournal(exportFilePath)
            }

            getInputField().onTextChanged { input ->
                val exportPath = if (input.isEmpty()) "" else "/Documents/$input.json"
                dialog.getInputLayout().hint = exportPath
            }

            positiveButton(R.string.title_export)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun attemptJournalImport() {
        Dexter.withContext(activity)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    startJournalImport()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                    toast(getString(R.string.denied_permission))
                }
            }).check()
    }

    private fun exportJournal(filePath: String) {

        val journalItems = JournalManager.loadJournalItems()
        JsonFileManager.exportJournal(context, filePath, journalItems,
            onSuccess = {
                runOnUiThread {
                    toast(R.string.toast_journal_exported)
                }
            },
            onError = { error ->
                runOnUiThread {
                    toast(error)
                }
            }
        )
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
}
