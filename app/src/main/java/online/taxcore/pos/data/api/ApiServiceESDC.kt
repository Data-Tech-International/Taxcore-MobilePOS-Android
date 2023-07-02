package online.taxcore.pos.data.api

import okhttp3.ResponseBody
import online.taxcore.pos.data.models.EnvResponse
import online.taxcore.pos.data.models.InvoiceResponse
import online.taxcore.pos.data.models.StatusResponse
import online.taxcore.pos.data.params.InvoiceRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiServiceESDC {

    @POST("api/v3/invoices")
    @Headers("Accept-Language: sr-Cyrl-RS")
    fun createInvoice(@Body payload: InvoiceRequest): Call<InvoiceResponse>

    @POST("api/v3/pin")
    fun verifyPin(@Body pin: String): Call<String>

    @GET("api/v3/status")
    fun fetchStatus(): Call<StatusResponse>

    @GET("api/v3/environment-parameters")
    fun fetchEnvParams(): Call<EnvResponse>

    @GET("api/v3/attention")
    fun attention(): Call<ResponseBody>

}
