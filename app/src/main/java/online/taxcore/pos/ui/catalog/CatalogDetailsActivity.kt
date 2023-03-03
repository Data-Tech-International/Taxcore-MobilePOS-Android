package online.taxcore.pos.ui.catalog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.catalog_details_activity.*
import online.taxcore.pos.R
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class CatalogDetailsActivity : BaseActivity() {

    private val catalogListFragment by lazy { CatalogListFragment() }
    private val catalogFilterFragment by lazy { CatalogFilterFragment() }

    companion object {

        private const val ACTIVITY_EXTRA = "CATALOG_FRAGMENT_TYPE"
        private const val CATALOG_SEARCH = "EXTRA_CATALOG_SEARCH"

        fun start(activity: AppCompatActivity, type: String = "") {
            val intent = Intent(activity, CatalogDetailsActivity::class.java).apply {
                putExtra(ACTIVITY_EXTRA, type)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ActivityCompat.startActivity(activity, intent, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.catalog_details_activity)

        initToolbar()

        setActiveFragment()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        CatalogManager.resetFilter()
    }

    private fun initToolbar() {
        setSupportActionBar(catalogDetailsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setActiveFragment() {
        when (intent.extras?.getString(ACTIVITY_EXTRA)) {
            CATALOG_SEARCH -> {
                catalogDetailsToolbar.setNavigationIcon(R.drawable.ic_exit)
                catalogDetailsToolbar.title = getString(R.string.title_search_catalog)
                addFragment(catalogFilterFragment, R.id.catalogDetailsFragment)
            }
            else -> {
                catalogDetailsToolbar.title = getString(R.string.title_catalog_items)
                addFragment(catalogListFragment, R.id.catalogDetailsFragment)
            }

        }
    }
}
