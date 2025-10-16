package online.taxcore.pos.ui.catalog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.AndroidSupportInjection
import online.taxcore.pos.R
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.local.TaxesManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.databinding.CatalogListFragmentBinding
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.utils.hideKeyboard
import online.taxcore.pos.utils.onQueryChange

class CatalogListFragment : Fragment() {

    private var _binding: CatalogListFragmentBinding? = null
    private val binding get() = _binding!!

    private var searchMenuItem: MenuItem? = null
    private var isFilterMode: Boolean = false

    private var catalogAdapter: CatalogAdapter? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CatalogListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initUI()
        initRecyclerView()

        setOnClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        setCatalogData()
    }

    private fun initUI() {
        arguments?.getBoolean("isSearch")?.let {
            isFilterMode = it
        }
    }

    private fun setOnClickListeners() {
        binding.filterFab.setOnClickListener {
            replaceFragment(R.id.catalogDetailsFragment, CatalogFilterFragment())
        }

        binding.catalogTryAgainButton.setOnClickListener {
            replaceFragment(R.id.catalogDetailsFragment, CatalogFilterFragment())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)

        searchMenuItem = menu.findItem(R.id.actionSearch)
        searchMenuItem?.isVisible = isFilterMode.not()

        if (isFilterMode.not()) {
            val searchView = searchMenuItem?.actionView as SearchView
            searchView.queryHint = getString(R.string.search_by_name_or_EAN)
            searchView.onQueryChange { query ->
                catalogAdapter?.changeDataByFilter(query)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.hideKeyboard()
                @Suppress("DEPRECATION")
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initRecyclerView() {
        val appTaxLabels = TaxesManager.getAllTaxes().map { it.code }
        catalogAdapter = context?.let {
            CatalogAdapter(it, appTaxLabels) { setCatalogData() }
        }

        binding.catalogRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.catalogRecyclerView.adapter = catalogAdapter
    }

    private fun setCatalogData() {
        val catalogItems = getCatalogItems()

        catalogAdapter?.setData(catalogItems)

        catalogItems.isEmpty().let { empty ->

            if (empty and isFilterMode.not()) {
                baseActivity()?.finish()
                return@let
            }

            binding.filterFab.visible = empty.not() and isFilterMode

            binding.catalogNoResultsLayout.visible = empty
            binding.catalogRecyclerView.visible = empty.not()
        }
    }

    private fun getCatalogItems(): MutableList<Item> {
        return if (isFilterMode) {
            CatalogManager.loadFilteredItems()
        } else {
            CatalogManager.loadCatalogItems()
        }
    }

}
