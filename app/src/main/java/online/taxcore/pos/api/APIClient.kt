package online.taxcore.pos.api

import com.google.gson.GsonBuilder
import online.taxcore.pos.api.handleCertificates.TrustAuthority
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class APIClient {
    companion object {

        private val gsonConverterFactory =
            GsonConverterFactory.create(GsonBuilder().serializeNulls().create())

        fun vsdc(url: String, certAlias: String): ApiService? {
            val retrofit: Retrofit?
            return try {
                retrofit = Retrofit.Builder().apply {
                    addConverterFactory(gsonConverterFactory)
                    baseUrl(url)
                    client(TrustAuthority.vsdcOkHttpClient(certAlias) ?: return null)
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
    }
}
