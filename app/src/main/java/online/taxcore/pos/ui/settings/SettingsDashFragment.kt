package online.taxcore.pos.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.settings_dashboard_fragment.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data_managers.PrefManager
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_ABOUT
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_CASHIERS
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_SERVER
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_TAX
import javax.inject.Inject

class SettingsDashFragment : Fragment() {

    @Inject
    lateinit var pref: SharedPreferences

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.settings_dashboard_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setOnClickListeners()
    }

    override fun onResume() {
        super.onResume()
        setDashboardButtons()
    }

    private fun setDashboardButtons() {

        val isESDCConfigured = pref.getString(PrefConstants.ESDC_ENDPOINT_URL, "")
        val useVSDCServer = PrefManager.useVSDCServer(pref)

        val foreColor =
            if (AppSession.isAppConfigured) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")

        settingsTaxesButton.isEnabled =
            AppSession.isAppConfigured || (!isESDCConfigured.isNullOrBlank() && (!useVSDCServer && !AppSession.isAppConfigured))
        settingsTaxesButton.foreground = ColorDrawable(
            if (AppSession.isAppConfigured || !isESDCConfigured.isNullOrBlank() && (!useVSDCServer && !AppSession.isAppConfigured)) Color.TRANSPARENT else Color.parseColor(
                "#90EEEEEE"
            )
        )

        settingsCashiersButton.isEnabled = AppSession.isAppConfigured
        settingsCashiersButton.foreground = ColorDrawable(foreColor)

        settingsServerButton.isEnabled = true
        settingsServerButton.foreground = ColorDrawable(Color.TRANSPARENT)

    }

    private fun setOnClickListeners() {
        settingsTaxesButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_TAX)
            }
        }

        settingsCashiersButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_CASHIERS)
            }
        }

        settingsServerButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_SERVER)
            }
        }

        settingsAboutButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_ABOUT)
            }
        }

    }

}
