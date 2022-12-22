package online.taxcore.pos

object AppSession {

    var isAppConfigured: Boolean = false
    var shouldAskForConfiguration: Boolean = true
    var pinCode: String = ""
    var pacCode: String = ""


    fun resetSessionCredentials() {
        pinCode = ""
        pacCode = ""
    }
}
