package online.taxcore.pos.api.handleCertificates

import android.security.KeyChain
import android.security.KeyChainException
import android.util.Log
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import online.taxcore.pos.BuildConfig
import online.taxcore.pos.TaxCoreApp
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class TrustAuthority {
    companion object {

        private fun createLogInterceptor(): HttpLoggingInterceptor {
            val logInterceptor = HttpLoggingInterceptor()

            when {
                BuildConfig.DEBUG -> logInterceptor.level = HttpLoggingInterceptor.Level.BODY
                else -> logInterceptor.level = HttpLoggingInterceptor.Level.NONE
            }

            return logInterceptor
        }

        fun okHttpClient(): OkHttpClient {
            return try {
                OkHttpClient.Builder()
                    .addInterceptor(createLogInterceptor())
                    .followSslRedirects(true)
                    .followRedirects(true)
                    .protocols(listOf(Protocol.HTTP_1_1))
                    .build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        fun esdcOkHttpClient(): OkHttpClient {
            return try {
                val x509TrustManager = EsdcTrustManager()
                val trustAllCerts: Array<TrustManager> = arrayOf(x509TrustManager)
                val sslContext: SSLContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                OkHttpClient.Builder()
                    .writeTimeout(40, TimeUnit.SECONDS)
                    .connectTimeout(40, TimeUnit.SECONDS)
                    .readTimeout(40, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.socketFactory, x509TrustManager)
                    .addInterceptor(createLogInterceptor())
                    .hostnameVerifier(HostnameVerifier { _: String?, _: SSLSession? -> true })
                    .protocols(listOf(Protocol.HTTP_1_1))
                    .build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        fun vsdcOkHttpClient(alias: String): OkHttpClient? {
            try {

                val certificateChain = KeyChain.getCertificateChain(TaxCoreApp.application, alias)
                val privateKey = KeyChain.getPrivateKey(TaxCoreApp.application, alias)
                val password = "".toCharArray()

                val x509TrustManager = VsdcTrustManager(certificateChain)
                val trustAllCerts = arrayOf<TrustManager>(x509TrustManager)

                val keyStore = KeyStore.getInstance("PKCS12").apply {
                    load(null, null)
                    setKeyEntry(alias, privateKey, password, certificateChain)
                }

                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(keyStore, null)

                val km = arrayOf<KeyManager>(MyKeyManager(certificateChain, keyStore, null))

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(km, trustAllCerts, SecureRandom())

                val sslSocketFactory = sslContext.socketFactory

                val tlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build()

                return OkHttpClient.Builder().apply {
                    writeTimeout(40, TimeUnit.SECONDS)
                    connectTimeout(40, TimeUnit.SECONDS)
                    readTimeout(40, TimeUnit.SECONDS)
                    protocols(listOf(Protocol.HTTP_1_1))
                    addInterceptor(createLogInterceptor())
                    connectionSpecs(listOf(tlsSpec))
                    sslSocketFactory(sslSocketFactory, x509TrustManager)
                    hostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                }.build()
            } catch (e: KeyChainException) {
                e.printStackTrace()
                return null
            } catch (e: Exception) {
                Log.i("LOG >>>", e.toString())
                throw RuntimeException(e)
            }
        }
    }
}
