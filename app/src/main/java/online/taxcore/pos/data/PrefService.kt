package online.taxcore.pos.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.firebase.crashlytics.FirebaseCrashlytics
import online.taxcore.pos.constants.PrefConstants
import online.taxcore.pos.data.models.CertData
import online.taxcore.pos.data.models.EnvData
import online.taxcore.pos.data.models.EnvResponse
import online.taxcore.pos.data.models.StatusResponse
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

class PrefService(context: Context) {

    companion object {
        private const val TAG = "PrefService"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    private val keyGenParameterSpec: KeyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    private val mainKeyAlias: String = MasterKeys.getOrCreate(keyGenParameterSpec)

    private val encryptedSharedPreferences: SharedPreferences =
        createEncryptedSharedPreferences(context)

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

        sharedPreferences.edit {
            putBoolean(PrefConstants.USE_VSDC_SERVER, true)
        }

        // Secretly Save app config from certificate
        encryptedSharedPreferences.edit {
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
        }

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
        encryptedSharedPreferences.edit { remove(PrefConstants.CERT_ALIAS_VALUE) }
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
        sharedPreferences.edit {
            putBoolean(PrefConstants.USE_VSDC_SERVER, useVsdc)
            putBoolean(PrefConstants.USE_ESDC_SERVER, !useVsdc)
        }
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
        sharedPreferences.edit {
            putBoolean(PrefConstants.IS_APP_CONFIGURED, true)
            putBoolean(PrefConstants.USE_ESDC_SERVER, useEsdc)
            putBoolean(PrefConstants.USE_VSDC_SERVER, !useEsdc)
        }
    }

    fun saveEnvData(envData: EnvResponse, isEsdc: Boolean = false) {

        // Secretly Save app config from certificate
        encryptedSharedPreferences.edit {
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
        }
    }

    fun saveStatusData(statusData: StatusResponse) {
        setAppConfigured()

        encryptedSharedPreferences.edit {
            putString(PrefConstants.ENV_UID, encodeString(statusData.uid))
            putString(PrefConstants.ENV_API_ADDRESS, encodeString(statusData.taxCoreApi))
        }
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
        sharedPreferences.edit { putString(PrefConstants.APP_LOCALE, locale) }
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
        sharedPreferences.edit { putBoolean(PrefConstants.IS_APP_CONFIGURED, configured) }
    }

    fun saveCredentialsTime() {
        sharedPreferences.edit { putLong(PrefConstants.LAST_CREDENTIALS_TIME, Date().time) }
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
        sharedPreferences.edit {
            remove(PrefConstants.IS_APP_CONFIGURED)
        }

        encryptedSharedPreferences.edit {
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
        }
    }

    // Secure pref helpers

    private fun saveSecureString(key: String, value: String) {
        val encodedValue = encodeString(value)
        encryptedSharedPreferences.edit { putString(key, encodedValue) }
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

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                PrefConstants.SP_SECURED_NAME_KEY,
                mainKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, clearing corrupted data", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            clearCorruptedEncryptedPreferences(context)

            // Regenerate master key and retry after clearing corrupted data
            val newMasterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
            EncryptedSharedPreferences.create(
                PrefConstants.SP_SECURED_NAME_KEY,
                newMasterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun clearCorruptedEncryptedPreferences(context: Context) {
        try {
            // Clear the encrypted SharedPreferences file
            val sharedPrefsFile = File(
                context.applicationInfo.dataDir + "/shared_prefs",
                PrefConstants.SP_SECURED_NAME_KEY + ".xml"
            )
            if (sharedPrefsFile.exists()) {
                sharedPrefsFile.delete()
                Log.d(TAG, "Deleted corrupted SharedPreferences file")
            }

            // Clear the keyset from Android Keystore
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                if (keyStore.containsAlias(mainKeyAlias)) {
                    keyStore.deleteEntry(mainKeyAlias)
                    Log.d(TAG, "Deleted corrupted key from Android Keystore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear key from Keystore", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            // Mark app as not configured since encrypted data is lost
            context.getSharedPreferences(PrefConstants.SP_NAME_KEY, Context.MODE_PRIVATE)
                .edit {
                    putBoolean(PrefConstants.IS_APP_CONFIGURED, false)
                }
            Log.d(TAG, "Marked app as not configured due to data loss")

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing corrupted encrypted preferences", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

}
