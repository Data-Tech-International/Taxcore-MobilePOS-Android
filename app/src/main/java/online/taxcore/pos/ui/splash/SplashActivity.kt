package online.taxcore.pos.ui.splash

import android.os.Bundle
import android.os.Handler
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.pawegio.kandroid.longToast
import kotlinx.android.synthetic.main.splash_activity.*
import online.taxcore.pos.AppSession
import online.taxcore.pos.R
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.data.api.APIClient
import online.taxcore.pos.data.api.ApiService
import online.taxcore.pos.data.api.handleCertificates.CertAuthority
import online.taxcore.pos.data.local.CertManager
import online.taxcore.pos.data.local.TaxesManager
import online.taxcore.pos.data.models.StatusResponse
import online.taxcore.pos.data.services.AppService
import online.taxcore.pos.helpers.AlertDialogHelper
import online.taxcore.pos.ui.base.BaseActivity
import online.taxcore.pos.ui.dashboard.DashboardActivity
import online.taxcore.pos.utils.TCUtil
import online.taxcore.pos.utils.isOffline
import org.jetbrains.anko.ctx
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class SplashActivity : BaseActivity() {

    lateinit var prefService: PrefService
    private var hasCert: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)

        prefService = try {
            PrefService(this)
        } catch (ex: SecurityException) {
            ctx.cacheDir.deleteRecursively()
            PrefService(this)
        }

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
        hasCert = prefService.hasCertInstalled()
        val useESDCServer = prefService.useESDCServer()
        val isAppConfigured = prefService.isAppConfigured()

        AppSession.let {
            it.isAppConfigured = isAppConfigured
            it.shouldAskForConfiguration = !hasCert && !useESDCServer
        }

        if (AppSession.isAppConfigured.not()) {
            prefService.setUseVsdcServer()
        }
    }

    private fun startAppConfig() {

        splashLoadingBar.visibility = View.VISIBLE

        val isConfigured = prefService.isAppConfigured()
        val hasCert = prefService.hasCertInstalled()

        if (isConfigured && hasCert) {
            fetchTaxes()
            return
        }

        Handler().postDelayed({
            DashboardActivity.start(this, null)
        }, 2000)
    }

    @Throws(IOException::class)
    private fun createVsdcService(): ApiService? {

        val activeCert = prefService.loadActiveCertName()
        val certPass = prefService.loadPfxPass(activeCert)

        val cert = CertManager.loadCert(activeCert)

        val certAuthority = CertAuthority.certificateParams(cert!!.pfxData, certPass)
        return APIClient.vsdc(certAuthority)
    }

    private fun fetchTaxes() {
        try {
            val apiServer = createVsdcService()

            if (apiServer == null) {
                runOnUiThread {
                    splashLoadingBar.visibility = View.GONE
                    DashboardActivity.start(this@SplashActivity, true)
                }
                return
            }

            val pac = prefService.loadGlobalPac()
            if (pac.isNotEmpty()) {
                apiServer.getTaxes(pac).enqueue(onFetchTaxLabelsCallback)
            }
        } catch (error: IOException) {
            runOnUiThread {
                Handler().postDelayed({
                    DashboardActivity.start(this, null)
                }, 2000)
            }
        }
    }

    private val onFetchTaxLabelsCallback = object : Callback<StatusResponse> {
        override fun onFailure(call: Call<StatusResponse>?, t: Throwable?) {
            t?.message?.let { errMsg ->

                AppService.resetConfiguration {
                    prefService.removeActiveCertName()
                    prefService.removeConfiguration()
                }

                longToast(R.string.error_general)
                DashboardActivity.start(this@SplashActivity, null)
            }
        }

        override fun onResponse(call: Call<StatusResponse>?, response: Response<StatusResponse>?) {
            response?.let { res ->

                if (res.isSuccessful.not()) {
                    val errorMessage = res.errorBody()?.string()
                    errorMessage?.let { _ ->
                        if (errorMessage.startsWith("<!DOCTYPE html", true)) {
                            val parsedMsg =
                                errorMessage.substringAfter("<h3>").substringBefore("</h3>")
                            longToast(parsedMsg)
                        }

                        AppService.resetConfiguration {
                            prefService.removeConfiguration()
                            prefService.setAppConfigured(false)
                        }

                        longToast(R.string.error_general)

                        DashboardActivity.start(this@SplashActivity, null)
                    }
                    return
                }

                res.body()?.let { taxRateResponse ->

                    val certOid = prefService.loadCertData().tinOid
                    val taxItems = taxRateResponse.getTaxLabels(certOid) ?: arrayListOf()

                    TaxesManager.replaceActiveTaxItems(taxItems)
                }

                DashboardActivity.start(this@SplashActivity, null)
            }
        }
    }

    private fun setLogoImage() {
        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        val envLogo = prefService.loadEnvLogo()

        val logoImage = if (envLogo.isNotBlank()) {
            envLogo
        } else {
            val tinOID = prefService.loadTinOid()
            TCUtil.getEnvLogo(tinOID)
        }

        Glide.with(this)
            .load(logoImage)
            .transition(withCrossFade(factory))
            .error(R.drawable.tax_core_logo_splash)
            .into(splashLogoImageView)
    }
}
