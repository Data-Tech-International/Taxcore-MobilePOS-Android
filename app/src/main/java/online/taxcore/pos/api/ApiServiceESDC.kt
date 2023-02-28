package online.taxcore.pos.api

import online.taxcore.pos.models.*
import online.taxcore.pos.params.AttentionParams
import online.taxcore.pos.params.InvoiceRequest
import online.taxcore.pos.params.PinParams
import online.taxcore.pos.params.StatusParams
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.*
import javax.net.ssl.*

interface ApiServiceESDC {

    @POST("api/Sign/SignInvoice")
    fun postInvoice(@Body params: InvoiceRequest): Call<InvoiceResponse>

    @POST("api/Status/VerifyPin")
    fun verifyPin(@Body params: PinParams): Call<PinResponse>

    @POST("api/Status/GetStatus")
    fun getStatus(@Body params: StatusParams): Call<StatusResponse>

    @POST("api/Status/Attention")
    fun getAttention(@Body params: AttentionParams): Call<AttentionResponse>

    @GET("api/Configuration/GetConfiguration")
    fun getConfiguration(): Call<ConfigurationResponse>

    @GET("api/Configuration/Environments")
    fun getConfigurationEnvironments(): Call<ArrayList<EnvironmentResponse>>

}
