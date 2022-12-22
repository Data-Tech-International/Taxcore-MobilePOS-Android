package online.taxcore.pos.data.api

import com.google.gson.GsonBuilder
import online.taxcore.pos.data.api.handleCertificates.ClientAuthority
import online.taxcore.pos.data.api.handleCertificates.TrustAuthority
import online.taxcore.pos.utils.TCUtil
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class APIClient {
    companion object {

        private val gsonConverterFactory =
            GsonConverterFactory.create(GsonBuilder().serializeNulls().create())

        fun vsdc(clientAuthority: ClientAuthority?): ApiService? {
            val (_, cert) = clientAuthority!!
            val vsdcEndpoint = TCUtil.getVSDCEndpoint(cert)
            val retrofit: Retrofit?
            return try {
                retrofit = Retrofit.Builder().apply {
                    addConverterFactory(gsonConverterFactory)
                    baseUrl(vsdcEndpoint)
                    client(TrustAuthority.vsdcOkHttpClient(clientAuthority) ?: return null)
                }.build()
                retrofit.create(ApiService::class.java)
            } catch (e: IllegalArgumentException) {
                throw e
            }
        }

        fun esdc(url: String): ApiServiceESDC? {
            val retrofit: Retrofit?
            return try {
                retrofit = Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(gsonConverterFactory)
                    .client(TrustAuthority.esdcOkHttpClient())
                    .build()
                retrofit.create(ApiServiceESDC::class.java)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun core(baseUrl: String): ApiFileService? {

            val requestUrl = when {
                baseUrl.trim().endsWith("/") -> baseUrl
                else -> baseUrl.trim().plus("/")
            }

            val retrofit: Retrofit?
            return try {
                retrofit = Retrofit.Builder()
                    .baseUrl(requestUrl)
                    .addConverterFactory(gsonConverterFactory)
                    .client(TrustAuthority.esdcOkHttpClient())
                    .build()
                retrofit.create(ApiFileService::class.java)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
