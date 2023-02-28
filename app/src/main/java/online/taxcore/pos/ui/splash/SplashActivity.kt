package online.taxcore.pos.ui.splash

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.runAsync
import kotlinx.android.synthetic.main.splash_activity.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.api.APIClient
import online.taxcore.pos.api.ApiService
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data_managers.PrefManager
import online.taxcore.pos.data_managers.TaxesManager
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.models.TaxRateResponse
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.ui.dashboard.DashboardActivity
import online.taxcore.pos.utils.TCUtil
import online.taxcore.pos.utils.isOffline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var pref: SharedPreferences
    private var hasCert: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)

        setAppSession()
        setLogoImage()

        if (isOffline()) {
            val errorMessage: Int = if (AppSession.isAppConfigured) {
                R.string.error_no_internet_message
            } else R.string.error_require_internet_message

            AlertDialogHelper.showNotInternetDialog(this, messageText = errorMessage) {
                DashboardActivity.start(this, null)
            }
            return
        }

        startAppConfig()
    }

    private fun setAppSession() {
        hasCert = PrefManager.hasCertInstalled(pref)
        val useESDCServer = PrefManager.useESDCServer(pref)
        val isAppConfigured = PrefManager.isAppConfigured(pref)

        AppSession.let {
            it.isAppConfigured = isAppConfigured
            it.shouldAskForConfiguration = !hasCert && !useESDCServer
        }

        if (!AppSession.isAppConfigured) {
            pref.edit().putBoolean(PrefConstants.USE_VSDC_SERVER, true).apply()
        }
    }

    private fun startAppConfig() {

        splashLoadingBar.visibility = View.VISIBLE

        if (AppSession.isAppConfigured && hasCert) {
            runAsync {
                fetchTaxes()
            }
            return
        }

        Handler().postDelayed({
            DashboardActivity.start(this, null)
        }, 2000)

    }

    private fun createVsdcService(): ApiService? {
        val vsdcEndpoint = pref.getString(PrefConstants.VSDC_ENDPOINT_URL, "").orEmpty()
        val alias = pref.getString(PrefConstants.CERT_ALIAS_VALUE, "").orEmpty()

        return APIClient.vsdc(vsdcEndpoint, alias)
    }

    private fun fetchTaxes() {
        val apiServer = createVsdcService()

        if (apiServer == null) {
            pref.edit().putString(PrefConstants.VSDC_ENDPOINT_URL, "").apply()
            pref.edit().putString(PrefConstants.TIN_OID, "").apply()

            TaxesManager.removeAllTaxes()
            AppSession.isAppConfigured = false
            AppSession.shouldAskForConfiguration = true

            pref.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, false).apply()

            runOnUiThread {
                splashLoadingBar.visibility = View.GONE
                DashboardActivity.start(this@SplashActivity, true)
            }
        }
        apiServer?.getTaxes()?.enqueue(onFetchTaxLabelsCallback)
    }

    private val onFetchTaxLabelsCallback = object : Callback<TaxRateResponse> {
        override fun onFailure(call: Call<TaxRateResponse>?, t: Throwable?) {
            t?.message?.let { errMsg ->
                longToast(errMsg)
                DashboardActivity.start(this@SplashActivity, null)
            }
        }

        override fun onResponse(
            call: Call<TaxRateResponse>?,
            response: Response<TaxRateResponse>?
        ) {
            response?.let { res ->

                if (res.isSuccessful.not()) {
                    val errorMessage = res.errorBody()?.string()
                    errorMessage?.let { _ ->
                        if (errorMessage.startsWith("<!DOCTYPE html", true)) {
                            val parsedMsg =
                                errorMessage.substringAfter("<h3>").substringBefore("</h3>")
                            longToast(parsedMsg)
                        }

                        DashboardActivity.start(this@SplashActivity, null)
                    }
                    return
                }

                res.body()?.let { taxRateResponse ->

                    val certOid = PrefManager.loadCertData(pref).tinOid
                    val taxItems = taxRateResponse.getTaxLabels(certOid) ?: arrayListOf()

                    TaxesManager.replaceActiveTaxItems(taxItems)
                }

                DashboardActivity.start(this@SplashActivity, null)
            }
        }
    }

    private fun setLogoImage() {
        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        val country = pref.getString(PrefConstants.COUNTRY, null)

        val logoImage = if (!country.isNullOrBlank()) {
            TCUtil.getCountryImage(country)
        } else {
            val tinOID = pref.getString(PrefConstants.TIN_OID, "").orEmpty()
            TCUtil.getEnvLogo(tinOID)
        }

        Glide.with(this)
            .load(logoImage)
            .transition(withCrossFade(factory))
            .error(R.drawable.tax_core_logo_splash)
            .into(splashLogoImageView)
    }
}
