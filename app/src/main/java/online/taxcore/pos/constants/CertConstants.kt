package online.taxcore.pos.constants

class CertConstants {
    companion object {
        const val OID_PREFIX = "1.3.6.1.4.1.49952"

        // X params ENV
        const val OID_X_INTERNAL = "1"
        const val OID_X_STAGING = "2"
        const val OID_X_PRODUCTION = "3"
        const val OID_X_PILOT = "4"

        // Y param COUNTRY
        const val OID_Y_FIJI = "2"
        const val OID_Y_WA_US = "5"
        const val OID_Y_SAMOA = "6"

        // Z param
        const val OID_Z_BACKEND_API = "5"
        const val OID_Z_TIN = "6"
        const val OID_Z_VSDC_URL = "7"
    }
}
