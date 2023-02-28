package online.taxcore.pos.ui.settings.about

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.utils.TCUtil
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AboutFragment : Fragment() {

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
        inflater.inflate(R.layout.about_fragment, container, false)

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menu.clear()
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setImage()
        setAppCountry()
        setAppVersion()
        setDateUpdate("January 2021.")
    }

    override fun onResume() {
        super.onResume()
        setImage()
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy")
        val currentCal = Calendar.getInstance()
        return dateFormat.format(currentCal.time)
    }

    @SuppressLint("SetTextI18n")
    private fun setDateUpdate(date: String) {
        fragment_about_app_date_update.text = "Date update: $date"
    }

    @SuppressLint("SetTextI18n")
    private fun setAppCountry() {
        val countryNameFromConfiguration = pref.getString(PrefConstants.COUNTRY, "")
        if (!countryNameFromConfiguration.isNullOrBlank()) {
            fragment_about_app_country.text = countryNameFromConfiguration
        } else {
            val countryNameFromCertificate = pref.getString(PrefConstants.CURRENT_COUNTRY, "")
            fragment_about_app_country.text = countryNameFromCertificate
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAppVersion() {
        val version = BuildConfig.VERSION_NAME
        fragment_about_app_version.text = "Version: $version"
    }

    private fun setImage() {
        val logoFromConfiguration = pref.getString(PrefConstants.LOGO, "")

        val logoImage = if (!logoFromConfiguration.isNullOrBlank()) {
            logoFromConfiguration
        } else {
            val tinOid = pref.getString(PrefConstants.TIN_OID, "").orEmpty()
            TCUtil.getEnvLogo(tinOid)
        }

        val country = pref.getString(PrefConstants.COUNTRY, null)

        Glide.with(this)
            .load(logoImage)
            .error(TCUtil.getCountryImage(country))
            .into(fragment_about_app_image)
    }

}
