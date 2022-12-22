package online.taxcore.pos.ui.journal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.base_details_activity.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class JournalDetailsActivity : BaseActivity() {

    private val journalListFragment by lazy { JournalListFragment() }
    private val journalFilterFragment by lazy { JournalFilterFragment() }

    companion object {

        private const val ACTIVITY_EXTRA = "JOURNAL_FRAGMENT_TYPE"
        private const val FRAGMENT_LIST = "FRAGMENT_LIST"

        fun start(activity: AppCompatActivity, type: String = FRAGMENT_LIST) {
            val intent = Intent(activity, JournalDetailsActivity::class.java).apply {
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

    fun getJournalToolbar(): Toolbar? {
        return baseToolbar
    }

    private fun setActiveFragment() {

        val intentExtra = intent.extras?.getString(ACTIVITY_EXTRA)
        when (intentExtra) {
            FRAGMENT_LIST -> {
                baseToolbar.title = getString(R.string.title_invoices)
                addFragment(journalListFragment, R.id.baseFragment)
            }
            else -> {
                baseToolbar.setNavigationIcon(R.drawable.ic_exit)
                baseToolbar.title = getString(R.string.title_search_journal)
                addFragment(journalFilterFragment, R.id.baseFragment)
            }

        }
    }
}
