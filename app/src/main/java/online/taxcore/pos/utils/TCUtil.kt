package online.taxcore.pos.utils

import androidx.annotation.DrawableRes
import online.taxcore.pos.R
import online.taxcore.pos.constants.CertConstants
import online.taxcore.pos.constants.CountryConstants
import java.security.cert.X509Certificate
import java.util.*

/**
 * TaxCore Helpers
 */
object TCUtil {

    @DrawableRes
    fun getEnvLogo(oid: String, horizontal: Boolean = false): Int {

        if (oid.isEmpty()) {
            return R.drawable.tax_core_logo_splash
        }

        val certOIDPrefix = "${CertConstants.OID_PREFIX}."
        val (_, envId) = oid.substringAfter(certOIDPrefix).split('.')
        return when (envId) {
            CertConstants.OID_Y_FIJI -> if (horizontal) R.drawable.img_fiji_logo_horizontal else R.drawable.fiji_logo_splash
            CertConstants.OID_Y_SAMOA -> R.drawable.img_ws_logo
            CertConstants.OID_Y_WA_US -> if (horizontal) R.drawable.logo_text else R.drawable.tax_core_logo_splash
            else -> if (horizontal) R.drawable.logo_text else R.drawable.tax_core_logo_splash
        }
    }

    @DrawableRes
    fun getCountryImage(country: String?, horizontal: Boolean = false): Int {
        return when (country) {
            CountryConstants.FIJI -> if (horizontal) R.drawable.img_fiji_logo_horizontal else R.drawable.fiji_logo_splash
            CountryConstants.SAMOA -> R.drawable.samoa_logo_app
            else -> if (horizontal) R.drawable.logo_text else R.drawable.tax_core_logo_splash
        }
    }

    fun getCurrency(oid: String): String {

        if (oid.isEmpty()) {
            return ""
        }

        val certOIDPrefix = "${CertConstants.OID_PREFIX}."
        val (_, envId) = oid.substringAfter(certOIDPrefix).split('.')
        return when (envId) {
            CertConstants.OID_Y_FIJI -> "FJ\$"
            CertConstants.OID_Y_SAMOA -> "WS\$"
            CertConstants.OID_Y_WA_US -> "\\US\$"
            CertConstants.OID_Y_SERBIA -> "RSD"
            else -> ""
        }
    }

    fun getCurrencyBy(countryCode: String?): String {
        if (countryCode.isNullOrEmpty()) {
            return ""
        }

        return when (countryCode.toUpperCase(Locale.getDefault())) {
            "FJ" -> "FJ\$"
            "WS" -> "WS\$"
            "US" -> "\\US\$"
            "RS" -> "RSD"
            else -> ""
        }
    }

    private fun getOidList(cert: X509Certificate): List<String> {
        return CertExtensionsUtils.getExtensionList(cert)
                .map { it["oid"] as String }
                .filter { it.startsWith(CertConstants.OID_PREFIX) }
    }

    @Throws(IllegalArgumentException::class)
    fun getVSDCEndpoint(cert: X509Certificate): String {
        val urlOid = findVsdcApiOid(cert)
        val vsdcUrl = CertExtensionsUtils.getExtensionValue(cert, urlOid) as String
        return vsdcUrl.trim()
    }

    private fun findVsdcApiOid(cert: X509Certificate): String? = getOidList(cert)
            .find { oid -> oid.endsWith(CertConstants.OID_Z_VSDC_URL) }
            .orEmpty()

    fun getAPIEndpoint(cert: X509Certificate): String {
        val apiOid = findBackendAPIOid(cert)

        val apiUrl = CertExtensionsUtils.getExtensionValue(cert, apiOid) as String

        return apiUrl.trim()
    }

    private fun findBackendAPIOid(cert: X509Certificate): String? = getOidList(cert)
            .find { oid -> oid.endsWith(CertConstants.OID_Z_BACKEND_API) }
            .orEmpty()

    fun getCustomerTIN(cert: X509Certificate): String {
        val tinOid = findTinOid(cert)

        val tin = CertExtensionsUtils.getExtensionValue(cert, tinOid) as String
        return tin.trim()
    }

    fun getCustomerTIN(tinOid: String, cert: X509Certificate): String {
        val tin = CertExtensionsUtils.getExtensionValue(cert, tinOid) as String
        return tin.trim()
    }

    fun findTinOid(cert: X509Certificate): String? = getOidList(cert)
            .find { oid -> oid.endsWith(CertConstants.OID_Z_TIN) }

    fun getEnvCountry(oid: String): String {

        if (oid.isEmpty()) {
            return ""
        }

        val certOIDPrefix = "${CertConstants.OID_PREFIX}."
        val (_, countryParam) = oid.substringAfter(certOIDPrefix).split('.')

        return when (countryParam) {
            CertConstants.OID_Y_FIJI -> "Fiji"
            CertConstants.OID_Y_SAMOA -> "Samoa"
            CertConstants.OID_Y_WA_US -> "WA US"
            CertConstants.OID_Y_SERBIA -> "Serbia"
            else -> ""
        }
    }
}
