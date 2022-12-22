package online.taxcore.pos.data.api.handleCertificates

import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.X509ExtendedKeyManager

class MyKeyManager(
        private val certificateChain: Array<X509Certificate>?,
        private var keystore: KeyStore,
        private var password: String?
) : X509ExtendedKeyManager() {
    override fun getClientAliases(p0: String?, p1: Array<out Principal>?): Array<String>? {
        return try {
            arrayOf(keystore.aliases().nextElement())
        } catch (e: Exception) {
            null
        }
    }

    override fun getServerAliases(p0: String?, p1: Array<out Principal>?): Array<String>? = null

    override fun chooseServerAlias(p0: String?, p1: Array<out Principal>?, p2: Socket?): String? = null

    override fun getCertificateChain(p0: String?): Array<X509Certificate>? = certificateChain

    override fun getPrivateKey(p0: String?): PrivateKey? {
        return try {
            val alias = keystore.aliases().nextElement()
            val protectionParam = KeyStore.PasswordProtection(password?.toCharArray())
            val keyEntry = keystore.getEntry(alias, protectionParam) as KeyStore.PrivateKeyEntry

            keyEntry.privateKey
        } catch (e: Exception) {
            null
        }
    }

    override fun chooseClientAlias(p0: Array<out String>?, p1: Array<out Principal>?, p2: Socket?) = ""
}
