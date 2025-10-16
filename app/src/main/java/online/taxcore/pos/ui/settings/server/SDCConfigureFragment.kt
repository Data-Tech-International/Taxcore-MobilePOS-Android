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
import dagger.android.support.AndroidSupportInjection
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.services.SdcService
import online.taxcore.pos.databinding.DialogLoadingBinding
import online.taxcore.pos.databinding.DropdownItemBinding
import online.taxcore.pos.databinding.SdcConfigureFragmentBinding
import online.taxcore.pos.extensions.checkRequiredFields
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.utils.IPAddressFilter
import online.taxcore.pos.utils.IPAddressFilterInterface
import online.taxcore.pos.utils.longToast
import java.util.Locale
import javax.inject.Inject

class SDCConfigureFragment : Fragment(R.layout.sdc_configure_fragment), IPAddressFilterInterface {

    private var _binding: SdcConfigureFragmentBinding? = null
    private val binding get() = _binding!!

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
        _binding = SdcConfigureFragmentBinding.bind(view)

        (activity as SettingsDetailsActivity).binding.baseToolbar.title =
            getString(R.string.esdc_server_title)

        setupProtocolSpinner()

        setupIpAddressInputListeners()

        binding.esdcPortInput.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
        }

        setupButtonListeners()

        populateExistingConfiguration()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupIpAddressInputListeners() {
        binding.ipAddressPart1Input.filters = arrayOf(IPAddressFilter(0, 255, this))
        binding.ipAddressPart2Input.filters = arrayOf(IPAddressFilter(0, 255, this))
        binding.ipAddressPart3Input.filters = arrayOf(IPAddressFilter(0, 255, this))
        binding.ipAddressPart4Input.filters = arrayOf(IPAddressFilter(0, 255, this))

        binding.ipAddressPart1Input.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
            if (it.length == 3) {
                binding.ipAddressPart2Input.requestFocus()
            }
        }

        binding.ipAddressPart1Input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.ipAddressPart2Input.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.ipAddressPart2Input.onTextChanged {
            validateAddressFromFields()
            if (it.length == 3) {
                binding.ipAddressPart3Input.requestFocus();
            } else if (it.isEmpty()) {
                binding.ipAddressPart1Input.requestFocus()
            }
        }

        binding.ipAddressPart2Input.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.ipAddressPart3Input.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.ipAddressPart3Input.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
            if (it.length == 3) {
                binding.ipAddressPart4Input.requestFocus();
            } else if (it.isEmpty()) {
                binding.ipAddressPart2Input.requestFocus()
            }
        }

        binding.ipAddressPart3Input.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.ipAddressPart4Input.requestFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.ipAddressPart4Input.onTextChanged {
            validateFormFields()
            validateAddressFromFields()
            if (it.isEmpty()) {
                binding.ipAddressPart3Input.requestFocus()
            }
        }
    }

    private fun setupProtocolSpinner() {
        binding.esdcProtocolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                        val dropdownBinding = DropdownItemBinding.bind(view)
                        val textView = dropdownBinding.textView
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

            binding.esdcProtocolSpinner.adapter = arrayAdapter
        }

    }

    private fun setupButtonListeners() {
        binding.pingEsdcEndpointButton.setOnClickListener {
            pingSdcConfiguration()
        }

        binding.saveESDCConfigurationButton.setOnClickListener {
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
                longToast(getString(R.string.esdc_status_0000))
            },
            onError = {
                if (it == null) {
                    longToast(getString(R.string.error_general))
                    return@pingEsdcServer
                }
                longToast(getString(getMessageForStatus(it)))
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
            binding.ipAddressPart1Input.text,
            binding.ipAddressPart2Input.text,
            binding.ipAddressPart3Input.text,
            binding.ipAddressPart4Input.text
        ).joinToString(".") { it.toString().trim() }

        val port = binding.esdcPortInput.text.toString()

        return "$chosenProtocol://$ipAddress:$port/".lowercase(Locale.ROOT)
    }

    private fun validateFormFields() {
        val isAllFilled = checkRequiredFields(
            binding.ipAddressPart1Input,
            binding.ipAddressPart2Input,
            binding.ipAddressPart3Input,
            binding.ipAddressPart4Input,
            binding.esdcPortInput
        )

        binding.saveESDCConfigurationButton.isEnabled = isAllFilled && chosenProtocol.isNotBlank()
    }

    private fun validateAddressFromFields() {
        val isButtonEnabled = checkRequiredFields(
            binding.ipAddressPart1Input,
            binding.ipAddressPart2Input,
            binding.ipAddressPart3Input,
            binding.ipAddressPart4Input,
            binding.esdcPortInput
        )
        binding.pingEsdcEndpointButton.isEnabled = isButtonEnabled
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
            chosenProtocol = serverAddress.split("://")[0].uppercase(Locale.ROOT)
            val ip_part1 = serverAddress.substringAfter("://").substringBefore(".")
            val ip_part2 = serverAddress.substringAfter(".")
            val ip_part3 = ip_part2.substringAfter(".")
            val ip_part4 = ip_part3.substringAfter(".")

            binding.ipAddressPart1Input.setText(ip_part1)
            binding.ipAddressPart2Input.setText(ip_part2.split(".")[0])
            binding.ipAddressPart3Input.setText(ip_part3.split(".")[0])
            binding.ipAddressPart4Input.setText(ip_part4.split(":")[0])
            binding.esdcPortInput.setText(ip_part4.substringAfter(":").substringBefore("/"))
        }
    }

    override fun shouldPassToNextEditText(symbol: String) {
        val indexOfLastNumber = symbol.length - 1
        when {
            binding.ipAddressPart1Input.hasFocus() -> {
                binding.ipAddressPart2Input.requestFocus()
                binding.ipAddressPart2Input.setText(symbol[indexOfLastNumber].toString())

                val inputTextLength = binding.ipAddressPart2Input.text.toString().length
                binding.ipAddressPart2Input.setSelection(inputTextLength)
            }

            binding.ipAddressPart2Input.hasFocus() -> {
                binding.ipAddressPart3Input.requestFocus()
                binding.ipAddressPart3Input.setText(symbol[indexOfLastNumber].toString())

                val inputTextLength = binding.ipAddressPart3Input.text.toString().length
                binding.ipAddressPart3Input.setSelection(inputTextLength)
            }

            binding.ipAddressPart3Input.hasFocus() -> {
                binding.ipAddressPart4Input.requestFocus()
                binding.ipAddressPart4Input.setText(symbol[indexOfLastNumber].toString())

                val inputTextLength = binding.ipAddressPart4Input.text.toString().length
                binding.ipAddressPart4Input.setSelection(inputTextLength)
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
                longToast(getString(R.string.toast_configuration_changed))
                activity?.onBackPressed()
            },
            onError = {
                longToast(getString(getMessageForStatus(it)))
            },
            onEnd = {
                loadingDialog.dismiss()
            })
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).apply {
            val dialogBinding = DialogLoadingBinding.inflate(layoutInflater)
            customView(view = dialogBinding.root)
            dialogBinding.loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }
}
