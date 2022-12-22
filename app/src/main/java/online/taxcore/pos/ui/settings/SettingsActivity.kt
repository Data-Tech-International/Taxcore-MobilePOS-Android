package online.taxcore.pos.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.secundary_activity.*
import online.taxcore.pos.R
import online.taxcore.pos.extensions.addFragment
import online.taxcore.pos.ui.base.BaseActivity

class SettingsActivity : BaseActivity() {

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
    }

    private fun setActiveFragment() {
        fragmentToolbar.title = getString(R.string.title_settings)
        addFragment(settingsDashFragment, R.id.activityFragment)
    }
}
