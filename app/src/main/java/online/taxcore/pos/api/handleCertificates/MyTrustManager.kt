package online.taxcore.pos.api.handleCertificates

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class VsdcTrustManager(private val certificateChain: Array<X509Certificate>?) : X509TrustManager {

    override fun getAcceptedIssuers(): Array<X509Certificate>? = certificateChain

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }
}

class EsdcTrustManager : X509TrustManager {

    override fun getAcceptedIssuers(): Array<X509Certificate>? = arrayOf()

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
    }
}
