package online.taxcore.pos.ui.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.vicpin.krealmextensions.queryAll
import online.taxcore.pos.R
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.realm.TaxesSettings
import online.taxcore.pos.databinding.CatalogFiltersFragmentBinding
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.onTextChanged
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.ui.common.TaxesCheckedAdapter
import online.taxcore.pos.utils.hideKeyboard

class CatalogFilterFragment : Fragment() {

    private var _binding: CatalogFiltersFragmentBinding? = null
    private val binding get() = _binding!!

    private var confirmFilterItem: MenuItem? = null
    private var taxesCheckedAdapter: TaxesCheckedAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CatalogFiltersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFields()
        initTaxesAdapter()

        setOnClickListeners()
        setOnInputChangeListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

            @Suppress("DEPRECATION")
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
            binding.catalogFilterItemNameInput.setText(this.itemName.trim())
            binding.catalogFilterUnitPriceInput.setText(this.unitPrice.trim())
            binding.catalogFilterGTINInput.setText(this.gtinNum.trim())
        }

        validateFilters()
    }

    private fun initTaxesAdapter() {
        binding.catalogFilterTaxesRecyclerView.layoutManager = LinearLayoutManager(baseActivity())
        taxesCheckedAdapter = TaxesCheckedAdapter {
            validateFilters()
        }
        binding.catalogFilterTaxesRecyclerView.adapter = taxesCheckedAdapter
    }

    private fun setOnClickListeners() {

        binding.catalogFilterResetButton.setOnClickListener {
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

        binding.catalogFilterResetButton.isEnabled = false

        binding.catalogFilterItemNameInput.setText("")
        binding.catalogFilterGTINInput.setText("")
        binding.catalogFilterUnitPriceInput.setText("")

        val taxesSettingsList = TaxesSettings().queryAll().toMutableList()
        taxesCheckedAdapter?.setData(taxesSettingsList)

        CatalogManager.resetFilter()
    }

    private fun setOnInputChangeListeners() {
        binding.catalogFilterItemNameInput.onTextChanged {
            validateFilters()
        }

        binding.catalogFilterGTINInput.onTextChanged {
            validateFilters()
        }

        binding.catalogFilterUnitPriceInput.onTextChanged {
            validateFilters()
        }
    }

    private fun hasTaxLabelApplied(): Boolean {
        val appliedTaxes = taxesCheckedAdapter?.getAppliedTaxes()
        return appliedTaxes.isNullOrEmpty().not()
    }

    private fun validateFilters() {
        val itemName = binding.catalogFilterItemNameInput.text.toString().trim()
        val unitPrice = binding.catalogFilterUnitPriceInput.text.toString().trim()
        val gtinNumber = binding.catalogFilterGTINInput.text.toString().trim()

        val isSearchEnabled = itemName.isNotBlank() or
                unitPrice.isNotEmpty() or
                gtinNumber.isNotEmpty() or
                hasTaxLabelApplied()

        confirmFilterItem?.isEnabled = isSearchEnabled
        binding.catalogFilterResetButton.isEnabled = isSearchEnabled
    }

    private fun applySearchFilter() {

        val appliedTaxes = taxesCheckedAdapter?.getAppliedTaxes()?.map { it.code }?.toTypedArray()
            ?: emptyArray()

        with(CatalogManager) {
            itemName = binding.catalogFilterItemNameInput.text.toString()
            unitPrice = binding.catalogFilterUnitPriceInput.text.toString()
            gtinNum = binding.catalogFilterGTINInput.text.toString()
            this.appliedTaxes = appliedTaxes
        }

        baseActivity()?.hideKeyboard()

        val args = Bundle().apply { putBoolean("isSearch", true) }

        val catalogListFragment = CatalogListFragment()
        catalogListFragment.arguments = args


        replaceFragment(R.id.catalogDetailsFragment, catalogListFragment)
    }

}
