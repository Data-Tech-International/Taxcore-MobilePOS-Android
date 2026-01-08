package online.taxcore.pos.ui.catalog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.vicpin.krealmextensions.queryAndUpdate
import com.vicpin.krealmextensions.queryFirst
import com.vicpin.krealmextensions.save
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.local.TaxesManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.data.realm.TaxesSettings
import online.taxcore.pos.databinding.ItemDetailsActivityBinding
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.roundTo2DecimalPlaces
import online.taxcore.pos.extensions.roundToDecimal
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.helpers.DecimalDigitsInputFilter
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.ui.common.TaxesCheckedAdapter
import online.taxcore.pos.ui.invoice.InvoiceFragment
import online.taxcore.pos.ui.invoice.InvoiceFragment.Companion.BARCODE_EAN_EXTRA
import online.taxcore.pos.utils.hideKeyboard
import online.taxcore.pos.utils.longToast
import javax.inject.Inject

class ItemDetailActivity : BaseActivity() {

    private lateinit var binding: ItemDetailsActivityBinding

    @Inject
    lateinit var prefService: PrefService

    private var taxesCheckedAdapter: TaxesCheckedAdapter? = null
    private var invalidTaxesCheckedAdapter: TaxesCheckedAdapter? = null

    private var item: Item? = null
    private var oldName = ""

    private var isNameValid: Boolean = false
    private var isPriceValid: Boolean = false
    private var isBarcodeValid: Boolean = true

    private var isInCreateMode: Boolean = false
    private var isInEditMode: Boolean = false

    private var favoriteMenuItem: MenuItem? = null
    private var confirmMenuItem: MenuItem? = null

    private var updatedFieldsMap = mutableMapOf<String, Boolean>()
    private var useESDC: Boolean = false

    companion object {
        private const val EXTRA_ITEM_UUID = "EXTRA_ITEM_UUID"

        fun start(context: Context, itemUUID: String? = "") {
            val intent = Intent(context, ItemDetailActivity::class.java).apply {
                putExtra(EXTRA_ITEM_UUID, itemUUID.orEmpty())
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ActivityCompat.startActivity(context, intent, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ItemDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFromBundleExtra()

        initToolbar()
        initTaxesAdapter()
        initFields()

        setOnNameInputChangeHandler()
        setOnPriceChangeHandler()
        setOnBarcodeInputChangeHandler()
    }

    override fun onResume() {
        super.onResume()
        useESDC = prefService.useESDCServer()
        populateTaxLabels(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.item_action, menu)

        favoriteMenuItem = menu.findItem(R.id.actionItemFavorite)
        confirmMenuItem = menu.findItem(R.id.actionItemSave)

        (favoriteMenuItem?.actionView as CheckBox).isChecked = item?.isFavorite ?: false

        setOnFavoriteChangeHandler()
        validateInputForm()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        R.id.actionItemSave -> {
            if (isInCreateMode and isFormValid()) {
                saveItem()
            } else if (isFormValid()) {
                updateItem()
            }

            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun initToolbar() {
        binding.appBarMain.toolbar.setNavigationIcon(R.drawable.ic_exit)
        setSupportActionBar(binding.appBarMain.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = if (isInCreateMode) getString(R.string.add_item) else getString(R.string.edit_item)
    }

    private fun initFields() {

        val isAppConfigured = prefService.isAppConfigured()

        binding.activityDetailPluInputName.isEnabled = isAppConfigured
        binding.activityDetailPluInputPrice.isEnabled = isAppConfigured
        binding.activityDetailPluInputBarcode.isEnabled = isAppConfigured

        if (isInEditMode) {
            populateItemFields(item)
        }
    }

    private fun initFromBundleExtra() {
        val itemUUID = intent.getStringExtra(EXTRA_ITEM_UUID).orEmpty()
        val barcode = intent.getStringExtra(BARCODE_EAN_EXTRA).orEmpty()

        binding.activityDetailPluInputBarcode.setText(barcode)

        isInCreateMode = itemUUID.isEmpty()
        isInEditMode = itemUUID.isNotEmpty()

        if (isInEditMode) {
            item = Item().queryFirst { equalTo("uuid", itemUUID) }
        }
    }

    private fun populateItemFields(item: Item?) {
        item?.let {
            oldName = it.name
            binding.activityDetailPluInputName.setText(oldName)
            binding.activityDetailPluInputPrice.setText(it.price.roundToDecimal())
            binding.activityDetailPluInputBarcode.setText(it.barcode)
        }

        isNameValid = true
        isPriceValid = true
        isBarcodeValid = true

        validateInputForm()
    }

    private fun populateTaxLabels(item: Item?) {
        val appTaxList = TaxesManager.getAllTaxes()

        // create new item init with empty tax list
        if (isInCreateMode) {

            taxesCheckedAdapter?.setData(appTaxList)
            binding.activityDetailPluVat.visible = appTaxList.isNotEmpty()

            binding.invalidTaxesLabel.visibility = View.GONE
            binding.llInvalidTaxes.visibility = View.GONE
            return
        }

        // edit existing item, init with applied appliedTaxes
        val itemTaxLabels = item?.tax?.map { it.code } ?: arrayListOf()

        val appliedTaxItems = appTaxList
            .map {
                if (itemTaxLabels.contains(it.code)) {
                    it.isChecked = true
                }
                it
            }

        val appTaxLabels = appTaxList.map { it.code }
        val invalidTaxItems = item?.tax?.filter { appTaxLabels.contains(it.code).not() }
            ?: arrayListOf()

        if (invalidTaxItems.isNotEmpty()) {
            binding.invalidTaxesLabel.visibility = View.VISIBLE
            binding.llInvalidTaxes.visibility = View.VISIBLE

            val invalidTaxSettings = invalidTaxItems.map {
                val taxSettings = TaxesSettings()
                taxSettings.name = it.name
                taxSettings.code = it.code
                taxSettings.isChecked = true
                taxSettings.rate = it.rate
                taxSettings.value = it.value
                taxSettings
            }.toMutableList()

            invalidTaxesCheckedAdapter?.setData(invalidTaxSettings)

        } else {
            binding.invalidTaxesLabel.visibility = View.GONE
            binding.llInvalidTaxes.visibility = View.GONE
        }

        taxesCheckedAdapter?.setData(appliedTaxItems.toMutableList())
        binding.activityDetailPluVat.visible = true
    }

    private fun initTaxesAdapter() {
        binding.activityDetailListVat.layoutManager = LinearLayoutManager(this)
        taxesCheckedAdapter = TaxesCheckedAdapter {
            validateInputForm()
        }
        binding.activityDetailListVat.adapter = taxesCheckedAdapter

        binding.invalidTaxesList.layoutManager = LinearLayoutManager(this)
        invalidTaxesCheckedAdapter = TaxesCheckedAdapter {
            validateInputForm()
        }
        binding.invalidTaxesList.adapter = invalidTaxesCheckedAdapter
    }

    private fun setOnBarcodeInputChangeHandler() {
        binding.activityDetailPluInputBarcode.onTextChanged {

            updatedFieldsMap["barcode"] = (it != item?.barcode)

            isBarcodeValid = it.isEmpty() || it.length > 7

            if (isBarcodeValid) {
                binding.activityDetailPluBarcode.isErrorEnabled = false
            } else {
                binding.activityDetailPluBarcode.error = getString(R.string.error_minimum_eight_characters)
            }

            validateInputForm()
        }
    }

    private fun setOnPriceChangeHandler() {
        binding.activityDetailPluInputPrice.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, 2))
        binding.activityDetailPluInputPrice.onTextChanged {

            if (isNumber(it)) {
                updatedFieldsMap["price"] =
                    (it.roundTo2DecimalPlaces().toDouble() != item?.price?.roundToDecimal()
                        ?.toDouble())
            }

            isPriceValid = it.isNotEmpty() && isNumber(it)
            if (isPriceValid) {
                binding.activityDetailPluPrice.isErrorEnabled = false
            } else {
                binding.activityDetailPluPrice.error = getString(R.string.error_unit_price)
            }
            validateInputForm()
        }
    }

    private fun setOnNameInputChangeHandler() {
        binding.activityDetailPluInputName.onTextChanged {
            updatedFieldsMap["name"] = (it != item?.name)

            isNameValid = it.isNotEmpty() && it.length < 2048

            if (isNameValid) {
                binding.activityDetailPluName.isErrorEnabled = false
            } else {
                binding.activityDetailPluName.error = getString(R.string.error_minimum_one_character)
            }
            validateInputForm()
        }
    }

    private fun setOnFavoriteChangeHandler() {
        val favoriteView = favoriteMenuItem?.actionView as CheckBox
        favoriteView.setOnCheckedChangeListener { _, isChecked ->
            updatedFieldsMap["favorite"] = (isChecked != item?.isFavorite)

            val toastMsg = if (isChecked) {
                getString(R.string.toast_item_marked_favorite)
            } else {
                getString(R.string.toast_item_removed_favorite)
            }

            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()

            validateInputForm()
        }
    }

    private fun isNumber(it: String): Boolean {
        return try {
            it.toDouble() > 0 && it.toDouble().roundToDecimal().isNotEmpty()
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun validateInputForm() {
        val isInputValid = isFormValid()
        if (isInCreateMode) {
            confirmMenuItem?.isEnabled = isInputValid
        }

        if (isInEditMode) {
            confirmMenuItem?.isEnabled = isInputValid and isFormUpdated()
        }
    }

    private fun isFormValid() = hasTaxLabelApplied() && isPriceValid && isNameValid && isBarcodeValid
    private fun isFormUpdated(): Boolean = updatedFieldsMap.any { item -> item.value }

    private fun hasTaxLabelApplied(): Boolean {

        val appliedTaxes = taxesCheckedAdapter?.getAppliedTaxes()
        val appliedInvalidTaxes = invalidTaxesCheckedAdapter?.getAppliedTaxes()

        if (isInEditMode) {
            val appliedLabelsList = appliedTaxes?.toList()?.map { it.code } as Collection<String>
            val appliedInvalidLabelsList = appliedInvalidTaxes?.toList()?.map { it.code } as Collection<String>
            val itemLabelsList = item?.tax?.toList()?.map { it.code } as Collection<String>

            updatedFieldsMap["appliedTaxes"] =
                appliedLabelsList != itemLabelsList || appliedInvalidLabelsList != itemLabelsList
        }

        return appliedTaxes.isNullOrEmpty().not() || appliedInvalidTaxes.isNullOrEmpty().not()
    }

    private fun saveItem() {

        val itemName = binding.activityDetailPluInputName.text.toString()

        val existingItem = Item().queryFirst { equalTo("name", itemName) }

        val hasProductWithSameName = existingItem != null

        if (hasProductWithSameName) {
            longToast(getString(R.string.msg_product_already_exists))
            return
        }

        val newItem = createItem()
        newItem.save()

        hideKeyboard()

        longToast(getString(R.string.toast_new_item_created))

        finishWithResult(newItem.uuid)
    }

    private fun finishWithResult(invoiceId: String) {
        val intent = Intent().apply {
            putExtra(InvoiceFragment.INVOICE_ID_EXTRA, invoiceId)
        }

        setResult(Activity.RESULT_OK, intent)

        finish()
    }

    private fun updateItem() {
        var isListAlreadyCleared = false
        val itemName = binding.activityDetailPluInputName.text.toString()
        val itemBarcode = binding.activityDetailPluInputBarcode.text.toString()
        val itemPrice = binding.activityDetailPluInputPrice.text.toString()
        val itemInFavorites = (favoriteMenuItem?.actionView as CheckBox).isChecked

        val existingItem = Item().queryFirst { equalTo("uuid", item?.uuid) }

        if (existingItem == null) {
            longToast(getString(R.string.error_general))
            return
        }

        existingItem.queryAndUpdate({ equalTo("uuid", item?.uuid) }) {
            it.name = itemName
            it.barcode = itemBarcode
            it.price = itemPrice.roundTo2DecimalPlaces().toDouble()
            it.isFavorite = itemInFavorites

            taxesCheckedAdapter?.getAppliedTaxes()?.let { appliedTaxes ->
                it.tax.clear()
                isListAlreadyCleared = true
                for (taxLabel in appliedTaxes) {
                    it.tax.add(taxLabel)
                }
            }

            invalidTaxesCheckedAdapter?.getAppliedTaxes()?.let { invalidAppliedTaxes ->
                if (!isListAlreadyCleared) {
                    it.tax.clear()
                }

                for (taxLabel in invalidAppliedTaxes) {
                    it.tax.add(taxLabel)
                }
            }

            // Set new values as current values
            item = it
            updatedFieldsMap.clear()

            populateItemFields(it)
        }

        hideKeyboard()

        findViewById<View>(android.R.id.content)?.let {
            Snackbar.make(it, R.string.toast_item_updated, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun createItem(): Item {
        val itemName = binding.activityDetailPluInputName.text.toString()
        val itemBarcode = binding.activityDetailPluInputBarcode.text.toString()
        val itemPrice = binding.activityDetailPluInputPrice.text.toString()
        val itemInFavorites = (favoriteMenuItem?.actionView as CheckBox).isChecked

        val newItem = Item()
        with(newItem) {
            this.name = itemName
            this.barcode = itemBarcode
            this.price = itemPrice.roundTo2DecimalPlaces().toDouble()
            this.isFavorite = itemInFavorites

            taxesCheckedAdapter?.getAppliedTaxes()?.let { appliedTaxes ->
                for (taxLabel in appliedTaxes) {
                    this.tax.add(taxLabel)
                }
            }
        }

        return newItem
    }

}
