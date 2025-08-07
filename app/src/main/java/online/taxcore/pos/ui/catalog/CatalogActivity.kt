package online.taxcore.pos.ui.catalog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import online.taxcore.pos.R
import online.taxcore.pos.databinding.SecundaryActivityBinding
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class CatalogActivity : BaseActivity() {

    private lateinit var binding: SecundaryActivityBinding
    private val catalogDashFragment by lazy { CatalogDashFragment() }

    companion object {

        private const val ACTIVITY_EXTRA = "CATALOG_FRAGMENT_TYPE"

        fun start(activity: AppCompatActivity, type: String) {
            val intent = Intent(activity, CatalogActivity::class.java).apply {
                putExtra(ACTIVITY_EXTRA, type)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ActivityCompat.startActivity(activity, intent, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SecundaryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()

        setActiveFragment()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.fragmentToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.fragmentToolbar.title = getString(R.string.catalog)
    }

    private fun setActiveFragment() {
        addFragment(catalogDashFragment, R.id.activityFragment)
    }
}
