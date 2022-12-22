package online.taxcore.pos.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data.models.CertData
import online.taxcore.pos.data.models.EnvData
import online.taxcore.pos.data.models.EnvResponse
import online.taxcore.pos.data.models.StatusResponse
import java.security.cert.X509Certificate
import java.util.*

class PrefService(context: Context) {

    private val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    private val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

    private val encryptedSharedPreferences =
        EncryptedSharedPreferences.create(
            PrefConstants.SP_SECURED_NAME_KEY,
            mainKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private val sharedPreferences = context.getSharedPreferences(
        PrefConstants.SP_NAME_KEY,
        Context.MODE_PRIVATE
    )

    fun hasCertInstalled(): Boolean {
        val vsdcUrl = loadSecureString(PrefConstants.VSDC_ENDPOINT_URL)
        val tinOid = loadSecureString(PrefConstants.CERT_TIN_OID)

        return vsdcUrl.isNotEmpty() && tinOid.isNotEmpty()
    }

    fun saveCertificateData(
        cert: X509Certificate
    ): CertData {
        val certData = CertData.extract(cert)

        sharedPreferences.edit().apply {
            putBoolean(PrefConstants.USE_VSDC_SERVER, true)
        }.apply()

        // Secretly Save app config from certificate
        encryptedSharedPreferences.edit().apply {
            putString(
                PrefConstants.VSDC_ENDPOINT_URL,
                encodeString(certData.vsdcEndpoint)
            )
            putString(
                PrefConstants.CERT_TIN_OID,
                encodeString(certData.tinOid)
            )
            putString(
                PrefConstants.CERT_COUNTRY,
                encodeString(certData.countryName)
            )

            putString(
                PrefConstants.CERT_SUBJECT,
                encodeString(certData.subject)
            )
        }.apply()

        return certData
    }

    fun loadCertData(): CertData {
        val certSubject = loadSecureString(PrefConstants.CERT_SUBJECT)
        val tinOid = loadSecureString(PrefConstants.CERT_TIN_OID)
        val country = loadSecureString(PrefConstants.CERT_COUNTRY)
        val vsdcUrl = loadSecureString(PrefConstants.VSDC_ENDPOINT_URL)

        return CertData(
            subject = certSubject,
            vsdcEndpoint = vsdcUrl,
            tinOid = tinOid,
            countryName = country
        )
    }

    fun saveActiveCertName(name: String) = saveSecureString(PrefConstants.CERT_ALIAS_VALUE, name)
    fun loadActiveCertName() = loadSecureString(PrefConstants.CERT_ALIAS_VALUE)
    fun removeActiveCertName() {
        encryptedSharedPreferences.edit().remove(PrefConstants.CERT_ALIAS_VALUE).apply()
    }

    fun loadCertCountry() = loadSecureString(PrefConstants.ENV_COUNTRY)
    fun loadTinOid() = loadSecureString(PrefConstants.CERT_TIN_OID)

    fun loadVsdcEndpoint(): String {
        return loadSecureString(PrefConstants.VSDC_ENDPOINT_URL)
    }

    fun savePfxPass(pfxFileName: String, pass: String) {
        saveSecureString(pfxFileName, pass)
    }

    fun loadPfxPass(name: String): String {
        return loadSecureString(name)
    }

    fun setUseVsdcServer(useVsdc: Boolean = true) {
        sharedPreferences.edit().apply {
            putBoolean(PrefConstants.USE_VSDC_SERVER, useVsdc)
            putBoolean(PrefConstants.USE_ESDC_SERVER, !useVsdc)
        }.apply()
    }

    fun useVSDCServer(): Boolean {
        return sharedPreferences.getBoolean(PrefConstants.USE_VSDC_SERVER, false)
    }

    // ESDC

    fun saveEsdcLocation(name: String) {
        val uid = loadSecureString(PrefConstants.ENV_UID)
        saveSecureString(uid, name)
    }

    fun loadLocation(uid: String): String {
        return loadSecureString(uid)
    }

    fun saveEsdcEndpoint(url: String) {
        saveSecureString(PrefConstants.ESDC_ENDPOINT_URL, url)
    }

    fun loadEsdcEndpoint(): String {
        return loadSecureString(PrefConstants.ESDC_ENDPOINT_URL)
    }

    fun useESDCServer(): Boolean {
        return sharedPreferences.getBoolean(PrefConstants.USE_ESDC_SERVER, false)
    }

    fun isESDCServerConfigured(): Boolean {
        return loadSecureString(PrefConstants.ESDC_ENDPOINT_URL).isNotBlank()
    }

    fun setUseEsdcServer(useEsdc: Boolean = true) {
        sharedPreferences.edit().apply {
            putBoolean(PrefConstants.IS_APP_CONFIGURED, true)
            putBoolean(PrefConstants.USE_ESDC_SERVER, useEsdc)
            putBoolean(PrefConstants.USE_VSDC_SERVER, !useEsdc)
        }.apply()
    }

    fun saveEnvData(envData: EnvResponse, isEsdc: Boolean = false) {

        // Secretly Save app config from certificate
        encryptedSharedPreferences.edit().apply {
            putString(PrefConstants.ENV_COUNTRY, encodeString(envData.country))
            putString(PrefConstants.ENV_LOGO_URL, encodeString(envData.logo))
            if (isEsdc) {
                putString(
                    PrefConstants.ENV_ESDC_API_URL,
                    encodeString(envData.endpoints.taxCoreApi)
                )
                putString(
                    PrefConstants.ENV_ESDC_NAME,
                    encodeString(envData.environmentName)
                )
            } else {
                putString(PrefConstants.ENV_API_ADDRESS, encodeString(envData.endpoints.taxCoreApi))
                putString(PrefConstants.ENV_NAME, encodeString(envData.environmentName))
            }
        }.apply()
    }

    fun saveStatusData(statusData: StatusResponse) {
        setAppConfigured()

        encryptedSharedPreferences.edit().apply {
            putString(PrefConstants.ENV_UID, encodeString(statusData.uid))
            putString(PrefConstants.ENV_API_ADDRESS, encodeString(statusData.taxCoreApi))
        }.apply()
    }

    fun loadEnvData(): EnvData {
        return EnvData(
            uid = loadSecureString(PrefConstants.ENV_UID),
            name = loadSecureString(PrefConstants.ENV_NAME),
            esdcEnvName = loadSecureString(PrefConstants.ENV_ESDC_NAME),
            esdcEndpoint = loadSecureString(PrefConstants.ESDC_ENDPOINT_URL),
            esdcApiEndpoint = loadSecureString(PrefConstants.ENV_ESDC_API_URL),
            apiEndpoint = loadSecureString(PrefConstants.ENV_API_ADDRESS),
            country = loadSecureString(PrefConstants.CERT_COUNTRY),
            logo = loadSecureString(PrefConstants.ENV_LOGO_URL)
        )
    }

    // Language

    fun loadLocale(): Locale {
        val lngTag = sharedPreferences.getString(PrefConstants.APP_LOCALE, "en").orEmpty()
        return Locale.forLanguageTag(lngTag)
    }

    fun setLanguage(locale: String) {
        sharedPreferences.edit().putString(PrefConstants.APP_LOCALE, locale).apply()
    }

    // ENV

    fun loadEnvironmentName() = loadSecureString(PrefConstants.ENV_NAME)

    fun loadEnvLogo(): String {
        return loadSecureString(PrefConstants.ENV_LOGO_URL)
    }

    fun isAppConfigured(): Boolean {
        return sharedPreferences.getBoolean(PrefConstants.IS_APP_CONFIGURED, false)
    }

    fun setAppConfigured(configured: Boolean = true) {
        sharedPreferences.edit().putBoolean(PrefConstants.IS_APP_CONFIGURED, configured).apply()
    }

    fun saveCredentialsTime() {
        sharedPreferences.edit().putLong(PrefConstants.LAST_CREDENTIALS_TIME, Date().time).apply()
    }

    fun getCredentialsTime(): Long {
        return sharedPreferences.getLong(PrefConstants.LAST_CREDENTIALS_TIME, Date().time)
    }

    fun savePac(token: String) {
        saveSecureString(PrefConstants.CERT_CERT_PAC, token)
        saveSecureString(PrefConstants.CERT_GLOBAL_PAC, token)
    }

    fun loadGlobalPac(): String {
        return loadSecureString(PrefConstants.CERT_GLOBAL_PAC)
    }

    fun removeConfiguration() {
        sharedPreferences.edit().apply {
            remove(PrefConstants.IS_APP_CONFIGURED)
        }.apply()

        encryptedSharedPreferences.edit().apply {
            remove(PrefConstants.CERT_GLOBAL_PAC)
            remove(PrefConstants.CERT_ALIAS_VALUE)
            remove(PrefConstants.CERT_TIN_OID)
            remove(PrefConstants.ENV_COUNTRY)
            remove(PrefConstants.ENV_LOGO_URL)
            remove(PrefConstants.VSDC_ENDPOINT_URL)
            remove(PrefConstants.ENV_API_ADDRESS)
            remove(PrefConstants.ENV_UID)
            remove(PrefConstants.ENV_NAME)
            remove(PrefConstants.ENV_ESDC_NAME)
            remove(PrefConstants.ENV_ESDC_API_URL)
        }.apply()
    }

    // Secure pref helpers

    private fun saveSecureString(key: String, value: String) {
        val encodedValue = encodeString(value)
        encryptedSharedPreferences.edit().putString(key, encodedValue).apply()
    }

    private fun loadSecureString(key: String): String {
        val encodedValue = encryptedSharedPreferences.getString(key, "").orEmpty()
        return decodeString(encodedValue)

    }

    // Data helpers

    private fun decodeString(encodedValue: String): String {
        return Base64.decode(encodedValue, Base64.DEFAULT).decodeToString()
    }

    private fun encodeString(value: String): String {
        return Base64.encodeToString(value.toByteArray(), Base64.DEFAULT)
    }

}
