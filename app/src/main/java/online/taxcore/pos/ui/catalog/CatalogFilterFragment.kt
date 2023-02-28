package online.taxcore.pos.ui.catalog

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.vicpin.krealmextensions.queryAll
import kotlinx.android.synthetic.main.catalog_filters_fragment.*
import online.taxcore.pos.R
import online.taxcore.pos.data_managers.CatalogManager
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.models.TaxesSettings
import online.taxcore.pos.ui.common.TaxesCheckedAdapter
import online.taxcore.pos.utils.hideKeyboard

class CatalogFilterFragment : Fragment() {

    private var confirmFilterItem: MenuItem? = null
    private var taxesCheckedAdapter: TaxesCheckedAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.catalog_filters_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFields()
        initTaxesAdapter()

        setOnClickListeners()
        setOnInputChangeListeners()
    }

    override fun onResume() {
        super.onResume()
        populateTaxLabels()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.filter, menu)

        confirmFilterItem = menu.findItem(R.id.actionConfirmFilter)

        validateFilters()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            CatalogManager.resetFilter()

            baseActivity()?.onBackPressed()
            true
        }
        R.id.actionConfirmFilter -> {
            applySearchFilter()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun initFields() {

        with(CatalogManager) {
            catalogFilterItemNameInput.setText(this.itemName.trim())
            catalogFilterUnitPriceInput.setText(this.unitPrice.trim())
            catalogFilterGTINInput.setText(this.gtinNum.trim())
        }

        validateFilters()
    }

    private fun initTaxesAdapter() {
        catalogFilterTaxesRecyclerView.layoutManager = LinearLayoutManager(baseActivity())
        taxesCheckedAdapter = TaxesCheckedAdapter {
            validateFilters()
        }
        catalogFilterTaxesRecyclerView.adapter = taxesCheckedAdapter
    }

    private fun setOnClickListeners() {

        catalogFilterResetButton.setOnClickListener {
            resetFilterFields()
        }
    }

    private fun populateTaxLabels() {
        val taxesSettingsList = TaxesSettings().queryAll().toMutableList()

        val appliedTaxSettings = taxesSettingsList
            .map {
                if (CatalogManager.appliedTaxes.contains(it.code)) {
                    it.isChecked = true
                }
                it
            }

        taxesCheckedAdapter?.setData(appliedTaxSettings.toMutableList())
    }

    private fun resetFilterFields() {

        catalogFilterResetButton.isEnabled = false

        catalogFilterItemNameInput.setText("")
        catalogFilterGTINInput.setText("")
        catalogFilterUnitPriceInput.setText("")

        val taxesSettingsList = TaxesSettings().queryAll().toMutableList()
        taxesCheckedAdapter?.setData(taxesSettingsList)

        CatalogManager.resetFilter()
    }

    private fun setOnInputChangeListeners() {
        catalogFilterItemNameInput.onTextChanged {
            validateFilters()
        }

        catalogFilterGTINInput.onTextChanged {
            validateFilters()
        }

        catalogFilterUnitPriceInput.onTextChanged {
            validateFilters()
        }
    }

    private fun hasTaxLabelApplied(): Boolean {
        val appliedTaxes = taxesCheckedAdapter?.getAppliedTaxes()
        return appliedTaxes.isNullOrEmpty().not()
    }

    private fun validateFilters() {
        val itemName = catalogFilterItemNameInput.text.toString().trim()
        val unitPrice = catalogFilterUnitPriceInput.text.toString().trim()
        val gtinNumber = catalogFilterGTINInput.text.toString().trim()

        val isSearchEnabled = itemName.isNotBlank() or
                unitPrice.isNotEmpty() or
                gtinNumber.isNotEmpty() or
                hasTaxLabelApplied()

        confirmFilterItem?.isEnabled = isSearchEnabled
        catalogFilterResetButton.isEnabled = isSearchEnabled
    }

    private fun applySearchFilter() {

        val appliedTaxes = taxesCheckedAdapter?.getAppliedTaxes()?.map { it.code }?.toTypedArray()
            ?: emptyArray()

        with(CatalogManager) {
            itemName = catalogFilterItemNameInput.text.toString()
            unitPrice = catalogFilterUnitPriceInput.text.toString()
            gtinNum = catalogFilterGTINInput.text.toString()
            this.appliedTaxes = appliedTaxes
        }

        baseActivity()?.hideKeyboard()

        val args = Bundle().apply { putBoolean("isSearch", true) }

        val catalogListFragment = CatalogListFragment()
        catalogListFragment.arguments = args


        replaceFragment(R.id.catalogDetailsFragment, catalogListFragment)
    }

}
