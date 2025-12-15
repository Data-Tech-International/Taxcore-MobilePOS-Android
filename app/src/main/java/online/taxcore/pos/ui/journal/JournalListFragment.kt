package online.taxcore.pos.ui.journal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import io.realm.Sort
import online.taxcore.pos.R
import online.taxcore.pos.data.local.JournalManager
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.databinding.JournalFragmentBinding
import online.taxcore.pos.events.ShowFiscalInvoicedialog
import online.taxcore.pos.events.ShowInvoiceActivity
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.extensions.replaceFragment
import online.taxcore.pos.extensions.visible
import online.taxcore.pos.ui.invoice.FiscalInvoiceFragment
import online.taxcore.pos.ui.invoice.InvoiceActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class JournalListFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var _binding: JournalFragmentBinding? = null
    private val binding get() = _binding!!

    private var isFilterMode: Boolean = false

    private var journalAdapter: JournalAdapter? = null

    private var sortJournalsMenuItem: MenuItem? = null

    private lateinit var realm: Realm
    private var journalResults: List<Journal>? = null

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = JournalFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        realm = Realm.getDefaultInstance()
        initUI()
        initJournalRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        updateJournalData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        journalResults = null
        if (::realm.isInitialized && !realm.isClosed) {
            realm.close()
        }
        _binding = null
    }

    private fun initUI() {
        arguments?.getBoolean("isSearch")?.let {
            isFilterMode = it
            binding.filterFab.visible = it
        }

        binding.filterFab.setOnClickListener {
            replaceFragment(R.id.baseFragment, JournalFilterFragment())
        }

        binding.journalTryAgainButton.setOnClickListener {
            replaceFragment(R.id.baseFragment, JournalFilterFragment())
        }
    }

    private fun initJournalRecyclerView() {
        journalAdapter = JournalAdapter()

        binding.journalRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.journalRecyclerView.adapter = journalAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.journal_list, menu)

        sortJournalsMenuItem = menu.findItem(R.id.actionSortJournals)
        (sortJournalsMenuItem?.actionView as AppCompatSpinner).onItemSelectedListener = this

        updateJournalData()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val journalItems = when (position) {
            0 -> getJournalItems(Sort.DESCENDING)
            else -> getJournalItems(Sort.ASCENDING)
        }

        journalAdapter?.setData(journalItems)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        throw UnsupportedOperationException("not implemented")
    }

    private fun getJournalItems(sort: Sort = Sort.DESCENDING): List<Journal> {
        journalResults = if (isFilterMode) {
            JournalManager.queryFilteredItems(realm, sort)
        } else {
            JournalManager.queryJournalItems(realm, sort)
        }
        return journalResults!!
    }

    private fun updateJournalData() {
        val journalItems = getJournalItems()

        journalAdapter?.setData(journalItems)

        journalItems.isEmpty().let { empty ->
            sortJournalsMenuItem?.isVisible = empty.not()

            binding.filterFab.visible = empty.not() and isFilterMode

            binding.journalNoResultsLayout.visible = empty
            binding.journalRecyclerView.visible = empty.not()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ShowFiscalInvoicedialog) {
        FiscalInvoiceFragment.showFiscalDialog(activity?.supportFragmentManager, event.id, event.qrCode, event.message, event.url)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun showInvoiceActivity(event: ShowInvoiceActivity) {
        InvoiceActivity.start(baseActivity()!!, event.invoiceActivityType, event.invoiceId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun copyInvoiceNumber(invoiceNumber: String) {

        val clipboard = (activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        val clip: ClipData = ClipData.newPlainText("JournalID", invoiceNumber)

        clipboard.setPrimaryClip(clip)

        val outputMsg = getString(R.string.toast_inv_no_copied, invoiceNumber)
        Toast.makeText(requireContext(), outputMsg, Toast.LENGTH_SHORT).show()
    }

}
