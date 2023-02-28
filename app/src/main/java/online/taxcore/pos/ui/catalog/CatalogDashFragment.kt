package online.taxcore.pos.ui.catalog

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.google.android.material.textfield.TextInputEditText
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
import online.taxcore.pos.data_managers.CatalogManager
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.models.Item
import online.taxcore.pos.utils.CsvFileManager
import online.taxcore.pos.utils.JsonFileManager
import java.io.File
import java.util.*

class CatalogDashFragment : Fragment() {

    private val EXPORT_CATALOG_JSON = 0
    private val EXPORT_CATALOG_CSV = 1
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_OK and EXPORT_CATALOG_JSON -> {
                data?.let { intent ->
                    exportJsonCatalog(intent.data!!)
                }
            }
            RESULT_OK and EXPORT_CATALOG_CSV -> {
                data?.let { intent ->
                    exportCsvCatalog(intent.data!!)
                }
            }
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
            attemptCatalogExport()
        }

        catalogImportButton.setOnClickListener {
            attemptCatalogImport()
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

    private fun startCatalogExport() {
        val inflater = (activity as CatalogActivity).layoutInflater
        val mView = inflater.inflate(R.layout.custom_export_catalog_dialog, null)
        val editTextForDialog = mView.findViewById<TextInputEditText>(R.id.et_dialog_catalog)
        val spinner: Spinner = mView.findViewById(R.id.spinner_export_format)

        spinner.adapter = arrayAdapter

        val alertDialog = AlertDialog.Builder(context)
            .setTitle(getString(R.string.title_export_catalog))
            .setView(mView)
            .setPositiveButton(
                getString(R.string.title_export)
            ) { _, _ ->
                createFileInStorage(
                    editTextForDialog.text.toString(),
                    spinner.selectedItem.toString().toLowerCase(Locale.ROOT)
                )
            }
            .setNegativeButton(
                getString(R.string.btn_close)
            ) { dialog, _ ->
                dialog.dismiss()
            }.create()
        alertDialog.show()

        val exportBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        exportBtn.isEnabled = false

        editTextForDialog.onTextChanged {
            exportBtn.isEnabled = it.isNotEmpty()
        }
    }

    private fun createFileInStorage(fileName: String, extension: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, "$fileName.$extension")
        }
        if (extension == "json") {
            startActivityForResult(intent, EXPORT_CATALOG_JSON)
        } else if (extension == "csv") {
            startActivityForResult(intent, EXPORT_CATALOG_CSV)
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
                    showCatalogImportConfirmDialog()
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

    private fun exportJsonCatalog(data: Uri) {

        val catalogItems = CatalogManager.loadCatalogItems()
        JsonFileManager.exportCatalog(context, data, catalogItems,
            onSuccess = {
                runOnUiThread {
                    toast("Catalog exported successfully")
                }
            },
            onError = { error ->
                runOnUiThread {
                    toast(error)
                }
            }
        )
    }

    private fun exportCsvCatalog(data: Uri) {
        val catalogItems = CatalogManager.loadCatalogItems()
        CsvFileManager.exportCatalog(context, data, catalogItems,
            onSuccess = {
                runOnUiThread {
                    toast("Catalog exported successfully")
                }
            },
            onError = { error ->
                runOnUiThread {
                    toast(error)
                }
            }
        )
    }

    private fun showCatalogImportConfirmDialog() {
        val listIsEmpty = CatalogManager.hasCatalogItems().not()

        if (listIsEmpty) {
            startCatalogImport()
            return
        }

        MaterialDialog(requireContext()).show {
            title(R.string.dialog_title_important)
            message(R.string.dialog_message_confirm_catalog_import)
            positiveButton(R.string.dialog_button_import) {
                startCatalogImport()
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
            message(text = "Select .json or .csv file for import")
            fileChooser(filter = jsonOrCsvFilter) { _, file ->

                if (file.length() > 0) {
                    this.dismiss()
                    importCatalogFrom(file)
                    return@fileChooser
                }

                longToast(R.string.toast_file_is_empty)
            }
            positiveButton(text = "Import catalog")
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

        when (file.extension.toLowerCase(Locale.ROOT)) {
            "csv" -> CsvFileManager.importCatalog(file, context,
                onSuccess = { catalogList ->
                    handleCatalogImport(catalogList, loadingDialog)
                },
                onError = { error ->
                    runOnUiThread {
                        loadingDialog.dismiss()
                        toast(error)
                    }
                })
            "json" -> JsonFileManager.importCatalog(file, context,
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
                longToast("Nothing to import")
                return@runOnUiThread
            }

            loadingDialog.dismiss()

            longToast(R.string.toast_catalog_imported)

            // Update UI
            setDashboardButtons()
        }
    }
}

