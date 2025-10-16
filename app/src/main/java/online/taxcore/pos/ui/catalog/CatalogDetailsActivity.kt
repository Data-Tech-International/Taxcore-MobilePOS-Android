package online.taxcore.pos.ui.catalog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import online.taxcore.pos.R
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.databinding.CatalogDetailsActivityBinding
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class CatalogDetailsActivity : BaseActivity() {

    private lateinit var binding: CatalogDetailsActivityBinding
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
        binding = CatalogDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()

        setActiveFragment()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        @Suppress("DEPRECATION")
        super.onBackPressed()
        CatalogManager.resetFilter()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.catalogDetailsToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setActiveFragment() {
        when (intent.extras?.getString(ACTIVITY_EXTRA)) {
            CATALOG_SEARCH -> {
                binding.catalogDetailsToolbar.setNavigationIcon(R.drawable.ic_exit)
                binding.catalogDetailsToolbar.title = getString(R.string.title_search_catalog)
                addFragment(catalogFilterFragment, R.id.catalogDetailsFragment)
            }
            else -> {
                binding.catalogDetailsToolbar.title = getString(R.string.title_catalog_items)
                addFragment(catalogListFragment, R.id.catalogDetailsFragment)
            }

        }
    }
}
