package online.taxcore.pos.ui.catalog

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.isItemChecked
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import kotlinx.android.synthetic.main.catalog_dashboard_fragment.*
import kotlinx.android.synthetic.main.dialog_loading.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.enums.ExportMimeType
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.helpers.StorageHelper
import online.taxcore.pos.utils.CatalogFileManager
import java.io.*
import java.util.*
import javax.inject.Inject

class CatalogDashFragment : Fragment() {

    @Inject
    lateinit var prefService: PrefService

    private lateinit var arrayAdapter: ArrayAdapter<String>

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.catalog_dashboard_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setClickListeners()
        val exportOptions = arrayListOf("CSV", "JSON")
        context?.let {
            arrayAdapter = ArrayAdapter(it, R.layout.dropdown_item, exportOptions)
        }
    }

    override fun onResume() {
        super.onResume()
        setDashboardButtons()
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
            IMPORT_CATALOG -> {
                resultData?.let {
                    handleImportActivityResult(it, requestCode)
                }
            }
            EXPORT_JSON_CATALOG, EXPORT_CSV_CATALOG -> {
                resultData?.let {
                    handleExportActivityResult(it, requestCode)
                }
            }
        }

    }

    private fun handleImportActivityResult(intent: Intent, requestCode: Int) {
        intent.data?.also { uri ->
            if (StorageHelper.isStorageAvailable()) {
                val jsonFile = StorageHelper.fileFromContentUri(requireContext(), uri)
                importCatalogFrom(jsonFile)
                return
            }

            longToast("Storage unavailable")
        }
    }

    private fun handleExportActivityResult(intent: Intent, requestCode: Int) {
        try {
            val data = intent.data ?: return

            val fileType = when (requestCode) {
                EXPORT_JSON_CATALOG -> ExportMimeType.JSON
                EXPORT_CSV_CATALOG -> ExportMimeType.CSV
                else -> ExportMimeType.JSON
            }

            requireActivity().contentResolver.openFileDescriptor(data, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { outStream ->
                    // Write file encoding
                    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                    outStream.write(bom)
                    val fileContent = getFileContent(fileType)
                    outStream.write(fileContent.toByteArray(Charsets.UTF_8))

                    runOnUiThread {
                        toast(R.string.toast_catalog_exported)
                    }
                }
            }

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
            toast("Unable to export catalog. File not found.")
        } catch (e: IOException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            toast("Unable to export catalog.")
            e.printStackTrace()
        }
    }

    private fun getFileContent(fileType: ExportMimeType): String {
        val catalogItems = CatalogManager.loadCatalogItems()
        return when (fileType) {
            ExportMimeType.JSON -> {
                CatalogFileManager.generateJsonFileContent(catalogItems)
            }
            ExportMimeType.CSV -> {
                val tableHeader = arrayListOf(
                    getString(R.string.ean_barcode).replace(":", ""),
                    getString(R.string.name).replace(":", ""),
                    getString(R.string.hint_unit_price),
                    getString(R.string.label_tax_labels),
                    getString(R.string.label_favorites),
                )
                CatalogFileManager.generateCsvFileContent(
                    tableHeader,
                    catalogItems
                )
            }
            else -> throw Error("Unsupported file type!")
        }
    }

    private fun setDashboardButtons() {
        val hasItems = CatalogManager.hasCatalogItems()
        val isAppConfigured = AppSession.isAppConfigured

        val configuredColor =
            if (isAppConfigured) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")
        val configuredWithItemsColor =
            if (isAppConfigured and hasItems) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")

        catalogAddItemButton.isEnabled = isAppConfigured
        catalogAddItemButton.foreground = ColorDrawable(configuredColor)

        catalogSearchItemsButton.isEnabled = hasItems and isAppConfigured
        catalogSearchItemsButton.foreground = ColorDrawable(configuredWithItemsColor)

        catalogViewItemsButton.isEnabled = hasItems and isAppConfigured
        catalogViewItemsButton.foreground = ColorDrawable(configuredWithItemsColor)

        catalogImportButton.isEnabled = isAppConfigured
        catalogImportButton.foreground = ColorDrawable(configuredColor)

        catalogExportButton.isEnabled = hasItems and isAppConfigured
        catalogExportButton.foreground = ColorDrawable(configuredWithItemsColor)

    }

    private fun setClickListeners() {
        catalogViewItemsButton.setOnClickListener {
            baseActivity()?.let { activity ->
                CatalogDetailsActivity.start(activity)
            }
        }

        catalogAddItemButton.setOnClickListener {
            baseActivity()?.let { activity ->
                ItemDetailActivity.start(activity)
            }
        }

        catalogSearchItemsButton.setOnClickListener {
            baseActivity()?.let {
                CatalogDetailsActivity.start(it, "EXTRA_CATALOG_SEARCH")
            }
        }

        catalogExportButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startCatalogExport()
            } else {
                attemptCatalogExport()
            }
        }

        catalogImportButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                showCatalogImportConfirmDialog {
                    openFile(IMPORT_CATALOG)
                }
            } else {
                attemptCatalogImport()
            }
        }
    }

    private fun attemptCatalogExport() {
        Dexter.withContext(activity)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                    startCatalogExport()
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

    @SuppressLint("CheckResult")
    private fun startCatalogExport() {
        MaterialDialog(requireContext()).show {
            title(R.string.title_export_catalog)
            message(R.string.msg_enter_name_and_export_type)

            input { _, fileName ->
                val exportType = if (isItemChecked(0)) ExportMimeType.CSV else ExportMimeType.JSON
                val exportRequestCode =
                    if (isItemChecked(0)) EXPORT_CSV_CATALOG else EXPORT_JSON_CATALOG

                createFile("$fileName", exportType, exportRequestCode)
            }

            val fileFormats = listOf(".csv", ".json")
            listItemsSingleChoice(
                items = fileFormats,
                initialSelection = 0,
                waitForPositiveButton = false
            ) { _, _, text ->
                val input = getInputField().text.toString()
                setActionButtonEnabled(WhichButton.POSITIVE, input.isNotBlank())

                val exportPath = if (input.isEmpty()) "" else "$input$text"
                getInputLayout().hint = exportPath
            }

            setActionButtonEnabled(WhichButton.POSITIVE, false)

            getInputField().onTextChanged { input ->

                setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    input.isNotBlank()
                )

                val ext = if (isItemChecked(0)) fileFormats[0] else fileFormats[1]
                val exportPath = if (input.isEmpty()) "" else "$input$ext"

                getInputLayout().hint = exportPath
            }

            positiveButton(R.string.title_export)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }

    }

    private fun attemptCatalogImport() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    showCatalogImportConfirmDialog {
                        startCatalogImport()
                    }
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

    private fun showCatalogImportConfirmDialog(onConfirm: () -> Unit) {
        val listIsEmpty = CatalogManager.hasCatalogItems().not()

        if (listIsEmpty) {
            onConfirm()
            return
        }

        MaterialDialog(requireContext()).show {
            title(R.string.dialog_title_important)
            message(R.string.dialog_message_confirm_catalog_import)
            positiveButton(R.string.dialog_button_import) {
                onConfirm()
            }

            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun startCatalogImport() {
        val jsonOrCsvFilter: FileFilter = {
            it.isDirectory or it.extension.endsWith("json", true) or it.extension.endsWith(
                "csv",
                true
            )
        }

        MaterialDialog(requireContext()).show {
            message(text = getString(R.string.msg_select_json_or_csv))
            fileChooser(
                context = context,
                filter = jsonOrCsvFilter,
                initialDirectory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                )
            ) { _, file ->

                if (file.length() > 0) {
                    this.dismiss()
                    importCatalogFrom(file)
                    return@fileChooser
                }

                longToast(R.string.toast_file_is_empty)
            }
            positiveButton(R.string.import_catalog)
            negativeButton(R.string.btn_close) {
                dismiss()
            }
        }
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).show {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }

    private fun importCatalogFrom(file: File) {
        val loadingDialog = createLoadingDialog(R.string.catalog_import_in_progress)

        when (file.extension.lowercase(Locale.ROOT)) {
            "csv" -> CatalogFileManager.importCsvCatalog(file, context,
                onSuccess = { catalogList ->
                    handleCatalogImport(catalogList, loadingDialog)
                },
                onError = { error ->
                    runOnUiThread {
                        loadingDialog.dismiss()
                        toast(error)
                    }
                })
            "json" -> CatalogFileManager.importJsonCatalog(file, context,
                onSuccess = { catalogList ->
                    handleCatalogImport(catalogList, loadingDialog)
                },
                onError = { error ->
                    runOnUiThread {
                        loadingDialog.dismiss()
                        toast(error)
                    }
                })
            else -> {
                loadingDialog.dismiss()
                longToast("Invalid format")
            }
        }
    }

    private fun handleCatalogImport(catalogList: List<Item>, loadingDialog: MaterialDialog) {
        runOnUiThread {
            if (catalogList.isEmpty()) {
                longToast(getString(R.string.toast_nothing_to_import))
                return@runOnUiThread
            }

            loadingDialog.dismiss()
            longToast(R.string.toast_catalog_imported)

            // Update UI
            setDashboardButtons()
        }
    }

    private fun openFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
            }
        }

        requireActivity().startActivityFromFragment(this, intent, requestCode)
    }

    private fun createFile(fileName: String, mimeType: ExportMimeType, requestCode: Int) {
        val outputFileName = when (mimeType) {
            ExportMimeType.JSON -> "$fileName.json"
            ExportMimeType.CSV -> "$fileName.csv"
            else -> throw Error("Invalid file type")
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)

            type = mimeType.type

            putExtra(Intent.EXTRA_TITLE, outputFileName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS)
            }
        }

        requireActivity().startActivityFromFragment(this, intent, requestCode)
    }

    companion object {
        // Request code for catalog
        private const val IMPORT_CATALOG = 201
        private const val EXPORT_JSON_CATALOG = 301
        private const val EXPORT_CSV_CATALOG = 401
    }
}

