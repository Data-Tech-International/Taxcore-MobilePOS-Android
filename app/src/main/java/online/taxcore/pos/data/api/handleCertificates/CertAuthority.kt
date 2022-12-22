package online.taxcore.pos.data.api.handleCertificates

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.cert.X509Certificate

typealias ClientAuthority = Triple<KeyStore, X509Certificate, String>

object CertAuthority {

    private const val KEYSTORE_TYPE = "PKCS12"

    @Throws(IOException::class)
    fun certificateParams(
            pfxEncoded: String,
            pfxPass: String
    ): ClientAuthority {
        val decodedPfx = Base64.decode(pfxEncoded, Base64.DEFAULT)
        val pfxInputStream = ByteArrayInputStream(decodedPfx)

        val password = pfxPass.toCharArray()
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
        keyStore.load(pfxInputStream, password)

        // Take first alias from the keystore
        val aliases = keyStore.aliases()
        val alias = aliases.nextElement() as String
        val cert = keyStore.getCertificate(alias)
        cert as X509Certificate

        return Triple(keyStore, cert, alias)
    }
}
