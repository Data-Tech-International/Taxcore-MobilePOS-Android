package online.taxcore.pos.ui.settings.about

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import dagger.android.support.AndroidSupportInjection
import online.taxcore.pos.BuildConfig
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.databinding.AboutFragmentBinding
import online.taxcore.pos.utils.TCUtil
import javax.inject.Inject

class AboutFragment : Fragment() {

    private var _binding: AboutFragmentBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var prefService: PrefService

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AboutFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu.clear()
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setImage()
        setAppCountry()
        setAppVersion()
        setDateUpdate()
    }

    override fun onResume() {
        super.onResume()
        setImage()
    }

    @SuppressLint("SetTextI18n")
    private fun setDateUpdate() {
        binding.fragmentAboutAppDateUpdate.text = getString(R.string.update_date) + " " + BuildConfig.BUILD_DATE
    }

    @SuppressLint("SetTextI18n")
    private fun setAppCountry() {
        binding.fragmentAboutAppCountry.text = prefService.loadCertCountry()
    }

    @SuppressLint("SetTextI18n")
    private fun setAppVersion() {
        val version = BuildConfig.VERSION_NAME
        binding.fragmentAboutAppVersion.text = getString(R.string.app_version) + " " + version
    }

    private fun setImage() {
        val envLogo = prefService.loadEnvLogo()
        val logoImage = envLogo.ifEmpty {
            val tinOid = prefService.loadTinOid()
            TCUtil.getEnvLogo(tinOid)
        }

        Glide.with(this)
            .load(logoImage)
            .error(R.drawable.tax_core_logo_splash)
                .into(binding.fragmentAboutAppImage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
