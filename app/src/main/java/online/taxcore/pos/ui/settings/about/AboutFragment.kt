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
import kotlinx.android.synthetic.main.about_fragment.*
import online.taxcore.pos.BuildConfig
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.utils.TCUtil
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AboutFragment : Fragment() {

    @Inject
    lateinit var prefService: PrefService

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.about_fragment, container, false)

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu.clear()
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setImage()
        setAppCountry()
        setAppVersion()
        setDateUpdate(getCurrentDate())
    }

    override fun onResume() {
        super.onResume()
        setImage()
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("MM.yyyy")
        val currentCal = Calendar.getInstance()
        return dateFormat.format(currentCal.time)
    }

    @SuppressLint("SetTextI18n")
    private fun setDateUpdate(date: String) {
        fragment_about_app_date_update.text = getString(R.string.update_date) + " " + date
    }

    @SuppressLint("SetTextI18n")
    private fun setAppCountry() {
        fragment_about_app_country.text = prefService.loadCertCountry()
    }

    @SuppressLint("SetTextI18n")
    private fun setAppVersion() {
        val version = BuildConfig.VERSION_NAME
        fragment_about_app_version.text = getString(R.string.app_version) + " " + version
    }

    private fun setImage() {
        val envLogo = prefService.loadEnvLogo()
        val logoImage = if (envLogo.isNotEmpty()) {
            envLogo
        } else {
            val tinOid = prefService.loadTinOid()
            TCUtil.getEnvLogo(tinOid)
        }

        Glide.with(this)
                .load(logoImage)
                .error(R.drawable.tax_core_logo_splash)
                .into(fragment_about_app_image)
    }

}
