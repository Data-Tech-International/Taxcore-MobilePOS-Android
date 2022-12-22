package online.taxcore.pos.data.local

import com.vicpin.krealmextensions.delete
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.queryFirst
import com.vicpin.krealmextensions.save
import io.realm.Case
import online.taxcore.pos.data.realm.Cert

object CertManager {

    fun saveCert(uid: String, name: String, data: String): Cert {

        val certItem = Cert().queryFirst {
            contains("name", name, Case.INSENSITIVE)
        }

        if (certItem == null) {
            val cert = Cert()
            cert.uid = uid
            cert.name = name
            cert.pfxData = data
            cert.save()
            return cert
        }

        certItem.uid = uid
        certItem.name = name
        certItem.pfxData = data

        return certItem

    }

    fun loadCert(name: String): Cert? {
        return Cert().queryFirst {
            contains("name", name, Case.INSENSITIVE)
        }
    }

    fun loadCerts(): MutableList<Cert> {
        return Cert().queryAll().toMutableList()
    }

    fun removeCert(name: String) {
        Cert().delete {
            contains("name", name, Case.INSENSITIVE)
        }
    }
}
