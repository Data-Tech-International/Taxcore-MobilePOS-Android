package online.taxcore.pos.ui.settings.server

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.runOnUiThread
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_loading.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.api.APIClient
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data_managers.PrefManager
import online.taxcore.pos.data_managers.TaxesManager
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.models.ConfigurationResponse
import online.taxcore.pos.models.TaxRateResponse
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.utils.TCUtil
import online.taxcore.pos.utils.isOffline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.cert.X509Certificate
import javax.inject.Inject

class SDCServerFragment : PreferenceFragmentCompat(),
    PreferenceManager.OnPreferenceTreeClickListener, KeyChainAliasCallback {

    @Inject
    lateinit var pref: SharedPreferences

    private lateinit var esdcServerName: Preference
    private lateinit var esdcBaseUrl: Preference
    private lateinit var chosenEnvironment: Preference
    private var switchPreferenceCompat: SwitchPreferenceCompat? = null
    private lateinit var configurePreference: Preference
    private lateinit var configDialog: MaterialDialog

    lateinit var vsdcBaseUrl: EditTextPreference

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sdc_settings, rootKey)

        switchPreferenceCompat = findPreference("use_vsdc_server")

        switchPreferenceCompat?.setOnPreferenceChangeListener { _, newValue ->

            activity?.let {
                if (it.isOffline()) {
                    AlertDialogHelper.showNotInternetDialog(
                        it,
                        messageText = R.string.error_require_internet_message
                    )
                    return@setOnPreferenceChangeListener false
                }
            }

            val isAlreadyUsingVSDC = PrefManager.useVSDCServer(pref)
            val isAppConfigured = PrefManager.isAppConfigured(pref)

            if (isAlreadyUsingVSDC && isAppConfigured) {
                AlertDialog.Builder(context)
                    .setTitle(context?.getString(R.string.warning))
                    .setMessage(context?.getString(R.string.switching_will_reset_app))
                    .setPositiveButton(
                        context?.getString(R.string.i_am_sure)
                    ) { dialog, id ->
                        pref.edit().apply {
                            putString(PrefConstants.VSDC_ENDPOINT_URL, "").apply()
                            putString(PrefConstants.TIN_OID, "").apply()
                            putBoolean(PrefConstants.IS_APP_CONFIGURED, false).apply()
                        }.apply()

                        resetAppSettings(newValue)

                        switchPreferenceCompat?.isChecked = false

                        dialog.dismiss()
                    }.setNegativeButton(
                        context?.getString(R.string.cancel)
                    )
                    { dialog, _ ->
                        switchPreferenceCompat?.isChecked = true
                        dialog.dismiss()
                    }.create().show()
                false
            } else {
                resetAppSettings(newValue)
                true
            }
        }

        findPreference<Preference>("esdc_server_name")?.let {
            esdcServerName = it
        }

        findPreference<Preference>("esdc_base_url")?.let {
            esdcBaseUrl = it
        }

        findPreference<Preference>("Configure")?.let {
            configurePreference = it
        }

        findPreference<EditTextPreference>("vsdc_base_url")?.let {
            vsdcBaseUrl = it
        }

        findPreference<Preference>("environment_name")?.let {
            chosenEnvironment = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (PrefManager.useESDCServer(pref)) {
            switchPreferenceCompat?.isChecked = false
        } else if (PrefManager.useVSDCServer(pref)) {
            switchPreferenceCompat?.isChecked = true
        }

        esdcServerName.summary =
            pref.getString(PrefConstants.ESDC_SERVER_NAME, getString(R.string.esdc_not_configured))
        esdcBaseUrl.summary =
            pref.getString(PrefConstants.ESDC_ENDPOINT_URL, getString(R.string.esdc_not_configured))


        val envName = pref.getString(PrefConstants.ENVIRONMENT_NAME, "")
        val envUrl = pref.getString(
            PrefConstants.ENVIRONMENT_ROOT_URL,
            getString(R.string.esdc_not_configured)
        )

        chosenEnvironment.summary = if (envName.isNullOrBlank()) {
            getString(R.string.esdc_not_configured)
        } else {
            "$envName - $envUrl"
        }

        enableServerNameAndServerAddress()

    }

    private fun enableServerNameAndServerAddress() {
        if (PrefManager.useESDCServer(pref)) {
            esdcServerName.isEnabled = true
            chosenEnvironment.isEnabled = true
            esdcBaseUrl.isEnabled = true
            configurePreference.isEnabled = true
            vsdcBaseUrl.shouldDisableView = true
            vsdcBaseUrl.text = pref.getString(PrefConstants.VSDC_ENDPOINT_URL, "")
        } else {
            esdcServerName.isEnabled = false
            chosenEnvironment.isEnabled = false
            esdcBaseUrl.isEnabled = false
            configurePreference.isEnabled = false
            vsdcBaseUrl.shouldDisableView = false
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key != "Configure") {
            return false
        }

        with((activity as SettingsDetailsActivity)) {
            return if (this.isOffline()) {
                AlertDialogHelper.showNotInternetDialog(
                    this,
                    messageText = R.string.error_require_internet_message
                )
                false
            } else {
                replaceFragment(R.id.baseFragment, SDCConfigureFragment(), true)
                true
            }
        }
    }

    override fun alias(alias: String?) {
        runOnUiThread {
            context?.let {
                configDialog =
                    MaterialDialog(it).show {
                        customView(R.layout.dialog_loading).loadingDialogText.text =
                            getString(R.string.text_loading_settings)

                        cancelable(false)  // calls setCancelable on the underlying dialog
                        cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
                    }
            }
        }

        if (alias.isNullOrBlank()) {
            runOnUiThread {
                configDialog.dismiss()
                AppSession.isAppConfigured = false
                pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, false).apply()
                pref.edit().putString(PrefConstants.LOGO, "").apply()
                pref.edit().putString(PrefConstants.COUNTRY, "").apply()
            }
            return
        }

        setServerSettings(alias)
    }


    private fun setServerSettings(alias: String) {
        context?.let {
            val certChain = KeyChain.getCertificateChain(it, alias)

            certChain ?: return

            val (myCert) = certChain

            fetchConfig(alias, myCert)
        }
    }

    private fun fetchConfig(alias: String, cert: X509Certificate) {
        val vsdcEndpoint = TCUtil.getVSDCEndpoint(cert)
        val apiServer = APIClient.vsdc(vsdcEndpoint, alias)

        apiServer?.getTaxes()?.enqueue(object : Callback<TaxRateResponse> {
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

                    AppSession.let {
                        it.shouldAskForConfiguration = false
                        it.isAppConfigured = true
                        it.resetSession()
                    }

                    pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, true).apply()

                    PrefManager.saveCertificateData(pref, cert, alias)

                    vsdcBaseUrl.text = pref.getString(PrefConstants.VSDC_ENDPOINT_URL, "")

                    val certOid = PrefManager.loadCertData(pref).tinOid
                    val taxes = taxRateResponse.getTaxLabels(certOid) ?: arrayListOf()

                    // Remove previous taxes from DB
                    TaxesManager.replaceActiveTaxItems(taxes)
                }

                configDialog.dismiss()
            }
        })
    }

    private fun openSelectCertificateDialog() {
        context?.let {
            if (it.isOffline()) {
                AlertDialogHelper.showNotInternetDialog(
                    (activity as SettingsDetailsActivity),
                    messageText = R.string.error_require_internet_message
                )
                return
            }
        }
        KeyChain.choosePrivateKeyAlias(
            (activity as SettingsDetailsActivity),
            this,
            arrayOf("ICA", "RSA", "RCA"),
            null,
            null,
            -1,
            null
        )
    }

    private fun resetAppSettings(newValue: Any) {
        TaxesManager.removeAllTaxes()

        val useVSDC = newValue as Boolean

        pref.edit().apply {
            putBoolean(PrefConstants.USE_ESDC_SERVER, !useVSDC).apply()
            putBoolean(PrefConstants.USE_VSDC_SERVER, useVSDC).apply()
        }.apply()


        if (useVSDC) {
            openSelectCertificateDialog()
        }

        enableServerNameAndServerAddress()

        val esdcEnabled = useVSDC.not()
        val esdcConfigured = PrefManager.isESDCServerConfigured(pref)

        when {
            esdcEnabled and esdcConfigured.not() -> {
                pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, false).apply()

                AppSession.isAppConfigured = false

                replaceFragment(R.id.baseFragment, SDCConfigureFragment(), true)
            }
            esdcEnabled and esdcConfigured -> {
                pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, true).apply()

                AppSession.isAppConfigured = true

                fetchSDCConfiguration()
            }
            else -> {
                pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, false).apply()

                AppSession.isAppConfigured = false
            }
        }

    }

    private fun fetchSDCConfiguration() {
        val loadingDialog = createLoadingDialog(R.string.loading_please_wait)
        val chosenEnvironmentRootUrl = pref.getString(PrefConstants.ENVIRONMENT_ROOT_URL, "")
        val configurationAddress = "https://api.$chosenEnvironmentRootUrl/"

        val apiServer = APIClient.esdc(configurationAddress)
        val call = apiServer?.getConfiguration()

        call?.enqueue(object : Callback<ConfigurationResponse> {
            override fun onFailure(call: Call<ConfigurationResponse>?, t: Throwable?) {
                loadingDialog.dismiss()
                t?.message?.let { longToast(it) }
            }

            override fun onResponse(
                call: Call<ConfigurationResponse>?,
                response: Response<ConfigurationResponse>?
            ) {
                loadingDialog.dismiss()
                response?.let {
                    if (it.isSuccessful) {

                        pref.edit().putString(PrefConstants.LOGO, it.body()?.Logo).apply()
                        pref.edit().putString(PrefConstants.COUNTRY, it.body()?.Country).apply()

                        AppSession.isAppConfigured = true
                        AppSession.shouldAskForConfiguration = false

                        pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, true).apply()

                        val activeTaxItems = it.body()?.getTaxItems()
                        activeTaxItems?.let { taxItems ->
                            TaxesManager.replaceActiveTaxItems(taxItems)
                        }

                        longToast("You have changed the configuration successfully")

                        return@let
                    }
                }
            }
        })
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).show {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }
}
