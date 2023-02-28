package online.taxcore.pos.models

import online.taxcore.pos.utils.TCUtil
import java.security.cert.X509Certificate

data class CertData(
    val subject: String,
    val tinOid: String,
    val countryName: String,
    val vsdcEndpoint: String
) {

    val commonName: String
        get() {
            return subject.substringAfter("CN=").substringBefore(",")
        }

    val serialNumber: String
        get() {
            return subject.substringAfter("SERIALNUMBER=").substringBefore(",")
        }

    val organisationUnit: String
        get() = this.subject.substringAfter("OU=").substringBefore(",")

    val organisationName: String
        get() = this.subject.substringAfter("O=").substringBefore(",")

    companion object {
        fun extract(cert: X509Certificate): CertData {
            val vsdcEndpoint = TCUtil.getVSDCEndpoint(cert)
            val tinOID = TCUtil.findTinOid(cert).orEmpty()
            val countryName = TCUtil.getEnvCountry(tinOID)
            val certSubject = cert.subjectDN.toString()

            return CertData(
                subject = certSubject,
                vsdcEndpoint = vsdcEndpoint,
                countryName = countryName,
                tinOid = tinOID
            )
        }
    }

}
