package online.taxcore.pos.ui.invoice

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.app_bar_main.*
import online.taxcore.pos.R
import online.taxcore.pos.data.local.InvoiceManager
import online.taxcore.pos.enums.InvoiceActivityType
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity
import java.util.*

class InvoiceActivity : BaseActivity() {

    private val invoiceFragment by lazy { InvoiceFragment() }

    companion object {

        const val ACTIVITY_EXTRA_TYPE = "FRAGMENT_TYPE"
        const val ACTIVITY_EXTRA_VALUE = "EXTRA_VALUE"

        fun start(activity: AppCompatActivity, invoiceActivityType: InvoiceActivityType, value: String? = null) {
            val intent = Intent(activity, InvoiceActivity::class.java).apply {

                putExtra(ACTIVITY_EXTRA_TYPE, invoiceActivityType.value)
                putExtra(ACTIVITY_EXTRA_VALUE, value)

                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ActivityCompat.startActivity(activity, intent, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.invoice_activity)

        initToolbar()

        setDefaultFragment()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        // FIXME: 3/2/21 - This need to reset session on go to background
        // AppSession.resetSession()
        InvoiceManager.selectedItems.clear()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        toolbar.setNavigationIcon(R.drawable.ic_exit)
    }

    private fun setDefaultFragment() {

        val activityExtraType = intent.extras?.getString(ACTIVITY_EXTRA_TYPE) ?: "normal"
        val activityExtraValue = intent.extras?.getString(ACTIVITY_EXTRA_VALUE).orEmpty()

        val fragmentType = InvoiceActivityType.valueOf(activityExtraType.uppercase(Locale.getDefault()))

        toolbar.title = when (fragmentType) {
            InvoiceActivityType.NORMAL -> getString(R.string.invoice_create_invoice)
            InvoiceActivityType.COPY -> getString(R.string.invoice_create_invoice_copy)
            InvoiceActivityType.REFUND -> getString(R.string.invoice_create_invoice_refund)
        }

        val bundle = Bundle()
        bundle.putString("invoiceType", fragmentType.value)
        bundle.putString("invoiceNumber", activityExtraValue)

        invoiceFragment.arguments = bundle
        addFragment(invoiceFragment, R.id.activity_home_fragment)

    }
}
