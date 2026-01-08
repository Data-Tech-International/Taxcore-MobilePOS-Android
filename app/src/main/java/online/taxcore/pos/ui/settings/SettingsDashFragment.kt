package online.taxcore.pos.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import dagger.android.support.AndroidSupportInjection
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.databinding.SettingsDashboardFragmentBinding
import online.taxcore.pos.extensions.baseActivity
import online.taxcore.pos.ui.dashboard.DashboardActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_ABOUT
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_CASHIERS
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_SERVER
import online.taxcore.pos.ui.settings.SettingsDetailsActivity.Companion.FRAGMENT_TAX
import javax.inject.Inject

class SettingsDashFragment : Fragment() {

    private var _binding: SettingsDashboardFragmentBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var prefService: PrefService

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsDashboardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setOnClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        setDashboardButtons()
    }

    private fun setDashboardButtons() {

        val isAppConfigured = prefService.isAppConfigured()

        val foreColor =
            if (isAppConfigured) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")

        binding.settingsTaxesButton.isEnabled = isAppConfigured
        binding.settingsTaxesButton.foreground = ColorDrawable(
            if (isAppConfigured) Color.TRANSPARENT else Color.parseColor("#90EEEEEE")
        )

        binding.settingsCashiersButton.isEnabled = AppSession.isAppConfigured
        binding.settingsCashiersButton.foreground = ColorDrawable(foreColor)

        binding.settingsServerButton.isEnabled = true
        binding.settingsServerButton.foreground = ColorDrawable(Color.TRANSPARENT)

    }

    @SuppressLint("CheckResult")
    private fun setOnClickListeners() {
        binding.settingsTaxesButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_TAX)
            }
        }

        binding.settingsCashiersButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_CASHIERS)
            }
        }

        binding.settingsServerButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_SERVER)
            }
        }

        binding.settingsLanguageButton.setOnClickListener {
            val selectedIndex = when (prefService.loadLocale().toLanguageTag()) {
                "en" -> 0
                "fr" -> 1
                "sr" -> 2
                "bs" -> 3
                else -> 0
            }

            MaterialDialog(requireContext()).show {
                icon(R.drawable.ic_language)
                title(R.string.title_language)
//                message(R.string.title_language)

                listItemsSingleChoice(
                    R.array.languages,
                    initialSelection = selectedIndex
                ) { _, index, _ ->
                    val lng = when (index) {
                        0 -> "en"
                        1 -> "fr"
                        2 -> "sr"
                        3 -> "bs"
                        else -> "en"
                    }

                    prefService.setLanguage(lng)
                    DashboardActivity.start(baseActivity()!!)
                    baseActivity()?.finishAffinity()
                }
            }
        }

        binding.settingsAboutButton.setOnClickListener {
            baseActivity()?.let { activity ->
                SettingsDetailsActivity.start(activity, FRAGMENT_ABOUT)
            }
        }

    }

}
