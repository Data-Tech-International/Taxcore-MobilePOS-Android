package online.taxcore.pos.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.base_details_activity.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.ui.settings.about.AboutFragment
import online.taxcore.pos.ui.settings.cashiers.CashiersFragment
import online.taxcore.pos.ui.settings.server.SDCConfigureFragment
import online.taxcore.pos.ui.settings.server.SDCServerFragment
import online.taxcore.pos.ui.settings.taxes.TaxesFragment

class SettingsDetailsActivity : BaseActivity() {

    private val taxesFragment by lazy { TaxesFragment() }
    private val aboutFragment by lazy { AboutFragment() }
    private val serverFragment by lazy { SDCServerFragment() }
    private val cashiersFragment by lazy { CashiersFragment() }
    private val sdcConfigureFragment by lazy { SDCConfigureFragment() }

    companion object {

        private const val ACTIVITY_EXTRA = "SETTINGS_FRAGMENT_TYPE"
        const val FRAGMENT_TAX = "SETTINGS_FRAGMENT_TAX"
        const val FRAGMENT_ABOUT = "SETTINGS_FRAGMENT_ABOUT"
        const val FRAGMENT_SERVER = "SETTINGS_FRAGMENT_SERVER"
        const val FRAGMENT_CASHIERS = "SETTINGS_FRAGMENT_CASHIERS"
        const val FRAGMENT_SDC_CONFIGURE = "SDC_CONFIGURE_FRAGMENT"

        fun start(activity: AppCompatActivity, type: String = "") {
            val intent = Intent(activity, SettingsDetailsActivity::class.java).apply {
                putExtra(ACTIVITY_EXTRA, type)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ActivityCompat.startActivity(activity, intent, null)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_details_activity)

        initToolbar()

        setActiveFragment()
    }

    private fun initToolbar() {
        setSupportActionBar(baseToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setActiveFragment() {

        val intentExtra = intent.extras?.getString(ACTIVITY_EXTRA)
        when (intentExtra) {
            FRAGMENT_TAX -> {
                baseToolbar.title = getString(R.string.title_active_taxes)
                addFragment(taxesFragment, R.id.baseFragment)
            }
            FRAGMENT_ABOUT -> {
                baseToolbar.title = getString(R.string.title_about)
                addFragment(aboutFragment, R.id.baseFragment)
            }
            FRAGMENT_SERVER -> {
                baseToolbar.title = getString(R.string.add_v_cdc_server)
                addFragment(serverFragment, R.id.baseFragment)
            }
            FRAGMENT_CASHIERS -> {
                baseToolbar.title = getString(R.string.title_manage_cashiers)
                addFragment(cashiersFragment, R.id.baseFragment)
            }
            FRAGMENT_SDC_CONFIGURE -> {
                addFragment(sdcConfigureFragment, R.id.baseFragment)
            }
            else -> finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

}
