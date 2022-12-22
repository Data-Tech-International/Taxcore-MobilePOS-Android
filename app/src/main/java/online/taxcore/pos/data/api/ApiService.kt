package online.taxcore.pos.data.api

import online.taxcore.pos.data.models.EnvResponse
import online.taxcore.pos.data.models.InvoiceResponse
import online.taxcore.pos.data.models.StatusResponse
import online.taxcore.pos.data.params.InvoiceRequest
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @Headers(
            "Accept: application/json",
            "Content-Type: application/json"
    )
    @POST("api/v3/invoices")
    fun createInvoice(
            @Header("PAC") pacValue: String,
            @Body payload: InvoiceRequest
    ): Call<InvoiceResponse>

    @Headers(
            "Accept: application/json",
            "Content-Type: application/json"
    )
    @GET("api/v3/status")
    fun getTaxes(
            @Header("PAC") pacValue: String
    ): Call<StatusResponse>

    @GET("api/v3/environment-parameters")
    fun fetchEnvParams(
            @Header("PAC") pacValue: String
    ): Call<EnvResponse>

}
