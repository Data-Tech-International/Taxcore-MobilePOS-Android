package online.taxcore.pos.models

import com.google.gson.annotations.SerializedName

class ErrorResponse {
    @SerializedName("Message")
    var message: String? = null

    @SerializedName("MessageDetails")
    var description: String? = null
}
