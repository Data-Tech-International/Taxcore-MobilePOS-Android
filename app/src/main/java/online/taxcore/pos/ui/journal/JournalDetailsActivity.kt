package online.taxcore.pos.ui.journal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import online.taxcore.pos.R
import online.taxcore.pos.databinding.BaseDetailsActivityBinding
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class JournalDetailsActivity : BaseActivity() {

    private lateinit var binding: BaseDetailsActivityBinding
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
        binding = BaseDetailsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        setActiveFragment()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.baseToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    fun getJournalToolbar(): Toolbar? {
        return binding.baseToolbar
    }

    private fun setActiveFragment() {

        val intentExtra = intent.extras?.getString(ACTIVITY_EXTRA)
        when (intentExtra) {
            FRAGMENT_LIST -> {
                binding.baseToolbar.title = getString(R.string.title_invoices)
                addFragment(journalListFragment, R.id.baseFragment)
            }
            else -> {
                binding.baseToolbar.setNavigationIcon(R.drawable.ic_exit)
                binding.baseToolbar.title = getString(R.string.title_search_journal)
                addFragment(journalFilterFragment, R.id.baseFragment)
            }

        }
    }
}
