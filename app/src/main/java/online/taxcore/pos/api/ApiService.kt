package online.taxcore.pos.api

import online.taxcore.pos.models.InvoiceResponse
import online.taxcore.pos.models.TaxRateResponse
import online.taxcore.pos.params.InvoiceRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("api/Sign")
    fun postInvoice(@Body params: InvoiceRequest): Call<InvoiceResponse>

    @GET("api/Status")
    fun getTaxes(): Call<TaxRateResponse>

}
