package online.taxcore.pos.ui.catalog

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.secundary_activity.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class CatalogActivity : BaseActivity() {

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
        setContentView(R.layout.secundary_activity)

        initToolbar()

        setActiveFragment()
    }

    private fun initToolbar() {
        setSupportActionBar(fragmentToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        fragmentToolbar.title = getString(R.string.catalog)
    }

    private fun setActiveFragment() {
        addFragment(catalogDashFragment, R.id.activityFragment)
    }
}
