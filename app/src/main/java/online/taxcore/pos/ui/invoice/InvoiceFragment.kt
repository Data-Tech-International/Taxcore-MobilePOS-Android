package online.taxcore.pos.ui.invoice

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.datetime.dateTimePicker
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
import online.taxcore.pos.BuildConfig
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.api.APIClient
import online.taxcore.pos.data.api.ApiService
import online.taxcore.pos.data.api.ApiServiceESDC
import online.taxcore.pos.data.api.handleCertificates.CertAuthority
import online.taxcore.pos.data.local.CertManager
import online.taxcore.pos.data.local.InvoiceManager
import online.taxcore.pos.data.local.JournalManager
import online.taxcore.pos.data.local.TaxesManager
import online.taxcore.pos.data.models.InvoiceResponse
import online.taxcore.pos.data.models.Item
import online.taxcore.pos.data.params.InvoiceRequest
import online.taxcore.pos.data.params.InvoiceRequestType
import online.taxcore.pos.data.params.PaymentItem
import online.taxcore.pos.data.realm.Cashier
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.enums.InvoiceOption
import online.taxcore.pos.enums.InvoiceOption.*
import online.taxcore.pos.enums.InvoiceType
import online.taxcore.pos.enums.InvoiceType.*
import online.taxcore.pos.enums.PaymentType
import online.taxcore.pos.enums.PaymentType.*
import online.taxcore.pos.enums.TransactionType
import online.taxcore.pos.enums.TransactionType.REFUND
import online.taxcore.pos.enums.TransactionType.SALE
import online.taxcore.pos.extensions.*
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.ui.catalog.ItemDetailActivity
import online.taxcore.pos.utils.TCUtil
import org.jetbrains.anko.AnkoLogger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import online.taxcore.pos.data.realm.Item as ItemModel

class InvoiceFragment : Fragment(), AnkoLogger, OnInvoiceOptionResult {

    @Inject
    lateinit var prefService: PrefService

    private lateinit var currency: String
    private val invoiceRefRegex = Regex("^[0-9a-zA-Z]{8}-[0-9a-zA-Z]{8}-[0-9]+$")

    private var invoiceAdapter: InvoiceAdapter? = null
    private var favoritesAdapter: FavoriteItemsAdapter? = null
    private var selectableItemsAdapter: SelectableItemsAdapter? = null

    private var invoiceSumAmount = 0.0

    private var isNewInvoiceTypeCopy = false
    private var isNewTransactionTypeRefund = false

    private var invoiceActionType: String = "normal"
    private var invoiceNumber: String = ""

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
        super.onViewCreated(view, savedInstanceState)

        val isVSDCSelected = prefService.useVSDCServer()
        val esdcCountry = prefService.loadCertCountry()

        val certOid = prefService.loadTinOid()
        currency =
            if (isVSDCSelected) TCUtil.getCurrency(certOid) else TCUtil.getCurrencyBy(esdcCountry)

        initInitial()

        arguments?.getString("invoiceType")?.let {
            invoiceActionType = it
        }

        arguments?.getString("invoiceNumber")?.let {
            invoiceNumber = it
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

    private fun initInitial() {
        invoiceTypeSelect.tag = NORMAL.value
        invoiceTypeSelect.text = getString(R.string.normal)

        invoiceTransactionTypeSelect.tag = SALE.value
        invoiceTransactionTypeSelect.text = getString(R.string.sale)

        invoicePaymentSelect.tag = CASH.value
        invoicePaymentSelect.text = getString(R.string.cash)
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

        invoiceRefTimeButton.setOnClickListener {
            showDateTimePicker()
        }

    }

    private fun setOnInputChangeListeners() {

        invoiceRefNumberInput.onTextChanged { refInputText ->

            invoiceRefDTLayout.visible = refInputText.isNotEmpty() and isRefInputValid()

            validateInvoice()

            if (isRefInputRequired()) {

                val isRefErrorEnabled = when {
                    refInputText.isEmpty() -> {
                        invoiceRefNumberLayout.error = getString(R.string.error_ref_doc_required)
                        true
                    }
                    refInputText.isNotEmpty() and !refInputText.matches(invoiceRefRegex) -> {
                        invoiceRefNumberLayout.error =
                            getString(R.string.error_invalid_ref_doc_format)
                        true
                    }
                    else -> false
                }

                invoiceRefNumberLayout.isErrorEnabled = isRefErrorEnabled

                return@onTextChanged
            }

            // Error enabled when
            invoiceRefNumberLayout.isErrorEnabled = when {
                isRefInputValid() -> false
                refInputText.isEmpty() -> false
                else -> {
                    invoiceRefNumberLayout.error = getString(R.string.error_invalid_ref_doc_format)
                    true
                }
            }
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

            invoiceTransactionTypeSelect.tag = journal.transactionType
            invoiceTransactionTypeSelect.text = findTransactionTypeName(journal.transactionType)

            invoiceRefTimeButton.text = formatDate(journal.date)

            invoiceTypeSelect.tag = journal.invoiceType
            invoiceTypeSelect.text = findInvoiceTypeName(journal.invoiceType)

            invoicePaymentSelect.tag = journal.paymentType
            invoicePaymentSelect.text = findPaymentTypeName(journal.paymentType)
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

                invoiceRefDTLayout.visible = true
                invoiceRefTimeButton.isEnabled = false
                invoiceRefDTLabel.visibility = View.VISIBLE

                invoiceTypeSelect.tag = COPY.value
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
                invoiceRefTimeButton.isEnabled = false
                invoiceRefDTLayout.visible = true
                invoiceRefDTLabel.visibility = View.VISIBLE

                invoiceTransactionTypeSelect.tag = REFUND.value
                invoiceTransactionTypeSelect.text = getString(R.string.invoice_refund)
                invoiceTransactionTypeSelect.isEnabled = false

                invoiceTypeSelect.isEnabled = false
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
        val isVSDCSelected = prefService.useVSDCServer()

        val useCachedCredentials = !isCacheStale(prefService.getCredentialsTime())

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
        val activeCertName = prefService.loadActiveCertName()
        val pfxPass = prefService.loadPfxPass(activeCertName)
        val cert = CertManager.loadCert(activeCertName)

        val ca = CertAuthority.certificateParams(cert!!.pfxData, pfxPass)

        return APIClient.vsdc(ca)
    }

    private fun createESDCService(): ApiServiceESDC? {
        val esdcEndpoint = prefService.loadEsdcEndpoint()
        return APIClient.esdc(esdcEndpoint)
    }

    private fun verifyEsdcPin(pinCode: String) {
        dialogESDC = createLoadingDialog(R.string.loading_please_wait)

        val esdcService = createESDCService()
        val verifyRequest = esdcService?.verifyPin(pinCode)

        if (verifyRequest == null) {
            dialogESDC?.dismiss()
            longToast(R.string.toast_esdc_address_invalid)
            return
        }

        verifyRequest.enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>?, t: Throwable?) {
                t?.message?.let { longToast(it) }
                dialogESDC?.dismiss()
            }

            override fun onResponse(call: Call<String>?, response: Response<String>?) {

                response ?: return

                if (response.isSuccessful.not()) {
                    dialogESDC?.dismiss()
                    // TODO: Improve error handling
                    AlertDialogHelper.showSimpleAlertDialog(
                        activity,
                        getString(R.string.error_general)
                    )
                    return
                }

                when (val cardStatus = response.body()!!) {
                    "0100" -> {
                        //PIN is valid save it to session
                        AppSession.pinCode = pinCode
                        prefService.saveCredentialsTime()
                        toast(getMessageForStatus(cardStatus))
                        signEsdcInvoice()
                        return
                    }
                    else -> longToast(getMessageForStatus(cardStatus))
                }

                dialogESDC?.dismiss()
            }
        })
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
            "2210" -> R.string.esdc_status_2210
            "2400" -> R.string.esdc_status_2400
            else -> R.string.error_general
        }
    }

    private fun showPinInputDialog() {
        val PIN_INPUT_LENGTH = 4

        MaterialDialog(requireContext()).show {
            title(R.string.title_enter_pin)
            customView(R.layout.dialog_pin_layout)

            // Add input listener
            getCustomView().pinInputView.onTextChanged { inputText ->
                setActionButtonEnabled(WhichButton.NEUTRAL, inputText.length == PIN_INPUT_LENGTH)
                if (inputText.length == PIN_INPUT_LENGTH) {
                    signInvoiceWithPin(this)
                    dismiss()
                }
            }

            setActionButtonEnabled(WhichButton.NEUTRAL, getClipboardText().isNotEmpty())
            neutralButton(R.string.paste_and_sign) {
                val clipboardText = getClipboardText()
                getCustomView().pinInputView.setText(clipboardText)
                dismiss()
            }

            negativeButton(R.string.cancel) {
                dismiss()
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

        MaterialDialog(requireContext()).show {
            title(R.string.title_enter_pac)
            customView(R.layout.dialog_pac_layout)

            getCustomView().pacInputView.onTextChanged { inputText ->
                if (inputText.length == PAC_INPUT_LENGTH) {
                    val inputPac = this.getCustomView().pacInputView.text.toString()
                        .toUpperCase(Locale.getDefault())
                    signVsdcInvoiceReceipt(inputPac)
                    dismiss()
                }
            }

            setActionButtonEnabled(WhichButton.NEUTRAL, getClipboardText().isNotEmpty())
            neutralButton(R.string.paste_and_sign) {
                val clipboardText = getClipboardText()
                getCustomView().pacInputView.setText(clipboardText)
            }

            negativeButton(R.string.cancel) {
                dismiss()
            }
        }
    }

    private fun showConfirmResetDialog() {
        MaterialDialog(requireContext()).show {
            title(text = getString(R.string.title_caution))
            message(text = getString(R.string.invoice_reset_message))

            negativeButton(R.string.btn_close)
            positiveButton(R.string.btn_reset_invoice) {
                resetInvoice()
            }
        }
    }

    private fun showDateTimePicker() {
        MaterialDialog(requireContext()).show {

            val refTimeBtn =
                this@InvoiceFragment.view?.findViewById<AppCompatButton>(R.id.invoiceRefTimeButton)
            val refTimeLabel =
                this@InvoiceFragment.view?.findViewById<AppCompatTextView>(R.id.invoiceRefDTLabel)

            dateTimePicker(show24HoursView = true) { _, dateTime ->
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ")
                val timeString = simpleDateFormat.format(dateTime.time)

                refTimeLabel?.visibility = View.VISIBLE
                refTimeLabel?.text = getString(R.string.ref_dt)

                refTimeBtn?.text = formatDate(timeString)

                validateInvoice()
            }

            neutralButton(R.string.reset) {
                refTimeLabel?.visibility = View.INVISIBLE
                refTimeLabel?.text = ""

                refTimeBtn?.text = ""
                refTimeBtn?.hint = getString(R.string.ref_dt)
            }
        }
    }

    private fun createLoadingDialog(@StringRes stringId: Int = R.string.loading_please_wait): MaterialDialog =
        MaterialDialog(requireContext()).show {
            customView(R.layout.dialog_loading).loadingDialogText.text = getString(stringId)

            cancelable(false)  // calls setCancelable on the underlying dialog
            cancelOnTouchOutside(false)  // calls setCanceledOnTouchOutside on the underlying dialog
        }

    private fun prepareInvoiceRequestBody(reqType: InvoiceRequestType): InvoiceRequest {
        val invoiceRequest = InvoiceRequest()

        val selectedCashier = Cashier().queryFirst { equalTo("isChecked", true) }
        val cashierId =
            if (selectedCashier?.id.isNullOrBlank()) null else selectedCashier?.id.orEmpty()

        val referentDocNo = invoiceRefNumberInput.stringOrNull()

        val refDTText = invoiceRefTimeButton.text.toString()
        val refDT =
            if (refDTText != getString(R.string.ref_dt)) refDTText else null

        with(invoiceRequest) {
            invoiceNumber = BuildConfig.VERSION_NAME
            paymentType = invoicePaymentSelect.tag as String
            transactionType = invoiceTransactionTypeSelect.tag as String?
            invoiceType = invoiceTypeSelect.tag as String
            cashier = cashierId
            buyerId = invoiceBuyerTinInput.stringOrNull()
            buyerCostCenterId = invoiceBuyerCostCenterInput.stringOrNull()
            referentDocumentNumber = referentDocNo
            referentDocumentDT = refDT
            payment = listOf(PaymentItem(invoiceSumAmount.toDouble(), paymentType))
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

            val gtin = it.barcode.ifEmpty { null }

            val taxLabels = arrayListOf<String>()
            it.tax.mapTo(taxLabels) { label -> label.code }

            with(item) {
                name = it.name
                quantity = it.quantity.roundingToDecimal(3)
                labels = taxLabels
                totalAmount = (it.price * it.quantity).roundingToDecimal(2)
                unitPrice = it.price.roundingToDecimal(2)
                this.gtin = gtin
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
        prefService.saveCredentialsTime()

        runAsync {
            val invoiceRequestBody = prepareInvoiceRequestBody(InvoiceRequestType.VSDC)

            val vsdcService = createVsdcService()
            val call = vsdcService?.createInvoice(inputPac, invoiceRequestBody)

            if (call == null) {
                runOnUiThread {
                    longToast(getString(R.string.error_invalid_vsdc))
                    AppSession.resetSessionCredentials()
                    loadingDialog.dismiss()
                }
                return@runAsync
            }

            call.enqueue(onSignInvoiceCallback(loadingDialog))
        }
    }

    private fun signEsdcInvoice() {
        val invoiceRequestBody = prepareInvoiceRequestBody(InvoiceRequestType.ESDC)

        val esdcService = createESDCService()
        val call = esdcService?.createInvoice(invoiceRequestBody)

        dialogESDC?.let {
            call?.enqueue(onSignInvoiceCallback(it))
        }
    }

    private fun onSignInvoiceCallback(dialog: MaterialDialog): Callback<InvoiceResponse> {
        return object : Callback<InvoiceResponse> {
            override fun onFailure(call: Call<InvoiceResponse>, t: Throwable) {
                t.message?.let { longToast(it) }
                AppSession.resetSessionCredentials()
                dialog.dismiss()
            }

            override fun onResponse(call: Call<InvoiceResponse>, res: Response<InvoiceResponse>) {
                try {

                    if (res.isSuccessful) {
                        val invoiceResponse = res.body()
                        dialog.dismiss()
                        handleSuccessfulInvoiceSign(invoiceResponse)
                        return
                    }

                    AppSession.resetSessionCredentials()

                    val errorMessage = res.errorBody()?.string()
                    if (errorMessage?.contains("PAC")!! && errorMessage.isNotEmpty()) {
                        longToast(getString(R.string.error_provide_valid_pac))
                        return
                    }

                    longToast(getString(R.string.error_general))
                } catch (e: NullPointerException) {
                    longToast(res.message())
                } finally {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun handleSuccessfulInvoiceSign(signedInvoice: InvoiceResponse?): Unit {
        signedInvoice?.let { invoice ->
            prefService.saveEsdcLocation(signedInvoice.locationName)
            saveJournalItem(invoice)
            resetInvoice()

            FiscalInvoiceFragment.showFiscalDialog(
                activity?.supportFragmentManager,
                invoice.journal.toString(),
                invoice.verificationQRCode.toString(),
                invoice.invoiceNumber.toString(),
                invoice.verificationUrl,
                invoiceActionType
            )
        }
    }

    private fun saveJournalItem(signedInvoice: InvoiceResponse) {
        JournalManager.saveItem(
            body = signedInvoice,
            buyerId = invoiceBuyerTinInput.text.toString(),
            paymentType = invoicePaymentSelect.tag as String,
            transactionType = invoiceTransactionTypeSelect.tag as String,
            invoiceType = invoiceTypeSelect.tag as String,
            buyerCostCenter = invoiceBuyerCostCenterInput.text.toString(),
            items = InvoiceManager.selectedItems
        )
    }

    private fun resetInvoice() {
        isNewInvoiceTypeCopy = false
        isNewTransactionTypeRefund = false

        invoiceRefNumberInput.setText("")
        invoiceRefTimeButton.setText("")
        invoiceBuyerTinInput.setText("")
        invoiceBuyerCostCenterInput.setText("")

        invoiceTypeSelect.tag = NORMAL.value
        invoiceTypeSelect.text = getString(R.string.normal)

        invoiceTransactionTypeSelect.tag = SALE.value
        invoiceTransactionTypeSelect.text = getString(R.string.sale)

        invoicePaymentSelect.tag = CASH.value
        invoicePaymentSelect.text = getString(R.string.cash)

        InvoiceManager.selectedItems.clear()

        initInvoiceItems()

        updateUI()
    }

    private fun updateSum() {
        invoiceAdapter?.let { adapter ->
            invoiceSumAmount = adapter.getInvoiceItems().sumOf { it.price * it.quantity }

            invoiceSumLabel.text = "$currency ${invoiceSumAmount.roundLocalized(2)}"
        }

        validateInvoice()
    }

    private fun validateInvoice() {
        val invoiceType = invoiceTypeSelect.text as String
        val invoiceTypeTag = invoiceTypeSelect.tag as String?

        val transactionType = invoiceTransactionTypeSelect.text as String
        val transactionTypeTag = invoiceTransactionTypeSelect.tag as String?

        val paymentType = invoicePaymentSelect.text as String
        val paymentTypeTag = invoicePaymentSelect.tag as String?

        val refValid = isRefInputValid()

        val isSubmitEnabled = hasInvoiceItems() and
                invoiceType.isNotBlank() and
                invoiceTypeTag.orEmpty().isNotBlank() and
                transactionType.isNotBlank() and
                transactionTypeTag.orEmpty().isNotBlank() and
                paymentType.isNotBlank() and
                paymentTypeTag.orEmpty().isNotBlank() and
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

// INVOICE TYPE

    private fun findInvoiceTypeName(invoiceType: String): String {
        return when (invoiceType.toLowerCase()) {
            NORMAL.value -> getString(R.string.normal)
            PROFORMA.value -> getString(R.string.proforma)
            COPY.value -> getString(R.string.copy)
            TRAINING.value -> getString(R.string.training)
            else -> throw Error("Unknown invoice type")
        }
    }

// PAYMENT TYPE

    private fun findPaymentTypeName(paymentType: String): String {
        return when (paymentType.toLowerCase()) {
            CASH.value -> getString(R.string.cash)
            CARD.value -> getString(R.string.card)
            OTHER.value -> getString(R.string.other)
            CHECK.value -> getString(R.string.check)
            WIRE_TRANSFER.value -> getString(R.string.wiretransfer)
            VOUCHER.value -> getString(R.string.voucher)
            MOBILE_MONEY.value -> getString(R.string.mobile_money)
            else -> throw Error("Unknown payment type")

        }
    }

// TRANSACTION TYPE

    private fun findTransactionTypeName(transactionType: String): String {
        return when (transactionType.toLowerCase()) {
            SALE.value -> getString(R.string.sale)
            REFUND.value -> getString(R.string.refund)
            else -> throw Error("Unknown transaction type")
        }
    }

// LISTENERS

    override fun onInvoiceTypeChanged(invoiceType: InvoiceType, selectedValue: String) {
        invoiceTypeSelect.tag = invoiceType.value
        isNewInvoiceTypeCopy = when (invoiceType) {
            COPY -> true
            else -> false
        }

        validateInvoice()
    }

    override fun onTransactionTypeChanged(transactionType: TransactionType, selectedValue: String) {
        invoiceTransactionTypeSelect.tag = transactionType.value
        isNewTransactionTypeRefund = when (transactionType) {
            REFUND -> true
            else -> false
        }

        validateInvoice()
    }

    override fun onPaymentChanged(paymentType: PaymentType, selectedValue: String) {
        invoicePaymentSelect.tag = paymentType.value
    }

    override fun setTitle(result: String, option: InvoiceOption) {
        when (option) {
            PAYMENT -> invoicePaymentSelect.text = result
            TRANSACTION -> invoiceTransactionTypeSelect.text = result
            INVOICE -> invoiceTypeSelect.text = result
        }
    }

    private fun formatDate(stringDate: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        val writeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val date: Date? = try {
            dateFormat.parse(stringDate)
        } catch (e: ParseException) {
            null
        }

        return if (date != null) writeFormat.format(date) else ""
    }
}
