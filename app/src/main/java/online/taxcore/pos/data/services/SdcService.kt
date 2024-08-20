package online.taxcore.pos.data.services

import okhttp3.ResponseBody
import online.taxcore.pos.AppSession
import online.taxcore.pos.data.api.APIClient
import online.taxcore.pos.data.api.handleCertificates.ClientAuthority
import online.taxcore.pos.data.local.TaxesManager
import online.taxcore.pos.data.models.EnvResponse
import online.taxcore.pos.data.models.StatusResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object SdcService {

    fun pingEsdcServer(
        endpoint: String,
        onStart: () -> Unit,
        onSuccess: () -> Unit,
        onError: (message: String?) -> Unit,
        onEnd: () -> Unit
    ) {

        onStart()

        val esdcService = APIClient.esdc(endpoint)
        val pingRequest = esdcService?.attention()

        if (pingRequest == null) {
            onError(null)
        }

        pingRequest?.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(null)
                onEnd()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    onSuccess()
                    return onEnd()
                }

                onError("")
                onEnd()
            }
        })
    }

    fun fetchEsdcConfiguration(
        endpoint: String,
        onStart: () -> Unit,
        onSuccessEnv: (res: EnvResponse) -> Unit,
        onSuccessStatus: (res: StatusResponse) -> Unit,
        onError: (message: String) -> Unit,
        onEnd: () -> Unit
    ) {

        onStart()

        val apiServer = APIClient.esdc(endpoint)
        var errorMsg = "Unable to complete request"

        val statusCall = apiServer?.fetchStatus()
        statusCall?.enqueue(object : Callback<StatusResponse> {
            override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                onError(errorMsg)
                onEnd()
            }

            override fun onResponse(
                call: Call<StatusResponse>,
                response: Response<StatusResponse>
            ) {

                if (response.code() != 200) {
                    onError("")
                    return onEnd()
                }

                val statusResponse = response.body() ?: return onEnd()

                if (statusResponse.hasErrors()) {
                    val errorCode = statusResponse.gsc.first { it.startsWith("2") }
                    onError(errorCode)
                    onEnd()
                    return
                }

                val envRequest = apiServer.fetchEnvParams()
                envRequest.enqueue(object : Callback<EnvResponse> {
                    override fun onFailure(call: Call<EnvResponse>, t: Throwable) {

                        t.message?.let {
                            errorMsg = it
                        }

                        onEnd()
                        onError(errorMsg)
                    }

                    override fun onResponse(
                        call: Call<EnvResponse>,
                        response: Response<EnvResponse>
                    ) {
                        if (response.isSuccessful.not()) {
                            onError("")
                            onEnd()
                            return
                        }

                        val environment = response.body() ?: return onEnd()
                        val activeTaxItems = statusResponse.getTaxLabels(environment.country)

                        TaxesManager.replaceActiveTaxItems(activeTaxItems)

                        AppSession.let {
                            it.isAppConfigured = true
                            it.shouldAskForConfiguration = false
                        }

                        onSuccessStatus(statusResponse)
                        onSuccessEnv(environment)
                        onEnd()
                    }
                })
            }
        })
    }

    fun fetchVsdcConfiguration(
        clientAuthority: ClientAuthority,
        pac: String,
        onStart: () -> Unit,
        onSuccessStatus: (StatusResponse: StatusResponse) -> Unit,
        onSuccessEnv: (res: EnvResponse) -> Unit,
        onError: (message: String) -> Unit,
        onEnd: () -> Unit
    ) {
        onStart()

        val apiServer = APIClient.vsdc(clientAuthority)

        var errorMsg = "Unable to complete request"

        val call = apiServer?.fetchEnvParams(pac)
        call?.enqueue(object : Callback<EnvResponse> {
            override fun onFailure(call: Call<EnvResponse>, t: Throwable) {

                t.message?.let {
                    errorMsg = it
                }

                onEnd()
                onError(errorMsg)
            }

            override fun onResponse(call: Call<EnvResponse>, response: Response<EnvResponse>) {
                if (response.isSuccessful.not()) {
                    onError("")
                    onEnd()
                    return
                }

                val environment = response.body() ?: return onEnd()

                val statusCall = apiServer.getTaxes(pac)
                statusCall.enqueue(object : Callback<StatusResponse> {
                    override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                        t.message?.let {
                            errorMsg = it
                        }

                        onEnd()
                        onError(errorMsg)
                    }

                    override fun onResponse(
                        call: Call<StatusResponse>,
                        response: Response<StatusResponse>
                    ) {
                        if (response.isSuccessful.not()) {
                            onError("Unsuccessful request")
                            onEnd()
                            return
                        }

                        val statusResponse = response.body()!!

                        AppSession.let {
                            it.shouldAskForConfiguration = false
                            it.isAppConfigured = true
                            it.resetSessionCredentials()
                            it.pacCode = pac
                        }

                        val taxes =
                            statusResponse.getTaxLabels(environment.country)

                        // Remove previous taxes from DB
                        TaxesManager.replaceActiveTaxItems(taxes)

                        onSuccessEnv(environment)
                        onSuccessStatus(statusResponse)
                        onEnd()
                    }
                })

            }
        })

    }
}

