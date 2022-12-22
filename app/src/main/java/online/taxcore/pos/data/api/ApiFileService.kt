package online.taxcore.pos.data.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiFileService {

    @GET
    fun download(@Url fileUrl: String): Call<ResponseBody>
}
