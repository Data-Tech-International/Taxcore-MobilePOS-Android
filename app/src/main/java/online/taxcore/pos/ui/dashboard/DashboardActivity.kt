package online.taxcore.pos.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pawegio.kandroid.longToast
import kotlinx.android.synthetic.main.dashboard_activity.*
import kotlinx.android.synthetic.main.dialog_cert_pass_layout.view.*
import kotlinx.android.synthetic.main.dialog_loading.*
import kotlinx.android.synthetic.main.dialog_loading.view.*
import kotlinx.android.synthetic.main.dialog_pac_layout.view.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.api.handleCertificates.CertAuthority
import online.taxcore.pos.data.api.handleCertificates.ClientAuthority
import online.taxcore.pos.data.local.CertManager
import online.taxcore.pos.data.realm.Cert
import online.taxcore.pos.data.services.DownloadService
import online.taxcore.pos.data.services.ErrorType
import online.taxcore.pos.data.services.SdcService
import online.taxcore.pos.enums.InvoiceActivityType
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.ui.catalog.CatalogActivity
import online.taxcore.pos.ui.invoice.InvoiceActivity
import online.taxcore.pos.ui.journal.JournalActivity
import online.taxcore.pos.ui.settings.SettingsActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_SDC_CONFIGURE
import online.taxcore.pos.utils.isOffline
import org.jetbrains.anko.contentView
import java.io.*
import java.util.*
import javax.inject.Inject

class DashboardActivity : BaseActivity() {

    @Inject
    lateinit var prefService: PrefService
    private var useESDCServer: Boolean = false

    var isAppConfigured: Boolean = false

    companion object {
        fun start(activity: AppCompatActivity, extras: Boolean? = null) {
            val intent = Intent(activity, DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_NEW_TASK)
                //FIXME: This is questionable
                extras?.let {
                    putExtra("snackbar", it)
                }
            }

            ActivityCompat.startActivity(activity, intent, null)
            activity.finishAffinity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        //FIXME: This is questionable
        val showSnackbar = intent.extras?.getBoolean("snackbar")
        showSnackbar?.let {
            if (it) {
                contentView?.let { view ->
                    Snackbar.make(
                        view,
                        R.string.previously_selected_certificate_not_exist,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        if (isOffline().not() and AppSession.shouldAskForConfiguration) {
            showAppNotConfiguredDialog()
        }

        initHeader()
        setClickListeners()
        askPermission()
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {

        if (!isAppConfigured) {
            showAppNotConfiguredDialog(
                titleText = R.string.title_finish_configuration,
                messageText = R.string.msg_exit_without_configuration,
                negativeBtnText = R.string.btn_close,
                onNotNow = {
                    AppSession.resetSessionCredentials()
                    finishAffinity()
                }
            )
            return
        }

        if (doubleBackToExitPressedOnce) {
            AppSession.resetSessionCredentials()
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        longToast(getString(R.string.exit_app))

        Handler().postDelayed({
            doubleBackToExitPressedOnce = false
        }, 3000)
    }

    override fun onResume() {
        super.onResume()
        initHeader()
    }


    @SuppressLint("SetTextI18n")
    private fun initHeader() {

        val hasCert = prefService.hasCertInstalled()
        useESDCServer = prefService.useESDCServer()
        isAppConfigured = prefService.isAppConfigured()
        val useVSDCServer = prefService.useVSDCServer()

        if (isAppConfigured.not()) {
            dashboardHeaderNoConfigLayout.visible = true
            dashboardHeaderCertInfoLayout.visible = false
            dashboardHeaderEsdcLayout.visible = false
            return
        }

        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        val envLogo = prefService.loadEnvLogo()

        Glide.with(this)
            .load(envLogo)
            .optionalFitCenter()
            .transition(DrawableTransitionOptions.withCrossFade(factory))
            .error(R.drawable.logo_text)
            .into(dashboardHeaderImageView)

        if (hasCert && useVSDCServer) {
            dashboardHeaderNoConfigLayout.visible = false
            dashboardHeaderEsdcLayout.visible = false
            dashboardHeaderCertInfoLayout.visible = true

            val certData = prefService.loadCertData()

            dashboardHeaderUIDTextView.text = certData.serialNumber
            dashboardHeaderLocationNameTextView.text = certData.organisationUnit

        } else if (useESDCServer) {
            dashboardHeaderNoConfigLayout.visible = false
            dashboardHeaderCertInfoLayout.visible = false
            dashboardHeaderEsdcLayout.visible = true

            val envData = prefService.loadEnvData()
            val locationName = prefService.loadLocation(envData.uid)
            dashHeaderEsdcLocationLayout.visible = locationName.isNotEmpty()
            dashHeaderEsdcLocationName.text = locationName

            dashHeaderEsdcUidLabel.text = envData.uid
            dashHeaderEsdcEnvLabel.text = envData.esdcEnvName
        } else {
            dashboardHeaderNoConfigLayout.visible = true
        }
    }

    private fun setClickListeners() {
        invoiceCardButton.setOnClickListener {
            onNewInvoice()
        }

        catalogCardButton.setOnClickListener {
            CatalogActivity.start(this, "")
        }

        journalCardButton.setOnClickListener {
            JournalActivity.start(this)
        }

        settingsCardButton.setOnClickListener {
            SettingsActivity.start(this)
        }

        dashboardConfigureButton.setOnClickListener {
            handleOnConfigureClick()
        }

        dashboardChangeCertButton.setOnClickListener {
            handleOnConfigureClick()
        }
    }

    private fun onNewInvoice() {
        if (isAppConfigured) {
            // SelectItemsActivity.start(this)
            InvoiceActivity.start(this, InvoiceActivityType.NORMAL)
            return
        }

        showAppNotConfiguredDialog()
    }

    private fun showAppNotConfiguredDialog(
        @StringRes titleText: Int = R.string.title_app_not_configured,
        @StringRes messageText: Int = R.string.msg_configure_application,
        @StringRes negativeBtnText: Int = R.string.btn_not_now,
        onNotNow: () -> Unit = { }
    ) {

        MaterialDialog(this).show {
            icon(R.drawable.ic_info)
            title(titleText)
            message(messageText)

            positiveButton(R.string.btn_configure_now) {
                handleOnConfigureClick()
            }

            negativeButton(negativeBtnText) {
                AppSession.shouldAskForConfiguration = false
                it.dismiss()
                onNotNow()
            }
        }
    }

    private fun handleOnConfigureClick() {
        if (isOffline()) {
            AlertDialogHelper.showNotInternetDialog(
                this,
                messageText = R.string.error_require_internet_message
            )
            return
        }

        // Open certificate picker dialog
        if (useESDCServer) {
            SettingsDetailsActivity.start(this, FRAGMENT_SDC_CONFIGURE)
        } else {
            val certList = CertManager.loadCerts()
            when {
                certList.isNotEmpty() -> openSelectCertificateDialog(certList)
                else -> openDownloadCertDialog()
            }

        }
    }

    private fun askPermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {}

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                    longToast(getString(R.string.denied_permission))
                }
            }).check()
    }

    /**
     * CERT HELPERS AND DIALOGS
     * THIS IS IDENTICAL CODE AS IN SDCServerFragment
     */

    private fun fetchConfig(pac: String, clientAuthority: ClientAuthority, certName: String) {
        val configDialog = createLoadingDialog()

        SdcService.fetchVsdcConfiguration(clientAuthority, pac,
            onStart = {
                configDialog.show()
            },
            onSuccessEnv = {
                prefService.saveEnvData(it)
            },
            onSuccessStatus = {

                prefService.setAppConfigured()
                prefService.savePac(pac)
                prefService.saveCertificateData(clientAuthority.second)
                prefService.saveActiveCertName(certName)

                initHeader()

                longToast(R.string.toast_configuration_changed)
            },
            onError = {
                longToast(R.string.error_provide_valid_pac)
            },
            onEnd = {
                configDialog.cancel()
            }
        )
    }

    @SuppressLint("CheckResult")
    private fun openSelectCertificateDialog(certificates: List<Cert>) {

        if (isOffline()) {
            AlertDialogHelper.showNotInternetDialog(
                this,
                messageText = R.string.error_require_internet_message
            )
            return
        }

        MaterialDialog(this).show {
            icon(R.drawable.ic_security)
            title(R.string.title_select_certificate)

            val selectedIndex =
                certificates.map { it.name }.indexOf(prefService.loadActiveCertName())
            val certListNames = certificates.map { it.displayName() }

            listItemsSingleChoice(
                items = certListNames,
                waitForPositiveButton = true,
                initialSelection = selectedIndex
            ) { _, index, _ ->
                // Invoked when the user taps an item
                val activeCert = certificates[index].name
                val cert = CertManager.loadCert(activeCert)
                cert ?: return@listItemsSingleChoice

                val savedCertPass = prefService.loadPfxPass(activeCert)

                if (savedCertPass.isEmpty()) {
                    // There is no password saved for the selected certificate
                    // in that case show again password input dialog
                    showCertPassInputDialog(cert.pfxData, cert.name)
                    return@listItemsSingleChoice
                }

                // Cert is in the database, with password in storage,
                // proceed to PAC input
                showPacInputDialog { pacInput ->
                    val clientAuthority =
                        CertAuthority.certificateParams(cert.pfxData, savedCertPass)

                    fetchConfig(pacInput, clientAuthority, cert.name)
                }
            }

            positiveButton(R.string.btn_allow)
            neutralButton(R.string.add_new_cert) {
                openDownloadCertDialog()
            }

            negativeButton(R.string.cancel) {
                dismiss()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun openDownloadCertDialog() {

        MaterialDialog(this).show {
            title(R.string.title_download_cert)
            input(inputType = InputType.TYPE_TEXT_VARIATION_URI)
            getInputField().onTextChanged { input ->
                setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    input.startsWith("https://") && Patterns.WEB_URL.matcher(input).matches()
                )
            }
            positiveButton(R.string.btn_download) {
                val inputUrl = this.getInputField()
                downloadCert(inputUrl.text.toString())
            }
            negativeButton(R.string.cancel) {
                dismiss()
            }
        }
    }

    private fun downloadCert(url: String) {

        val configDialog = createLoadingDialog(R.string.btn_download)

        DownloadService.downloadCert(
            url,
            cacheDir,
            onStart = {
                configDialog.show()
            },
            onSuccess = { pfx, p12File ->
                configDialog.getCustomView().loadingDialogText.text =
                    getString(R.string.msg_file_downloaded)
                showCertPassInputDialog(pfx, p12File)
            },
            onError = { errorType, _ ->
                when (errorType) {
                    ErrorType.INVALID_OR_USED_LINK -> longToast(R.string.msg_nothing_to_download)
                    ErrorType.NO_CERT_FILE_FOUND -> longToast(R.string.error_no_cert_files_found)
                    else -> longToast(R.string.msg_failed_try_again)
                }
            },
            onEnd = {
                configDialog.cancel()
            }
        )
    }

    private fun showCertPassInputDialog(pfx: String, certName: String) {
        createCertPassDialog { certPass ->
            try {
                // Try to open certificate with provided credentials
                val clientAuthority = CertAuthority.certificateParams(pfx, certPass)

                // save cert password
                prefService.savePfxPass(certName, certPass)
                prefService.saveActiveCertName(certName)

                showPacInputDialog { pacInput ->
                    fetchConfig(pacInput, clientAuthority, certName)
                }
            } catch (e: IOException) {
                longToast(R.string.error_wrong_pass_or_file)
            }
        }
    }

    private fun showPacInputDialog(callback: (pac: String) -> Unit) {
        val PAC_INPUT_LENGTH = 6

        MaterialDialog(this).show {
            title(R.string.title_enter_pac)
            customView(R.layout.dialog_pac_layout)
            cancelable(false)

            setActionButtonEnabled(WhichButton.NEUTRAL, getClipboardText().isNotEmpty())
            neutralButton(R.string.paste_and_continue) {
                val clipboardText = getClipboardText()
                getCustomView().pacInputView.setText(clipboardText)
            }

            negativeButton(R.string.cancel) {
                dismiss()
            }

            getCustomView().pacInputView.onTextChanged { inputText ->
                setActionButtonEnabled(WhichButton.POSITIVE, inputText.length == PAC_INPUT_LENGTH)
                if (inputText.length == PAC_INPUT_LENGTH) {
                    val inputPac = this.getCustomView().pacInputView.text.toString()
                        .toUpperCase(Locale.getDefault())
                    callback(inputPac)
                    dismiss()
                }
            }
        }
    }

    private fun createCertPassDialog(callback: (pac: String) -> Unit) {
        val PASS_INPUT_LENGTH = 8

        MaterialDialog(this).show {
            title(R.string.title_cert_password)
            customView(R.layout.dialog_cert_pass_layout)
            cancelable(false)

            setActionButtonEnabled(WhichButton.NEUTRAL, getClipboardText().isNotEmpty())
            neutralButton(R.string.paste_and_continue) {
                val clipboardText = getClipboardText()
                getCustomView().certPassInput.setText(clipboardText)
            }

            negativeButton(R.string.cancel) {
                dismiss()
            }

            getCustomView().certPassInput.onTextChanged { inputText ->
                setActionButtonEnabled(WhichButton.POSITIVE, inputText.length == PASS_INPUT_LENGTH)
                if (inputText.length == PASS_INPUT_LENGTH) {
                    val inputPass = this.getCustomView().certPassInput.text.toString()
                        .toUpperCase(Locale.getDefault())
                    callback(inputPass)
                    dismiss()
                }
            }
        }
    }

    private fun getClipboardText(): CharSequence {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)

        return item?.text?.trim() ?: ""
    }

/* Helpers */

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(this).apply {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }

}
