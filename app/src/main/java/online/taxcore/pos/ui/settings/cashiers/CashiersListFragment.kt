package online.taxcore.pos.ui.settings.cashiers

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pawegio.kandroid.onQueryChange
import com.vicpin.krealmextensions.queryAll
import kotlinx.android.synthetic.main.cashiers_list_fragment.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.models.Cashier
import online.taxcore.pos.utils.hideKeyboard

class CashiersListFragment : Fragment() {

    private var searchMenuItem: MenuItem? = null
    private var cashiersAdapter: CashiersAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.cashiers_list_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) =
        initList()

    override fun onResume() {
        super.onResume()
        cashiersAdapter?.setData(Cashier().queryAll().toMutableList())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)

        searchMenuItem = menu.findItem(R.id.actionSearch)

        val searchView = searchMenuItem?.actionView as SearchView
        searchView.queryHint = "Search by Name or ID"
        searchView.onQueryChange { query ->
            cashiersAdapter?.changeDataByFilter(query)
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

    private fun initList() {
        cashiersAdapter = CashiersAdapter()

        cashiersRecyclerView.layoutManager = LinearLayoutManager(baseActivity())
        cashiersRecyclerView.adapter = cashiersAdapter

    }
}
