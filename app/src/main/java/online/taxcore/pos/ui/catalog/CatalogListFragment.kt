package online.taxcore.pos.ui.catalog

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pawegio.kandroid.onQueryChange
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.catalog_list_fragment.*
import online.taxcore.pos.R
import online.taxcore.pos.data_managers.CatalogManager
import online.taxcore.pos.data_managers.TaxesManager
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.models.Item
import online.taxcore.pos.utils.hideKeyboard

class CatalogListFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.catalog_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initUI()
        initRecyclerView()

        setOnClickListeners()
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
        filterFab.setOnClickListener {
            replaceFragment(R.id.catalogDetailsFragment, CatalogFilterFragment())
        }

        catalogTryAgainButton.setOnClickListener {
            replaceFragment(R.id.catalogDetailsFragment, CatalogFilterFragment())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)

        searchMenuItem = menu.findItem(R.id.actionSearch)
        searchMenuItem?.isVisible = isFilterMode.not()

        if (isFilterMode.not()) {
            val searchView = searchMenuItem?.actionView as SearchView
            searchView.queryHint = "Search by name or EAN"
            searchView.onQueryChange { query ->
                catalogAdapter?.changeDataByFilter(query)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.hideKeyboard()
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

        catalogRecyclerView.layoutManager = LinearLayoutManager(activity)
        catalogRecyclerView.adapter = catalogAdapter
    }

    private fun setCatalogData() {
        val catalogItems = getCatalogItems()

        catalogAdapter?.setData(catalogItems)

        catalogItems.isEmpty().let { empty ->

            if (empty and isFilterMode.not()) {
                baseActivity()?.finish()
                return@let
            }

            filterFab.visible = empty.not() and isFilterMode

            catalogNoResultsLayout.visible = empty
            catalogRecyclerView.visible = empty.not()
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
