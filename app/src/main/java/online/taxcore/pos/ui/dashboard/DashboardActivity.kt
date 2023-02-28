package online.taxcore.pos.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
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
import kotlinx.android.synthetic.main.dialog_loading.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.api.APIClient
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data_managers.PrefManager
import online.taxcore.pos.data_managers.TaxesManager
import online.taxcore.pos.enums.InvoiceActivityType
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.models.TaxRateResponse
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.ui.catalog.CatalogActivity
import online.taxcore.pos.ui.invoice.InvoiceActivity
import online.taxcore.pos.ui.journal.JournalActivity
import online.taxcore.pos.ui.settings.SettingsActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_SDC_CONFIGURE
import online.taxcore.pos.utils.TCUtil
import online.taxcore.pos.utils.isOffline
import org.jetbrains.anko.contentView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.cert.X509Certificate
import javax.inject.Inject

class DashboardActivity : BaseActivity(), KeyChainAliasCallback {

    @Inject
    lateinit var pref: SharedPreferences

    private lateinit var configDialog: MaterialDialog
    private var useESDCServer: Boolean = false

    var isAppConfigured: Boolean = false

    companion object {
        fun start(activity: AppCompatActivity, extras: Boolean?) {
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
                    AppSession.resetSession()
                    finishAffinity()
                }
            )
            return
        }

        if (doubleBackToExitPressedOnce) {
            AppSession.resetSession()
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        longToast(getString(R.string.exit_app))

        Handler().postDelayed({
            doubleBackToExitPressedOnce = false
        }, 3000)
    }

    override fun alias(alias: String?) {
        runOnUiThread {
            configDialog = MaterialDialog(this).show {
                customView(R.layout.dialog_loading).loadingDialogText.text =
                    getString(R.string.text_loading_settings)

                cancelable(false)  // calls setCancelable on the underlying dialog
                cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
            }
        }

        if (alias.isNullOrBlank()) {
            runOnUiThread {
                configDialog.dismiss()
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startActivity(KeyChain.createInstallIntent())
            }

            return
        }

        setServerSettings(alias)
    }

    @SuppressLint("SetTextI18n")
    private fun initHeader() {

        val hasCert = PrefManager.hasCertInstalled(pref)
        useESDCServer = PrefManager.useESDCServer(pref)
        isAppConfigured = PrefManager.isAppConfigured(pref)
        val useVSDCServer = PrefManager.useVSDCServer(pref)

        if (isAppConfigured) {
            if (hasCert && useVSDCServer) {
                dashboardHeaderNoConfigLayout.visible = false
                dashboardHeaderCertInfoLayout.visible = true
            } else if (useESDCServer) {
                dashboardHeaderNoConfigLayout.visible = true
                tv_app_not_configured.text = getString(R.string.app_is_configured_with_esdc)
                dashboardHeaderCertInfoLayout.visible = false
            }
        } else {
            dashboardHeaderNoConfigLayout.visible = true
            tv_app_not_configured.text = getString(R.string.not_configured)
            dashboardHeaderCertInfoLayout.visible = false
            return
        }

        val certData = PrefManager.loadCertData(pref)

        dashboardHeaderUIDTextView.text = certData.serialNumber
        dashboardHeaderLocationNameTextView.text = certData.organisationUnit
        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        val country = pref.getString(PrefConstants.COUNTRY, null)

        val logoImage = if (!country.isNullOrBlank()) {
            TCUtil.getCountryImage(country, true)
        } else {
            TCUtil.getEnvLogo(certData.tinOid, true)
        }

        Glide.with(this)
            .load(logoImage)
            .optionalFitCenter()
            .transition(DrawableTransitionOptions.withCrossFade(factory))
            .into(dashboardHeaderImageView)
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
            // Open certificate picker dialog
            if (useESDCServer) {
                openConfigureESDC()
            } else {
                openSelectCertificateDialog()
            }
        }

        dashboardChangeCertButton.setOnClickListener {
            // Open certificate picker dialog
            openSelectCertificateDialog()
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
                if (useESDCServer) {
                    openConfigureESDC()
                } else {
                    openSelectCertificateDialog()
                }
            }

            negativeButton(negativeBtnText) {
                AppSession.shouldAskForConfiguration = false
                it.dismiss()
                onNotNow()
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

    private fun setServerSettings(alias: String) {
        val certChain = KeyChain.getCertificateChain(this, alias)

        certChain ?: return

        val (myCert) = certChain

        fetchConfig(alias, myCert)

    }

    private fun fetchConfig(alias: String, cert: X509Certificate) {
        val vsdcEndpoint = TCUtil.getVSDCEndpoint(cert)
        val vsdcService = APIClient.vsdc(vsdcEndpoint, alias)

        vsdcService?.getTaxes()?.enqueue(object : Callback<TaxRateResponse> {
            override fun onFailure(call: Call<TaxRateResponse>?, t: Throwable?) {
                configDialog.dismiss()
                t?.message?.let { errMsg ->
                    longToast(errMsg)
                }
            }

            override fun onResponse(
                call: Call<TaxRateResponse>?,
                response: Response<TaxRateResponse>?
            ) {
                val res = response ?: return configDialog.dismiss()

                if (res.isSuccessful.not()) {
                    configDialog.dismiss()
                    val errorMessage = res.errorBody()?.string()
                    errorMessage?.let { _ ->
                        val isHtmlError = errorMessage.startsWith("<!DOCTYPE html", true)

                        if (isHtmlError) {
                            val parsedMsg =
                                errorMessage.substringAfter("<h3>").substringBefore("</h3>")
                            longToast(parsedMsg)
                        }
                    }
                    return
                }

                res.body()?.let { taxRateResponse ->

                    // Reset session data
                    AppSession.let {
                        it.shouldAskForConfiguration = false
                        it.isAppConfigured = true
                        it.resetSession()
                    }

                    pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, true).apply()

                    PrefManager.saveCertificateData(pref, cert, alias)

                    val certOid = PrefManager.loadCertData(pref).tinOid

                    val taxItems = taxRateResponse.getTaxLabels(certOid)
                    taxItems?.let {
                        TaxesManager.replaceActiveTaxItems(it)
                    }

                    initHeader()
                }

                configDialog.dismiss()
            }
        })

    }


    private fun openSelectCertificateDialog() {

        if (isOffline()) {
            AlertDialogHelper.showNotInternetDialog(
                this,
                messageText = R.string.error_require_internet_message
            )
            return
        }

//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
//            MaterialDialog(this).show {
//                title(text = "No certificate found")
//                message(text = "You can add a certificate from your local file storage. Would you like to add a certificate? Please add certificate again once your certificate is installed.")
//
//                positiveButton(text = "Add Certificate") {
//                    dismiss()
//                    startActivity(KeyChain.createInstallIntent())
//                }
//            }
//            return
//        }

        KeyChain.choosePrivateKeyAlias(
            this,
            this,
            arrayOf("ICA", "RSA", "RCA"),
            null,
            null,
            -1,
            null
        )
    }

    private fun openConfigureESDC() {
        if (isOffline()) {
            AlertDialogHelper.showNotInternetDialog(
                this,
                messageText = R.string.error_require_internet_message
            )
            return
        }

        SettingsDetailsActivity.start(this, FRAGMENT_SDC_CONFIGURE)
    }

    override fun onResume() {
        super.onResume()
        initHeader()
    }
}
