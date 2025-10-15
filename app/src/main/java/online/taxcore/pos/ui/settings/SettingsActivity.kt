package online.taxcore.pos.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import online.taxcore.pos.R
import online.taxcore.pos.databinding.SecundaryActivityBinding
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class SettingsActivity : BaseActivity() {

    private lateinit var binding: SecundaryActivityBinding
    private val settingsDashFragment by lazy { SettingsDashFragment() }

    companion object {
        fun start(activity: AppCompatActivity, type: String = "") {
            val intent = Intent(activity, SettingsActivity::class.java).apply {
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
    }

    private fun setActiveFragment() {
        binding.fragmentToolbar.title = getString(R.string.title_settings)
        addFragment(settingsDashFragment, R.id.activityFragment)
    }
}
