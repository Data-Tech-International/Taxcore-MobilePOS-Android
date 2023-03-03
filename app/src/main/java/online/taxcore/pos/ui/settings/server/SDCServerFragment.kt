package online.taxcore.pos.ui.settings.server

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.pawegio.kandroid.longToast
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.base_details_activity.*
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
import online.taxcore.pos.data.services.AppService
import online.taxcore.pos.data.services.DownloadService
import online.taxcore.pos.data.services.ErrorType
import online.taxcore.pos.data.services.SdcService
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.utils.isOffline
import java.io.IOException
import java.util.*
import javax.inject.Inject

class SDCServerFragment : PreferenceFragmentCompat(),
    PreferenceManager.OnPreferenceTreeClickListener {

    @Inject
    lateinit var prefService: PrefService

    private lateinit var configurePreference: Preference
    private lateinit var esdcBaseUrlPref: Preference
    private lateinit var esdcEnvNamePref: Preference
    private lateinit var esdcUidPref: Preference
    private lateinit var esdcApiUrlPref: Preference
    private lateinit var vsdcBaseUrl: Preference

    private var switchPreferenceCompat: SwitchPreferenceCompat? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        (activity as SettingsDetailsActivity).baseToolbar.title =
            getString(R.string.add_v_cdc_server)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sdc_settings, rootKey)

        findPreference<SwitchPreferenceCompat>("use_vsdc_server").let {
            switchPreferenceCompat = it
        }

        findPreference<Preference>("vsdc_base_url")?.let {
            vsdcBaseUrl = it
        }

        findPreference<Preference>("esdc_base_url")?.let {
            esdcBaseUrlPref = it
        }

        findPreference<Preference>("esdc_configure")?.let {
            configurePreference = it
        }


        findPreference<Preference>("esdc_env_uid")?.let {
            esdcUidPref = it
        }

        findPreference<Preference>("esdc_env_name")?.let {
            esdcEnvNamePref = it
        }

        findPreference<Preference>("esdc_base_url")?.let {
            esdcBaseUrlPref = it
        }

        findPreference<Preference>("esdc_api_url")?.let {
            esdcApiUrlPref = it
        }

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

            val isAlreadyUsingVSDC = prefService.useVSDCServer()
            val isAppConfigured = prefService.isAppConfigured()

            if (isAlreadyUsingVSDC && isAppConfigured) {
                AlertDialog.Builder(context)
                    .setTitle(context?.getString(R.string.warning))
                    .setMessage(context?.getString(R.string.switching_will_reset_app))
                    .setPositiveButton(
                        context?.getString(R.string.i_am_sure)
                    ) { dialog, id ->
                        prefService.removeConfiguration()

                        resetAppSettings(newValue as Boolean)

                        switchPreferenceCompat?.isChecked = false

                        dialog.dismiss()
                    }.setNegativeButton(
                        context?.getString(R.string.cancel)
                    )
                    { dialog, _ ->
                        prefService.removeConfiguration()
                        switchPreferenceCompat?.isChecked = true
                        dialog.dismiss()
                    }.create().show()
                false
            } else {
                resetAppSettings(newValue as Boolean)
                true
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefService.useESDCServer()) {
            switchPreferenceCompat?.isChecked = false
        } else if (prefService.useVSDCServer()) {
            switchPreferenceCompat?.isChecked = true
        }

        configurePreference.onPreferenceClickListener = configureClickListener

        vsdcBaseUrl.summary = prefService.loadVsdcEndpoint()

        val envData = prefService.loadEnvData()

        esdcUidPref.summary = when {
            envData.uid.isNotEmpty() -> envData.uid
            else -> getString(R.string.esdc_not_configured)
        }

        esdcEnvNamePref.summary = when {
            envData.esdcEnvName.isNotEmpty() -> envData.esdcEnvName
            else -> getString(R.string.esdc_not_configured)
        }

        esdcBaseUrlPref.summary = when {
            envData.esdcEndpoint.isNotEmpty() -> envData.esdcEndpoint
            else -> getString(R.string.esdc_not_configured)
        }

        esdcApiUrlPref.summary = when {
            envData.esdcApiEndpoint.isNotEmpty() -> envData.esdcApiEndpoint
            else -> getString(R.string.esdc_not_configured)
        }

        refreshPrefFields()

    }

    private val configureClickListener = Preference.OnPreferenceClickListener {
        with((activity as SettingsDetailsActivity)) {
            if (this.isOffline()) {
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

    private fun refreshPrefFields() {
        when {
            prefService.useESDCServer() -> {
                configurePreference.isEnabled = true
                esdcEnvNamePref.isEnabled = true
                esdcBaseUrlPref.isEnabled = true
                esdcApiUrlPref.isEnabled = true
                esdcUidPref.isEnabled = true
                vsdcBaseUrl.shouldDisableView = true
            }
            else -> {
                esdcUidPref.isEnabled = false
                esdcBaseUrlPref.isEnabled = false
                esdcApiUrlPref.isEnabled = false
                esdcEnvNamePref.isEnabled = false
                configurePreference.isEnabled = false
                vsdcBaseUrl.shouldDisableView = false
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != "esdc_configure") {
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

    private fun resetAppSettings(useVSDC: Boolean) {

        AppService.resetConfiguration {
            prefService.removeConfiguration()
            prefService.removeActiveCertName()
            prefService.setUseVsdcServer(useVSDC)
        }

        if (useVSDC) {
            val certificates = CertManager.loadCerts()
            if (certificates.isEmpty()) {
                openDownloadCertDialog()
            } else {
                openSelectCertificateDialog()
            }
            return
        }

        refreshPrefFields()

        val esdcEnabled = useVSDC.not()
        val esdcConfigured = prefService.isESDCServerConfigured()

        when {
            esdcEnabled and esdcConfigured.not() -> {
                prefService.setAppConfigured(false)

                AppSession.isAppConfigured = false

                replaceFragment(R.id.baseFragment, SDCConfigureFragment(), true)
            }
            esdcEnabled and esdcConfigured -> {
                fetchEsdcConfiguration()
            }
            else -> {
                AppService.resetConfiguration {
                    prefService.removeConfiguration()
                }
            }
        }
    }

    private fun fetchEsdcConfiguration() {
        val configDialog = createLoadingDialog(R.string.text_loading_settings)
        val endpoint = prefService.loadEsdcEndpoint()

        SdcService.fetchEsdcConfiguration(endpoint,
            onStart = {
                configDialog.show()
            },
            onSuccessEnv = {
                prefService.setUseEsdcServer()
                prefService.saveEnvData(it, true)
            },
            onSuccessStatus = {
                prefService.saveStatusData(it)
                activity?.onBackPressed()
            },
            onError = {
                longToast(getMessageForStatus(it))
            },
            onEnd = {
                configDialog.dismiss()
            })
    }

    private fun fetchConfig(pac: String, clientAuthority: ClientAuthority, certName: String) {
        val configDialog = createLoadingDialog()

        SdcService.fetchVsdcConfiguration(clientAuthority, pac,
            onStart = {
                configDialog.show()
            },
            onSuccessEnv = {
                prefService.setUseVsdcServer()
                prefService.saveEnvData(it)
            },
            onSuccessStatus = {
                try {
                    prefService.setAppConfigured()
                    prefService.savePac(pac)
                    prefService.saveCertificateData(clientAuthority.second)
                    prefService.saveActiveCertName(certName)

                    vsdcBaseUrl.summary = prefService.loadVsdcEndpoint()
                    refreshPrefFields()

                    longToast(R.string.toast_configuration_changed)

                } catch (e: IllegalArgumentException) {
                    resetAppSettings(false)
                    longToast(R.string.error_general)
                }

            },
            onError = {
                longToast(R.string.error_provide_valid_pac)
            },
            onEnd = {
                configDialog.cancel()
            }
        )
    }

    // THIS IS IDENTICAL CODE AS IN DASHBOARD ACTIVITY

    @SuppressLint("CheckResult")
    private fun openSelectCertificateDialog() {
        with((activity as SettingsDetailsActivity)) {
            if (this.isOffline()) {
                AlertDialogHelper.showNotInternetDialog(
                    this,
                    messageText = R.string.error_require_internet_message
                )
                return
            }

        }

        val certificates = CertManager.loadCerts()
        val selectedIndex = certificates.map { it.name }.indexOf(prefService.loadActiveCertName())
        val certListNames = certificates.map { it.displayName() }

        MaterialDialog(requireContext()).show {
            icon(R.drawable.ic_security)
            title(text = getString(R.string.title_select_certificate))

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

            positiveButton(text = getString(R.string.btn_allow))
            neutralButton(text = getString(R.string.add_new_cert)) {
                openDownloadCertDialog()
            }

            negativeButton(R.string.cancel) {
                refreshPrefFields()
                dismiss()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun openDownloadCertDialog() {
        MaterialDialog(requireContext()).show {
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
                val certEndpoint = inputUrl.text.toString()

                downloadCert(certEndpoint)
            }

            negativeButton(R.string.cancel) {
                refreshPrefFields()
                dismiss()
            }
        }
    }

    private fun downloadCert(url: String) {

        val configDialog = createLoadingDialog(R.string.msg_downloading)

        DownloadService.downloadCert(
            url,
            requireActivity().cacheDir,
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

        MaterialDialog(requireContext()).show {
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
        val PAC_INPUT_LENGTH = 8

        MaterialDialog(requireContext()).show {
            title(R.string.title_cert_password)
            customView(R.layout.dialog_cert_pass_layout)
            cancelable(false)

            setActionButtonEnabled(WhichButton.NEUTRAL, getClipboardText().isNotEmpty())
            positiveButton(R.string.paste_and_continue) {
                val clipboardText = getClipboardText()
                getCustomView().certPassInput.setText(clipboardText)
            }

            negativeButton(R.string.cancel) {
                dismiss()
            }

            getCustomView().certPassInput.onTextChanged { inputText ->
                setActionButtonEnabled(WhichButton.POSITIVE, inputText.length == PAC_INPUT_LENGTH)
                if (inputText.length == PAC_INPUT_LENGTH) {
                    val inputPass = this.getCustomView().certPassInput.text.toString()
                        .toUpperCase(Locale.getDefault())
                    callback(inputPass)
                    dismiss()
                }
            }
        }
    }

    private fun getClipboardText(): CharSequence {
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)

        return item?.text?.trim() ?: ""
    }

    // HELPERS

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).apply {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }

    @StringRes
    private fun getMessageForStatus(status: String): Int {
        return when (status) {
            "0000" -> R.string.esdc_status_0000
            "0100" -> R.string.esdc_status_0100
            "1300" -> R.string.esdc_status_1300
            "1500" -> R.string.esdc_status_1500
            "2100" -> R.string.esdc_status_2100
            "2110" -> R.string.esdc_status_2110
            "2400" -> R.string.esdc_status_2400
            else -> R.string.error_general
        }
    }

}
