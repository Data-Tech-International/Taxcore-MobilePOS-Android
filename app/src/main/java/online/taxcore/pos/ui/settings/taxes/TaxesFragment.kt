package online.taxcore.pos.ui.settings.taxes

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.taxes_fragment.*
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.local.TaxesManager
import javax.inject.Inject

class TaxesFragment : Fragment() {

    @Inject
    lateinit var prefService: PrefService
    private var useESDC: Boolean = false

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useESDC = prefService.useESDCServer()
    }

    private var taxesAdapter: TaxesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.taxes_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initList()
        if (useESDC) {
            tv_taxes_are_configured.text =
                context?.getString(R.string.taxes_are_automatically_configured_from_server_settings)
        } else {
            tv_taxes_are_configured.text = context?.getString(R.string.taxes_info)
        }
    }

    override fun onResume() {
        super.onResume()
        taxesAdapter?.setData(TaxesManager.getAllTaxes())
    }

    private fun initList() {
        taxesAdapter = TaxesAdapter()
        taxesRecyclerView.layoutManager = LinearLayoutManager(activity)
        taxesRecyclerView.adapter = taxesAdapter
    }


}
