package online.taxcore.pos.data_managers

import android.content.SharedPreferences
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.models.CertData
import java.security.cert.X509Certificate
import java.util.*

object PrefManager {

    fun hasCertInstalled(pref: SharedPreferences): Boolean {
        val vsdcUrl = pref.getString(PrefConstants.VSDC_ENDPOINT_URL, "").orEmpty()
        val tinOid = pref.getString(PrefConstants.TIN_OID, "").orEmpty()

        return vsdcUrl.isNotEmpty() && tinOid.isNotEmpty()
    }

    fun saveCertificateData(
        pref: SharedPreferences,
        cert: X509Certificate,
        alias: String
    ): CertData {
        val certData = CertData.extract(cert)

        // Save app config from certificate
        pref.edit().apply {
            putBoolean(PrefConstants.USE_VSDC_SERVER, true)
            putString(PrefConstants.CERT_ALIAS_VALUE, alias)
            putString(PrefConstants.VSDC_ENDPOINT_URL, certData.vsdcEndpoint)
            putString(PrefConstants.TIN_OID, certData.tinOid)
            putString(PrefConstants.CURRENT_COUNTRY, certData.countryName)
            putString(PrefConstants.CERT_SUBJECT, certData.subject)

            putString(PrefConstants.LOGO, "")
            putString(PrefConstants.COUNTRY, "")
        }.apply()

        return certData
    }

    fun loadCertData(pref: SharedPreferences): CertData {
        val certSubject = pref.getString(PrefConstants.CERT_SUBJECT, "").orEmpty()
        val vsdcUrl = pref.getString(PrefConstants.VSDC_ENDPOINT_URL, "").orEmpty()
        val tinOid = pref.getString(PrefConstants.TIN_OID, "").orEmpty()
        val country = pref.getString(PrefConstants.CURRENT_COUNTRY, "").orEmpty()

        return CertData(
            subject = certSubject,
            vsdcEndpoint = vsdcUrl,
            tinOid = tinOid,
            countryName = country
        )
    }

    fun useESDCServer(pref: SharedPreferences): Boolean {
        return pref.getBoolean(PrefConstants.USE_ESDC_SERVER, false)
    }

    fun isESDCServerConfigured(pref: SharedPreferences): Boolean {
        return pref.getString(PrefConstants.ESDC_ENDPOINT_URL, null).isNullOrBlank().not()
    }

    fun isAppConfigured(pref: SharedPreferences): Boolean {
        return pref.getBoolean(PrefConstants.IS_APP_CONFIGURED, false)
    }

    fun useVSDCServer(pref: SharedPreferences): Boolean {
        return pref.getBoolean(PrefConstants.USE_VSDC_SERVER, false)
    }

    fun saveCredentialsTime(pref: SharedPreferences) {
        pref.edit().putLong(PrefConstants.LAST_CREDENTIALS_TIME, Date().time).apply()
    }

    fun getCredentialsTime(pref: SharedPreferences): Long {
        return pref.getLong(PrefConstants.LAST_CREDENTIALS_TIME, Date().time)
    }
}
