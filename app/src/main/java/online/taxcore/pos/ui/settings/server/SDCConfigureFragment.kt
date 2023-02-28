package online.taxcore.pos.ui.settings.server

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.gson.Gson
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.runOnUiThread
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.base_details_activity.*
import kotlinx.android.synthetic.main.dialog_loading.*
import kotlinx.android.synthetic.main.dropdown_item.view.*
import kotlinx.android.synthetic.main.sdc_configure_fragment.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.api.APIClient
import online.taxcore.pos.constants.ApiConstants
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data_managers.TaxesManager
import online.taxcore.pos.extensions.*
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.models.AttentionResponse
import online.taxcore.pos.models.ConfigurationResponse
import online.taxcore.pos.models.EnvironmentResponse
import online.taxcore.pos.models.ErrorResponse
import online.taxcore.pos.params.AttentionParams
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.utils.IPAddressFilter
import online.taxcore.pos.utils.IPAddressFilterInterface
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.inject.Inject

class SDCConfigureFragment : Fragment(R.layout.sdc_configure_fragment), IPAddressFilterInterface {

    private var chosenProtocol = ""
    private var isDevModeOff = true
    private lateinit var menu: Menu
    private var arrayAdapterEnvironmentSpinner: ArrayAdapter<String>? = null
    private var isCustomChosen = false
    private var chosenEnvironment: EnvironmentResponse? = null
    var environmentNames = listOf<String>()

    @Inject
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu
        inflater.inflate(R.menu.dev_mode_menu, menu)

        if (pref.getBoolean(PrefConstants.DEV_MODE, false)) {
            turnOnDevMode()
        } else {
            turnOffDevMode()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionDevMode -> {
                if (isDevModeOff) {
                    turnOnDevMode()
                } else {
                    turnOffDevMode()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsDetailsActivity).baseToolbar.title = "ESDC server settings"

        fetchEnvironments()
        setupProtocolSpinner()

        esdcServerNameInput.onTextChanged {
            validateFormFields()
        }

        setupIpAddressInputListeners()

        esdcPortInput.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
        }

        setupButtonListeners()

        populateExistingConfiguration()
    }

    private fun setupIpAddressInputListeners() {
        ipAddressPart1Input.filters = arrayOf(IPAddressFilter(0, 255, this))
        ipAddressPart2Input.filters = arrayOf(IPAddressFilter(0, 255, this))
        ipAddressPart3Input.filters = arrayOf(IPAddressFilter(0, 255, this))
        ipAddressPart4Input.filters = arrayOf(IPAddressFilter(0, 255, this))

        ipAddressPart1Input.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
            if (it.length == 3) {
                ipAddressPart2Input.requestFocus()
            }
        }

        ipAddressPart1Input.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ipAddressPart2Input.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        ipAddressPart2Input.onTextChanged {
            validateAddressFromFields()
            if (it.length == 3) {
                ipAddressPart3Input.requestFocus()
            } else if (it.isEmpty()) {
                ipAddressPart1Input.requestFocus()
            }
        }

        ipAddressPart2Input.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ipAddressPart3Input.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        ipAddressPart3Input.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
            if (it.length == 3) {
                ipAddressPart4Input.requestFocus()
            } else if (it.isEmpty()) {
                ipAddressPart2Input.requestFocus()
            }
        }

        ipAddressPart3Input.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ipAddressPart4Input.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        ipAddressPart4Input.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
            if (it.isEmpty()) {
                ipAddressPart3Input.requestFocus()
            }
        }
    }

    private fun setupProtocolSpinner() {
        esdcProtocolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                chosenProtocol = parent?.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }

        context?.let { context ->
            val protocolOptions = arrayListOf("HTTP", "HTTPS")
            val arrayAdapter =
                object : ArrayAdapter<String>(context, R.layout.dropdown_item, protocolOptions) {
                    override fun isEnabled(position: Int): Boolean {
                        return position != 1
                    }

                    override fun areAllItemsEnabled(): Boolean {
                        return false
                    }

                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        view.setPadding(10, 10, view.paddingRight, 10)
                        return view
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        val textView = view.text_view
                        if (position == 1) {
                            textView.setTextColor(ContextCompat.getColor(context, R.color.disabled))
                        } else {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.colorBlack
                                )
                            )
                        }
                        return view
                    }
                }

            esdcProtocolSpinner.adapter = arrayAdapter
        }

    }

    private fun setupButtonListeners() {
        pingEsdcEndpointButton.setOnClickListener {
            pingSdcConfiguration()
        }

        saveESDCConfigurationButton.setOnClickListener {
            saveSdcConfiguration()
        }
    }

    private fun pingSdcConfiguration() {
        val loadingDialog = createLoadingDialog(R.string.loading_please_wait)
        val address =
            "$chosenProtocol://${ipAddressPart1Input.text}.${ipAddressPart2Input.text}.${ipAddressPart3Input.text}.${ipAddressPart4Input.text}:${esdcPortInput.text}"

        val esdcEndpoint = address.toLowerCase(Locale.getDefault())
        val esdcService = APIClient.esdc(esdcEndpoint)

        val params = AttentionParams()
        val pingRequest = esdcService?.getAttention(params)

        if (pingRequest == null) {
            runOnUiThread {
                longToast(R.string.server_unreachable)
                loadingDialog.dismiss()
            }
        }

        pingRequest?.enqueue(object : Callback<AttentionResponse> {
            override fun onFailure(call: Call<AttentionResponse>?, t: Throwable?) {
                loadingDialog.dismiss()
                longToast(R.string.server_unreachable)
            }

            override fun onResponse(
                call: Call<AttentionResponse>?,
                response: Response<AttentionResponse>?
            ) {
                response?.let { res ->
                    if (res.isSuccessful.not()) {
                        loadingDialog.dismiss()
                        val errorMessage = res.errorBody()?.string()
                        errorMessage?.let { err -> showErrorMessage(err) }
                        return@let
                    }

                    res.body()?.let { response ->
                        loadingDialog.dismiss()
                        if (response.ATT_GSC.contains("0000")) {
                            longToast(R.string.all_ok)
                        } else {
                            longToast(R.string.server_unreachable)
                        }
                    }
                }
            }
        })
    }

    private fun saveSdcConfiguration() {
        pref.edit().putString(PrefConstants.ESDC_SERVER_NAME, esdcServerNameInput.text.toString())
            .apply()

        val ipAddress = arrayOf(
            ipAddressPart1Input.text,
            ipAddressPart2Input.text,
            ipAddressPart3Input.text,
            ipAddressPart4Input.text
        ).joinToString(".") { it.toString().trim() }
        val port = esdcPortInput.text.toString()
        val address = "$chosenProtocol://$ipAddress:$port".toLowerCase(Locale.ROOT)

        pref.edit().putString(PrefConstants.ESDC_ENDPOINT_URL, address).apply()

        when (isCustomChosen) {
            true -> {
                pref.edit().apply {
                    putBoolean(PrefConstants.IS_CUSTOM_CHOSEN, true)
                    putString(PrefConstants.ENVIRONMENT_NAME, "Custom")
                    putString(
                        PrefConstants.ENVIRONMENT_ROOT_URL,
                        customEndpointUrlInput.text.toString()
                    )
                    putString(
                        PrefConstants.CUSTOM_ENVIRONMENT_URL,
                        customEndpointUrlInput.text.toString()
                    )
                }.apply()
            }
            else -> {
                pref.edit().apply {
                    putString(PrefConstants.ENVIRONMENT_ROOT_URL, chosenEnvironment?.Url)
                    putString(PrefConstants.ENVIRONMENT_NAME, chosenEnvironment?.Name)
                }.apply()
            }
        }

        fetchServerConfiguration()
    }

    private fun showErrorMessage(errorMessage: String) {
        val stringWithoutBrackets = errorMessage.removeBrackets()
        AlertDialogHelper.showSimpleAlertDialog(activity, stringWithoutBrackets.showErrorMessage())
    }

    private fun validateFormFields() {
        val isAllFilled = checkRequiredFields(
            esdcServerNameInput,
            ipAddressPart1Input,
            ipAddressPart2Input,
            ipAddressPart3Input,
            ipAddressPart4Input,
            esdcPortInput
        )
        saveESDCConfigurationButton.isEnabled =
            isAllFilled && chosenProtocol.isNotBlank() && if (!isDevModeOff && isCustomChosen) checkRequiredFields(
                customEndpointUrlInput
            ) else true
    }

    private fun validateAddressFromFields() {
        val isButtonEnabled = checkRequiredFields(
            ipAddressPart1Input,
            ipAddressPart2Input,
            ipAddressPart3Input,
            ipAddressPart4Input,
            esdcPortInput
        )
        pingEsdcEndpointButton.isEnabled = isButtonEnabled
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).show {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }

    private fun populateExistingConfiguration() {
        populateEnvironmentData()
        populateIpAddressData()

        validateFormFields()
        validateAddressFromFields()
    }

    private fun populateEnvironmentData() {
        customEndpointUrlInput.onTextChanged {
            validateFormFields()
        }

        // ENVIRONMENT
        val savedEnvironmentName = pref.getString(PrefConstants.ENVIRONMENT_NAME, "") ?: ""
        environmentNames.forEach {
            if (it.startsWith(savedEnvironmentName)) {
                selectEnvironmentSpinner.setSelection(environmentNames.indexOf(it))
            }
        }

        if (savedEnvironmentName == "Custom") {
            customEndpointUrlInput.visibility = View.VISIBLE

            val prevCustomUrl = pref.getString(PrefConstants.CUSTOM_ENVIRONMENT_URL, "")
            val currentCustomUrl = pref.getString(PrefConstants.ENVIRONMENT_ROOT_URL, prevCustomUrl)

            customEndpointUrlInput.setText(currentCustomUrl)
        }
    }

    private fun populateIpAddressData() {
        // SERVER NAME
        val currentServerName = pref.getString(PrefConstants.ESDC_SERVER_NAME, "")
        esdcServerNameInput.setText(currentServerName)

        // IP ADDRESS
        val serverAddress = pref.getString(PrefConstants.ESDC_ENDPOINT_URL, "")
        if (!serverAddress.isNullOrBlank()) {
            chosenProtocol = serverAddress.split("://")[0].toUpperCase(Locale.ROOT)
            val ip_part1 = serverAddress.substringAfter("://").substringBefore(".")
            val ip_part2 = serverAddress.substringAfter(".")
            val ip_part3 = ip_part2.substringAfter(".")
            val ip_part4 = ip_part3.substringAfter(".")

            ipAddressPart1Input.setText(ip_part1)
            ipAddressPart2Input.setText(ip_part2.split(".")[0])
            ipAddressPart3Input.setText(ip_part3.split(".")[0])
            ipAddressPart4Input.setText(ip_part4.split(":")[0])
            esdcPortInput.setText(ip_part4.substringAfter(":").substringBefore("/"))
        }
    }

    override fun shouldPassToNextEditText(symbol: String) {
        val indexOfLastNumber = symbol.length - 1
        when {
            ipAddressPart1Input.hasFocus() -> {
                ipAddressPart2Input.requestFocus()
                ipAddressPart2Input.setText(symbol[indexOfLastNumber].toString())

                val inputTextLength = ipAddressPart2Input.text.toString().length
                ipAddressPart2Input.setSelection(inputTextLength)
            }

            ipAddressPart2Input.hasFocus() -> {
                ipAddressPart3Input.requestFocus()
                ipAddressPart3Input.setText(symbol[indexOfLastNumber].toString())

                val inputTextLength = ipAddressPart3Input.text.toString().length
                ipAddressPart3Input.setSelection(inputTextLength)
            }

            ipAddressPart3Input.hasFocus() -> {
                ipAddressPart4Input.requestFocus()
                ipAddressPart4Input.setText(symbol[indexOfLastNumber].toString())

                val inputTextLength = ipAddressPart4Input.text.toString().length
                ipAddressPart4Input.setSelection(inputTextLength)
            }
        }
    }

    private fun turnOnDevMode() {
        menu.getItem(0).setIcon(R.drawable.ic_dev_mode_on)
        pref.edit().putBoolean(PrefConstants.DEV_MODE, true).apply()
        arrayAdapterEnvironmentSpinner?.add("Custom")
        arrayAdapterEnvironmentSpinner?.getPosition("Custom")
            ?.let { selectEnvironmentSpinner.setSelection(it) }
        isDevModeOff = false
    }

    private fun turnOffDevMode() {
        menu.getItem(0).setIcon(R.drawable.ic_dev_mode_off)
        pref.edit().putBoolean(PrefConstants.DEV_MODE, false).apply()
        arrayAdapterEnvironmentSpinner?.remove("Custom")
        isDevModeOff = true
    }

    private fun fetchEnvironments() {
        val loadingDialog = createLoadingDialog(R.string.loading_please_wait)

        val apiServer = APIClient.esdc(ApiConstants.ENV_SETUP_URL)
        val call = apiServer?.getConfigurationEnvironments()

        call?.enqueue(onFetchEnvironmentCallback(loadingDialog))
    }

    private fun onFetchEnvironmentCallback(loadingDialog: MaterialDialog): Callback<ArrayList<EnvironmentResponse>> {
        return object : Callback<ArrayList<EnvironmentResponse>> {
            override fun onFailure(call: Call<ArrayList<EnvironmentResponse>>?, t: Throwable?) {
                loadingDialog.dismiss()
                t?.message?.let { longToast(it) }
            }

            override fun onResponse(
                call: Call<ArrayList<EnvironmentResponse>>?,
                response: Response<ArrayList<EnvironmentResponse>>?
            ) {
                loadingDialog.dismiss()
                response?.let {
                    if (it.isSuccessful) {
                        it.body()?.let { environmentsList ->
                            setupEnvironmentSpinner(environmentsList)
                        }
                        return@let
                    }
                }
            }
        }

    }

    private fun setupEnvironmentSpinner(environmentOptions: ArrayList<EnvironmentResponse>) {
        environmentNames = environmentOptions.map { "${it.Name} - ${it.Url}" }

        if (isCustomChosen) {
            (environmentNames as ArrayList<String>).add("Custom")
        }

        selectEnvironmentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (parent?.adapter?.getItem(position) == "Custom") { //selected Custom from dropdown
                        val prevCustomUrl = pref.getString(PrefConstants.CUSTOM_ENVIRONMENT_URL, "")
                        customEndpointInputLayout.visibility = View.VISIBLE
                        if (customEndpointUrlInput.text.toString().isEmpty()) {
                            customEndpointUrlInput.setText(prevCustomUrl)
                        }
                        isCustomChosen = true
                    } else {
                        customEndpointInputLayout.visibility = View.GONE
                        isCustomChosen = false
                        chosenEnvironment = environmentOptions[position]
                    }

                    validateFormFields()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        context?.let {
            arrayAdapterEnvironmentSpinner =
                object : ArrayAdapter<String>(it, R.layout.dropdown_item, environmentNames) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        view.setPadding(10, 10, view.paddingRight, 10)
                        return view
                    }
                }
            selectEnvironmentSpinner.adapter = arrayAdapterEnvironmentSpinner
        }

        if (!isDevModeOff) {
            arrayAdapterEnvironmentSpinner?.add("Custom")
        } else {
            arrayAdapterEnvironmentSpinner?.remove("Custom")
        }

        populateExistingConfiguration()
    }

    private fun fetchServerConfiguration() {
        val loadingDialog = createLoadingDialog(R.string.loading_please_wait)
        val configurationAddress = if (isCustomChosen.not()) {
            "https://api.${chosenEnvironment?.Url}/"
        } else {
            val rootEndpoint = customEndpointUrlInput.text.toString()
            "https://api.$rootEndpoint/"
        }

        val apiServer = APIClient.esdc(configurationAddress)
        val call = apiServer?.getConfiguration()

        call?.enqueue(onFetchConfigurationCallback(loadingDialog))
    }

    private fun onFetchConfigurationCallback(loadingDialog: MaterialDialog): Callback<ConfigurationResponse> {
        return object : Callback<ConfigurationResponse> {
            override fun onFailure(call: Call<ConfigurationResponse>?, t: Throwable?) {
                loadingDialog.dismiss()
                t?.message?.let { longToast(it) }
            }

            override fun onResponse(
                call: Call<ConfigurationResponse>?,
                response: Response<ConfigurationResponse>?
            ) {

                response?.let {

                    when {
                        it.isSuccessful.not() -> {
                            loadingDialog.dismiss()
                            var errorMsg = "Unable to save configuration"
                            try {
                                val errorJson = it.errorBody()?.string()
                                val errorResponse =
                                    Gson().fromJson(errorJson, ErrorResponse::class.java)
                                errorResponse?.message?.let { msg ->
                                    errorMsg = msg
                                }
                            } finally {
                                // do nothing
                            }

                            showErrorMessage(errorMsg)

                            return@let
                        }

                        it.isSuccessful -> {

                            AppSession.isAppConfigured = true
                            AppSession.shouldAskForConfiguration = false

                            pref.edit().apply {
                                putString(PrefConstants.LOGO, it.body()?.Logo)
                                putString(PrefConstants.COUNTRY, it.body()?.Country)
                                putBoolean(PrefConstants.IS_APP_CONFIGURED, true)
                            }.apply()


                            val activeTaxItems = it.body()?.getTaxItems()
                            activeTaxItems?.let { taxItems ->
                                TaxesManager.replaceActiveTaxItems(taxItems)
                            }

                            loadingDialog.dismiss()

                            activity?.onBackPressed()
                            return@let
                        }
                    }

                }
            }
        }
    }
}
