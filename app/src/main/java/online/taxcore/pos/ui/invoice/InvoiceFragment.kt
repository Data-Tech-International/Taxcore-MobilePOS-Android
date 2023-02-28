package online.taxcore.pos.ui.invoice

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.customListAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.runAsync
import com.pawegio.kandroid.runOnUiThread
import com.pawegio.kandroid.toast
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.queryFirst
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_loading.*
import kotlinx.android.synthetic.main.dialog_pac_layout.view.*
import kotlinx.android.synthetic.main.dialog_pin_layout.view.*
import kotlinx.android.synthetic.main.invoice_fragment.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.api.APIClient
import online.taxcore.pos.api.ApiService
import online.taxcore.pos.api.ApiServiceESDC
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data_managers.InvoiceManager
import online.taxcore.pos.data_managers.JournalManager
import online.taxcore.pos.data_managers.PrefManager
import online.taxcore.pos.data_managers.TaxesManager
import online.taxcore.pos.enums.InvoiceOption
import online.taxcore.pos.enums.InvoiceOption.*
import online.taxcore.pos.enums.InvoiceType
import online.taxcore.pos.enums.PaymentType
import online.taxcore.pos.enums.TransactionType
import online.taxcore.pos.enums.TransactionType.REFUND
import online.taxcore.pos.extensions.*
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.models.Cashier
import online.taxcore.pos.models.InvoiceResponse
import online.taxcore.pos.models.Journal
import online.taxcore.pos.models.PinResponse
import online.taxcore.pos.params.InvoiceRequest
import online.taxcore.pos.params.Item
import online.taxcore.pos.params.PinParams
import online.taxcore.pos.ui.catalog.ItemDetailActivity
import online.taxcore.pos.utils.TCUtil
import org.jetbrains.anko.AnkoLogger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import online.taxcore.pos.models.Item as ItemModel

class InvoiceFragment : Fragment(), AnkoLogger, OnInvoiceOptionResult {

    @Inject
    lateinit var pref: SharedPreferences

    private lateinit var currency: String
    private val invoiceRefRegex = Regex("^[0-9a-zA-Z]{8}-[0-9a-zA-Z]{8}-[0-9]+$")

    private var invoiceAdapter: InvoiceAdapter? = null
    private var favoritesAdapter: FavoriteItemsAdapter? = null
    private var selectableItemsAdapter: SelectableItemsAdapter? = null

    private var sum = ""

    private var isNewInvoiceTypeCopy = false
    private var isNewTransactionTypeRefund = false

    private var invoiceActionType: String = "normal"

    private var dialogESDC: MaterialDialog? = null

    companion object {
        const val DATA_CACHE_INTERVAL = 15.0 // minutes

        const val SCAN_BARCODE_REQUEST = 100
        const val BARCODE_EAN_EXTRA: String = "BARCODE_EAN_EXTRA"

        const val NEW_INVOICE_REQUEST = 200
        const val INVOICE_ID_EXTRA: String = "INVOICE_ID_EXTRA"
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.invoice_fragment, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val isVSDCSelected = pref.getBoolean(PrefConstants.USE_VSDC_SERVER, false)
        val esdcCountry = pref.getString(PrefConstants.COUNTRY, "") ?: ""

        val certOid = PrefManager.loadCertData(pref).tinOid
        currency =
            if (isVSDCSelected) TCUtil.getCurrency(certOid) else TCUtil.getCurrencyBy(esdcCountry)

        arguments?.getString("invoiceType")?.let {
            invoiceActionType = it
        }

        arguments?.getString("invoiceNumber")?.let {
            populateWithInvoiceData(it)
            setInputFields()
        }

        initInvoiceItems()

        setupClickListeners()
        setOnInputChangeListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            NEW_INVOICE_REQUEST -> {
                val invoiceId = data?.getStringExtra(INVOICE_ID_EXTRA).orEmpty()

                if (invoiceId.isEmpty()) {
                    return
                }

                val item = ItemModel().queryFirst { equalTo("uuid", invoiceId) }
                addItemToInvoice(item)
            }
            SCAN_BARCODE_REQUEST -> {
                val barcode = data?.getStringExtra(BARCODE_EAN_EXTRA).orEmpty()

                val item = ItemModel().queryFirst { equalTo("barcode", barcode) }

                if (item != null) {
                    addItemToInvoice(item)
                } else {
                    val intent = Intent(context, ItemDetailActivity::class.java)
                    intent.putExtra(BARCODE_EAN_EXTRA, barcode)
                    startActivityForResult(intent, NEW_INVOICE_REQUEST)
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateUI()
    }

    private fun contributeTime(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        return simpleDateFormat.format(Date())
    }

    private fun isCacheStale(milliseconds: Long): Boolean {
        val cacheTime = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val cacheTimeFromNow = TimeUnit.MILLISECONDS.toMinutes(Date().time) - cacheTime

        return cacheTimeFromNow > DATA_CACHE_INTERVAL
    }

    private fun addItemToInvoice(item: ItemModel?) {
        item?.let {
            if (it.isFavorite) {
                favoritesAdapter?.addNewItem(it)
            }
            invoiceAdapter?.addNewItem(it)
            selectableItemsAdapter?.addNewItem(it)
        }
    }

    private fun setupClickListeners() {

        invoiceAddItemButton.setOnClickListener {
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.MATCH_PARENT)).show {
                customListAdapter(selectableItemsAdapter!!)
            }
        }

        invoiceCreateItemButton.setOnClickListener {
            val intent = Intent(context, ItemDetailActivity::class.java)
            startActivityForResult(intent, NEW_INVOICE_REQUEST)
        }

        invoiceScanItemButton.setOnClickListener {
            startScanActivity()
        }

        invoiceResetButton.setOnClickListener {
            showConfirmResetDialog()
        }

        invoiceFinishButton.setOnClickListener {
            signInvoiceListener()
        }

        invoiceTypeSelect.setOnClickListener {
            showBottomDialog(INVOICE)
        }

        invoiceTransactionTypeSelect.setOnClickListener {
            showBottomDialog(TRANSACTION)
        }

        invoicePaymentSelect.setOnClickListener {
            showBottomDialog(PAYMENT)
        }

    }

    private fun setOnInputChangeListeners() {

        invoiceRefNumberInput.onTextChanged {

            if (isRefInputRequired()) {
                invoiceRefNumberLayout.isErrorEnabled = when {
                    it.isEmpty() -> {
                        invoiceRefNumberLayout.error = getString(R.string.error_ref_doc_required)
                        true
                    }
                    it.isNotEmpty() and !it.matches(invoiceRefRegex) -> {
                        invoiceRefNumberLayout.error =
                            getString(R.string.error_invalid_ref_doc_format)
                        true
                    }
                    else -> false
                }
                return@onTextChanged
            }

            invoiceRefNumberLayout.isErrorEnabled = when {
                isRefInputValid() -> false
                it.isEmpty() -> false
                else -> {
                    invoiceRefNumberLayout.error = getString(R.string.error_invalid_ref_doc_format)
                    true
                }
            }

            validateInvoice()
        }

        invoiceBuyerTinInput.onTextChanged {
            invoiceBuyerCostCenterLayout.visible = it.isNotEmpty()
        }
    }

    private fun updateUI() {
        val isListEmpty = invoiceAdapter?.getInvoiceItems().isNullOrEmpty()

        invoiceNoItems.visible = isListEmpty
        invoiceResetButton.visible = isListEmpty.not() and (invoiceActionType == "normal")

        updateSum()
    }

    private fun populateWithInvoiceData(invoiceNumber: String) {

        val invoiceData = Journal().queryFirst { equalTo("invoiceNumber", invoiceNumber) }

        invoiceData?.let { journal ->
            val restoredItems = Gson().fromJson<List<ItemModel>>(
                journal.invoiceItemsData,
                object : TypeToken<List<ItemModel>>() {}.type
            )

            InvoiceManager.selectedItems = restoredItems.toMutableList()

            invoiceAdapter?.notifyDataSetChanged()

            invoiceRefNumberInput.setText(journal.invoiceNumber)
            invoiceBuyerTinInput.setText(journal.buyerTin)

            invoiceBuyerCostCenterInput.setText(journal.buyerCostCenter)
            invoiceBuyerCostCenterLayout.visible =
                journal.buyerCostCenter.isNotEmpty() or journal.buyerTin.isNotEmpty()

            invoiceTransactionTypeSelect.text = journal.transactionType
            invoiceTypeSelect.text = journal.invoiceType
            invoicePaymentSelect.text = journal.paymentType
        }
    }

    private fun setInputFields() {
        when (invoiceActionType) {
            "copy" -> {
                invoiceActionsLayout.visible = false
                invoiceFavoritesRecycler.visible = false
                invoiceResetButton.visible = false

                invoiceRefNumberInput.isEnabled = false
                invoiceBuyerTinInput.isEnabled = false
                invoiceBuyerCostCenterInput.isEnabled = false

                invoiceTypeSelect.text = getString(R.string.invoice_copy)
                invoiceTypeSelect.isEnabled = false
                invoiceTransactionTypeSelect.isEnabled = false
                invoicePaymentSelect.isEnabled = false
            }
            "refund" -> {
                invoiceActionsLayout.visible = false
                invoiceFavoritesRecycler.visible = false
                invoiceResetButton.visible = false

                invoiceRefNumberInput.isEnabled = false

                invoiceTransactionTypeSelect.text = getString(R.string.invoice_refund)

                invoiceTypeSelect.isEnabled = false
                invoiceTransactionTypeSelect.isEnabled = false
            }
        }
    }

    private fun initInvoiceItems() {
        val catalogItems = ItemModel().queryAll().reversed().toMutableList()
        val favoriteItems = catalogItems.filter { it.isFavorite }

        setupInvoiceItemsList()
        setupFavoritesList(favoriteItems)
        setupSelectableItemsList(catalogItems)
    }

    private fun setupInvoiceItemsList() {
        val taxLabels = TaxesManager.getAllTaxes().map { it.code }
        invoiceAdapter = InvoiceAdapter(
            invoiceType = invoiceActionType,
            validTaxes = taxLabels,
            onItemRemoved = {
                favoritesAdapter?.removeSelection(it)
                selectableItemsAdapter?.removeSelection(it)
                updateUI()
            },
            onItemUpdated = {
                updateSum()
            }
        )

        invoiceAdapter?.setData(InvoiceManager.selectedItems)

        invoiceItemsRecycler.adapter = invoiceAdapter

    }

    private fun setupFavoritesList(catalogItems: List<ItemModel>) {
        val taxLabels = TaxesManager.getAllTaxes().map { it.code }
        favoritesAdapter = FavoriteItemsAdapter(taxLabels) {
            invoiceAdapter?.notifyDataSetChanged()
            updateUI()
        }

        favoritesAdapter?.setData(catalogItems.reversed().toMutableList())

        invoiceFavoritesRecycler.adapter = favoritesAdapter
    }

    private fun setupSelectableItemsList(catalogItems: MutableList<ItemModel>) {
        val taxLabels = TaxesManager.getAllTaxes().map { it.code }
        selectableItemsAdapter = SelectableItemsAdapter(taxLabels) {
            invoiceAdapter?.notifyDataSetChanged()
            favoritesAdapter?.notifyDataSetChanged()
            updateUI()
        }

        selectableItemsAdapter?.setData(catalogItems)
    }

    private fun startScanActivity() {
        Dexter.withContext(activity)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    baseActivity()?.let {
                        val intent = Intent(context, BarcodeScannerActivity::class.java)
                        startActivityForResult(intent, SCAN_BARCODE_REQUEST)
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
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

    private fun signInvoiceListener() {

        // VSDC Server selected
        val isVSDCSelected = pref.getBoolean(PrefConstants.USE_VSDC_SERVER, false)

        val useCachedCredentials = !isCacheStale(PrefManager.getCredentialsTime(pref))

        // Get PAC from session
        if (isVSDCSelected) {
            val invoicePac = AppSession.pacCode.toUpperCase(Locale.getDefault())
            when {
                invoicePac.isNotEmpty() and useCachedCredentials -> signVsdcInvoiceReceipt(
                    invoicePac
                )
                invoicePac.isEmpty() -> showPacInputDialog()
                else -> showPacInputDialog()
            }
            return
        }

        // ESDC selected
        val invoicePin = AppSession.pinCode
        when {
            invoicePin.isNotEmpty() and useCachedCredentials -> verifyEsdcPin(invoicePin)
            invoicePin.isEmpty() -> showPinInputDialog()
            else -> showPinInputDialog()
        }
    }

    private fun createVsdcService(): ApiService? {
        val aliasValue = pref.getString(PrefConstants.CERT_ALIAS_VALUE, "").orEmpty()
        val vsdcEndpoint = loadVSDCEndpoint()

        return APIClient.vsdc(vsdcEndpoint, aliasValue)
    }

    private fun createESDCService(): ApiServiceESDC? {
        val esdcEndpoint = loadESDCEndpoint()
        return APIClient.esdc(esdcEndpoint)
    }

    private fun verifyEsdcPin(pinCode: String) {
        dialogESDC = createLoadingDialog(R.string.loading_please_wait)

        val pin = PinParams()
        pin.VPIN = pinCode

        val esdcService = createESDCService()
        val verifyRequest = esdcService?.verifyPin(pin)

        if (verifyRequest == null) {
            dialogESDC?.dismiss()
            longToast(R.string.toast_esdc_address_invalid)
            return
        }

        verifyRequest.enqueue(object : Callback<PinResponse> {
            override fun onFailure(call: Call<PinResponse>?, t: Throwable?) {
                t?.message?.let { longToast(it) }
                dialogESDC?.dismiss()
            }

            override fun onResponse(call: Call<PinResponse>?, response: Response<PinResponse>?) {
                response?.let { res ->
                    if (res.isSuccessful.not()) {
                        dialogESDC?.dismiss()
                        val errorMessage = res.errorBody()?.string()
                        errorMessage?.let { err -> showErrorMessage(err) }
                        return@let
                    }

                    res.body()?.let { pinRes ->
                        if (pinRes.VPIN_GSC.contains("0100")) {
                            //PIN is valid save it to session
                            AppSession.pinCode = pinCode
                            PrefManager.saveCredentialsTime(pref)

                            signEsdcInvoice()
                            return
                        }

                        if (pinRes.VPIN_GSC.contains("2100")) {
                            longToast(R.string.toast_pin_invalid)
                        }
                        dialogESDC?.dismiss()
                    }
                }
            }
        })
    }

    private fun showPinInputDialog() {
        val PIN_INPUT_LENGTH = 4

        val clipboardText = getClipboardText()
        val isPinFormatValid =
            TextUtils.isDigitsOnly(clipboardText) and (PIN_INPUT_LENGTH == clipboardText.length)

        MaterialDialog(requireContext()).show {
            title(R.string.title_enter_pin)
            customView(R.layout.dialog_pin_layout)

            if (isPinFormatValid) {
                positiveButton(R.string.paste_and_sign) {
                    getCustomView().pinInputView.setText(clipboardText)
                }
            }

            // Add input listener
            getCustomView().pinInputView.onTextChanged { inputText ->
                setActionButtonEnabled(WhichButton.POSITIVE, inputText.length == PIN_INPUT_LENGTH)
                if (inputText.length == PIN_INPUT_LENGTH) {
                    signInvoiceWithPin(this)
                    dismiss()
                }
            }
        }
    }

    private fun signInvoiceWithPin(it: MaterialDialog) {
        val inputPin = it.getCustomView().pinInputView.text.toString()
        verifyEsdcPin(inputPin)
    }

    private fun getClipboardText(): CharSequence {
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)

        return item?.text?.trim() ?: ""
    }

    private fun showPacInputDialog() {
        val PAC_INPUT_LENGTH = 6

        val clipboardText = getClipboardText()
        val isPacFormatValid = PAC_INPUT_LENGTH == clipboardText.length

        MaterialDialog(requireContext()).show {
            title(R.string.title_enter_pac)
            customView(R.layout.dialog_pac_layout)

            if (isPacFormatValid) {
                positiveButton(R.string.paste_and_sign) {
                    getCustomView().pacInputView.setText(clipboardText)
                }
            }

            getCustomView().pacInputView.onTextChanged { inputText ->
                setActionButtonEnabled(WhichButton.POSITIVE, inputText.length == PAC_INPUT_LENGTH)
                if (inputText.length == PAC_INPUT_LENGTH) {
                    val inputPac = this.getCustomView().pacInputView.text.toString()
                        .toUpperCase(Locale.getDefault())
                    signVsdcInvoiceReceipt(inputPac)
                    dismiss()
                }
            }
        }
    }

    private fun showConfirmResetDialog() {
        MaterialDialog(requireContext()).show {
            title(text = "Caution")
            message(text = getString(R.string.invoice_reset_message))

            negativeButton(R.string.btn_close)
            positiveButton(R.string.btn_reset_invoice) {
                resetInvoice()
            }
        }
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).show {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }

    private fun prepareInvoiceRequestBody(pac: String = ""): InvoiceRequest {
        val invoiceRequest = InvoiceRequest()

        val selectedCashier = Cashier().queryFirst { equalTo("isChecked", true) }
        val cashierId =
            if (selectedCashier?.id.isNullOrBlank()) null else selectedCashier?.id.orEmpty()

        with(invoiceRequest) {
            dateAndTimeOfIssue = contributeTime()
            iT = invoiceTypeSelect.text as String
            paymentType = invoicePaymentSelect.text as String
            tT = invoiceTransactionTypeSelect.text as String
            cashier = cashierId
            pAC = pac.stringOrNull()
            bD = invoiceBuyerTinInput.stringOrNull()
            buyerCostCenterId = invoiceBuyerCostCenterInput.stringOrNull()
            referentDocumentNumber = invoiceRefNumberInput.stringOrNull()
            items = collectSelectedItems()
        }

        val gsonBuilder = GsonBuilder().serializeNulls().create()
        val jsonRequest = gsonBuilder.toJson(invoiceRequest)

        invoiceRequest.hash = jsonRequest.md5

        return invoiceRequest
    }

    private fun collectSelectedItems(): ArrayList<Item> {
        val items: ArrayList<Item> = arrayListOf()

        InvoiceManager.selectedItems.forEach {
            val item = Item()

            val gtin = if (it.barcode.isEmpty()) null else it.barcode

            val taxLabels = arrayListOf<String>()
            it.tax.mapTo(taxLabels) { label -> label.code }

            with(item) {
                name = it.name
                quantity = it.quantity.roundingToDecimal(3)
                labels = taxLabels
                totalAmount = (it.price * it.quantity).roundingToDecimal(2)
                unitPrice = it.price.roundingToDecimal(2)
                GTIN = gtin
            }
            items.add(item)
        }

        return items
    }

    private fun showBottomDialog(invoiceOption: InvoiceOption) =
        InvoiceBottomDialog.showBottomDialog(
            activity?.supportFragmentManager,
            invoiceOption.value,
            this
        )

    private fun signVsdcInvoiceReceipt(inputPac: String) {
        val loadingDialog = createLoadingDialog(R.string.loading_please_wait)

        AppSession.pacCode = inputPac
        PrefManager.saveCredentialsTime(pref)

        runAsync {
            val invoiceRequestBody = prepareInvoiceRequestBody(inputPac)

            val vsdcService = createVsdcService()
            val call = vsdcService?.postInvoice(invoiceRequestBody)

            if (call == null) {
                runOnUiThread {
                    longToast("V-SDC address is invalid")
                    AppSession.resetSession()
                    loadingDialog.dismiss()
                }
                return@runAsync
            }

            call.enqueue(onSignInvoiceCallback(loadingDialog))
        }
    }

    private fun signEsdcInvoice() {
        val invoiceRequestBody = prepareInvoiceRequestBody()

        val esdcService = createESDCService()
        val call = esdcService?.postInvoice(invoiceRequestBody)

        dialogESDC?.let {
            call?.enqueue(onSignInvoiceCallback(it))
        }
    }

    private fun onSignInvoiceCallback(dialog: MaterialDialog): Callback<InvoiceResponse> {
        return object : Callback<InvoiceResponse> {
            override fun onFailure(call: Call<InvoiceResponse>?, t: Throwable?) {
                t?.message?.let { longToast(it) }
                AppSession.resetSession()
                dialog.dismiss()
            }

            override fun onResponse(
                call: Call<InvoiceResponse>?,
                response: Response<InvoiceResponse>?
            ) {
                response?.let { res ->
                    try {

                        if (res.isSuccessful) {
                            val invoiceResponse = res.body()
                            dialog.dismiss()
                            handleSuccessfulInvoiceSign(invoiceResponse)
                            return
                        }

                        AppSession.resetSession()

                        val errorMessage = response.errorBody()?.string()
                        if (errorMessage?.contains("PAC")!! && errorMessage.isNotEmpty()) {
                            longToast("You have to provide valid PAC value")
                            return
                        }

                        if (errorMessage.startsWith("<!DOCTYPE html", true)) {
                            val parsedMsg =
                                errorMessage.substringAfter("<h3>").substringBefore("</h3>")
                            showErrorMessage(parsedMsg)
                        } else {
                            showErrorMessage(errorMessage)
                        }
                    } catch (e: NullPointerException) {
                        longToast(response.message())
                    } finally {
                        dialog.dismiss()
                    }

                }
            }
        }
    }

    private fun handleSuccessfulInvoiceSign(signedInvoice: InvoiceResponse?): Unit {

        signedInvoice?.let { invoice ->
            saveJournalItem(invoice)
            resetInvoice()

            FiscalInvoiceFragment.showFiscalDialog(
                activity?.supportFragmentManager,
                invoice.Journal.toString(),
                invoice.VerificationQRCode.toString(),
                invoice.IN.toString(),
                invoice.VerificationUrl,
                invoiceActionType
            )
        }
    }

    private fun saveJournalItem(signedInvoice: InvoiceResponse) {
        JournalManager.saveItem(
            body = signedInvoice,
            buyerId = invoiceBuyerTinInput.text.toString(),
            paymentType = invoicePaymentSelect.text.toString(),
            transactionType = invoiceTransactionTypeSelect.text.toString(),
            invoiceType = invoiceTypeSelect.text.toString(),
            buyerCostCenter = invoiceBuyerCostCenterInput.text.toString(),
            items = InvoiceManager.selectedItems
        )
    }

    private fun showErrorMessage(errorMessage: String) {
        val stringWithoutBrackets = errorMessage.removeBrackets()
        AlertDialogHelper.showSimpleAlertDialog(activity, stringWithoutBrackets.showErrorMessage())
    }

    private fun loadVSDCEndpoint(): String {
        return pref.getString(PrefConstants.VSDC_ENDPOINT_URL, "").orEmpty()
    }

    private fun loadESDCEndpoint(): String {
        return pref.getString(PrefConstants.ESDC_ENDPOINT_URL, "").orEmpty()
    }

    private fun resetInvoice() {
        isNewInvoiceTypeCopy = false
        isNewTransactionTypeRefund = false

        invoiceRefNumberInput.setText("")
        invoiceBuyerTinInput.setText("")
        invoiceBuyerCostCenterInput.setText("")

        invoiceTypeSelect.text = resources.getString(R.string.normal)
        invoiceTransactionTypeSelect.text = resources.getString(R.string.sale)
        invoicePaymentSelect.text = resources.getString(R.string.cash)

        InvoiceManager.selectedItems.clear()

        initInvoiceItems()

        updateUI()
    }

    private fun updateSum() {
        invoiceAdapter?.let { adapter ->
            sum = adapter.getInvoiceItems().sumByDouble { it.price * it.quantity }
                .roundTo2DecimalPlaces()
            invoiceSumLabel.text = "${currency} ${sum}"
        }

        validateInvoice()
    }

    private fun validateInvoice() {
        val invoiceType = invoiceTypeSelect.text as String
        val transactionType = invoiceTransactionTypeSelect.text as String
        val paymentType = invoicePaymentSelect.text as String

        val refValid = isRefInputValid()

        val isSubmitEnabled = hasInvoiceItems() and
                invoiceType.isNotBlank() and
                transactionType.isNotBlank() and
                paymentType.isNotBlank() and
                refValid

        if (isRefInputRequired()) {
            invoiceRefNumberLayout.error = getString(R.string.error_ref_doc_required)
        }

        invoiceRefNumberLayout.isErrorEnabled = refValid.not()
        invoiceFinishButton.isEnabled = isSubmitEnabled
    }

    private fun isRefInputValid(): Boolean {
        val invoiceRef = invoiceRefNumberInput.text.toString()

        return if (isRefInputRequired()) {
            invoiceRef.isNotEmpty() and invoiceRef.matches(invoiceRefRegex)
        } else {
            invoiceRef.isEmpty() or invoiceRef.matches(invoiceRefRegex)
        }
    }

    private fun isRefInputRequired(): Boolean = isNewInvoiceTypeCopy or isNewTransactionTypeRefund

    private fun hasInvoiceItems(): Boolean = invoiceAdapter?.getInvoiceItems()?.isNotEmpty()
        ?: false

    override fun onInvoiceTypeChanged(invoiceType: InvoiceType, selectedValue: String) {
        isNewInvoiceTypeCopy = when (invoiceType) {
            InvoiceType.COPY -> true
            else -> false
        }

        validateInvoice()
    }

    override fun onTransactionTypeChanged(transactionType: TransactionType, selectedValue: String) {
        isNewTransactionTypeRefund = when (transactionType) {
            REFUND -> true
            else -> false
        }

        validateInvoice()
    }

    override fun onPaymentChanged(paymentType: PaymentType, selectedValue: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setTitle(result: String, option: InvoiceOption) {
        when (option) {
            PAYMENT -> invoicePaymentSelect.text = result
            TRANSACTION -> invoiceTransactionTypeSelect.text = result
            INVOICE -> invoiceTypeSelect.text = result
        }
    }
}
