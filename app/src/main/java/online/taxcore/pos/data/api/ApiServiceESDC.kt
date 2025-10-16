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

    @Headers("Content-Type: application/json")
    @POST("api/v3/invoices")
    fun createInvoice(@Body payload: InvoiceRequest): Call<InvoiceResponse>

    @POST("api/v3/pin")
    fun verifyPin(@Body pin: String): Call<String>

    @Headers("Content-Type: application/json")
    @GET("api/v3/status")
    fun fetchStatus(): Call<StatusResponse>

    @Headers("Content-Type: application/json")
    @GET("api/v3/environment-parameters")
    fun fetchEnvParams(): Call<EnvResponse>

    @Headers("Content-Type: application/json")
    @GET("api/v3/attention")
    fun attention(): Call<ResponseBody>

}
