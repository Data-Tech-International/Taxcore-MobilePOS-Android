package online.taxcore.pos.constants

class PrefConstants {
    companion object {
        const val CASHIER_NAME = "cashier_name"

        // Settings
        const val USE_VSDC_SERVER = "use_vsdc_server"
        const val USE_ESDC_SERVER = "use_esdc_server"

        const val VSDC_ENDPOINT_URL = "vsdc_base_url"
        const val ESDC_ENDPOINT_URL = "esdc_base_url"
        const val ESDC_SERVER_NAME = "esdc_server_name"

        const val DEV_MODE = "dev_mode"

        // Certificate
        const val TIN_OID: String = "tin_oid"
        const val CURRENT_COUNTRY = "current_country"
        const val CERT_ALIAS_VALUE = "cert_alias_value"
        const val CERT_SUBJECT: String = "cert_full_subject"

        const val IS_APP_CONFIGURED = "is_app_configured"

        //SDC Configuration
        const val LOGO: String = "logo"
        const val COUNTRY: String = "country"
        const val ENVIRONMENT_NAME = "environment_name"
        const val ENVIRONMENT_ROOT_URL = "environment_root_url"
        const val IS_CUSTOM_CHOSEN = "is_custom_environment_selected"
        const val CUSTOM_ENVIRONMENT_URL = "prev_custom_environment_url"

        // Invoice
        const val LAST_CREDENTIALS_TIME = "online.taxcore.credentials.timestamp"
    }
}
