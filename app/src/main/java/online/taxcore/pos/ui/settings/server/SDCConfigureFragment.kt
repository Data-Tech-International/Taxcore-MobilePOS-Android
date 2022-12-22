package online.taxcore.pos.ui.settings.server

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.pawegio.kandroid.longToast
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.base_details_activity.*
import kotlinx.android.synthetic.main.dialog_loading.*
import kotlinx.android.synthetic.main.dropdown_item.view.*
import kotlinx.android.synthetic.main.sdc_configure_fragment.*
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.services.SdcService
import online.taxcore.pos.extensions.checkRequiredFields
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.utils.IPAddressFilter
import online.taxcore.pos.utils.IPAddressFilterInterface
import java.util.*
import javax.inject.Inject

class SDCConfigureFragment : Fragment(R.layout.sdc_configure_fragment), IPAddressFilterInterface {

    private var chosenProtocol = ""

    @Inject
    lateinit var prefService: PrefService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsDetailsActivity).baseToolbar.title =
            getString(R.string.esdc_server_title)

        setupProtocolSpinner()

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
                ipAddressPart3Input.requestFocus();
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
                ipAddressPart4Input.requestFocus();
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

    private fun pingSdcConfiguration() {
        val loadingDialog = createLoadingDialog(R.string.loading_please_wait)
        val esdcEndpoint = generateServerEndpoint()

        SdcService.pingEsdcServer(
            esdcEndpoint,
            onStart = {
                loadingDialog.show()
            },
            onSuccess = {
                longToast(R.string.esdc_status_0000)
            },
            onError = {
                if (it == null) {
                    longToast(R.string.error_general)
                    return@pingEsdcServer
                }
                longToast(getMessageForStatus(it))
            },
            onEnd = {
                loadingDialog.dismiss()
            }
        )
    }

    private fun saveSdcConfiguration() {
        val address = generateServerEndpoint()

        prefService.saveEsdcEndpoint(address)

        fetchServerConfiguration(address)
    }

    private fun generateServerEndpoint(): String {
        val ipAddress = arrayOf(
            ipAddressPart1Input.text,
            ipAddressPart2Input.text,
            ipAddressPart3Input.text,
            ipAddressPart4Input.text
        ).joinToString(".") { it.toString().trim() }

        val port = esdcPortInput.text.toString()

        return "$chosenProtocol://$ipAddress:$port/".toLowerCase(Locale.ROOT)
    }

    private fun validateFormFields() {
        val isAllFilled = checkRequiredFields(
            ipAddressPart1Input,
            ipAddressPart2Input,
            ipAddressPart3Input,
            ipAddressPart4Input,
            esdcPortInput
        )

        saveESDCConfigurationButton.isEnabled = isAllFilled && chosenProtocol.isNotBlank()
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

    private fun populateExistingConfiguration() {
        populateIpAddressData()

        validateFormFields()
        validateAddressFromFields()
    }

    private fun populateIpAddressData() {
        // IP ADDRESS
        val serverAddress = prefService.loadEsdcEndpoint()
        if (serverAddress.isNotBlank()) {
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

    private fun fetchServerConfiguration(endpoint: String) {
        val loadingDialog = createLoadingDialog(R.string.text_loading_settings)
        SdcService.fetchEsdcConfiguration(endpoint,
            onStart = {
                loadingDialog.show()
            },
            onSuccessEnv = {
                prefService.setUseEsdcServer()
                prefService.saveEnvData(it, true)
            },
            onSuccessStatus = {
                prefService.saveStatusData(it)
                longToast(R.string.toast_configuration_changed)
                activity?.onBackPressed()
            },
            onError = {
                longToast(getMessageForStatus(it))
            },
            onEnd = {
                loadingDialog.dismiss()
            })
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).apply {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }
}
