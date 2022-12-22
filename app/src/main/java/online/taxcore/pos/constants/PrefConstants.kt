package online.taxcore.pos.constants

import online.taxcore.pos.BuildConfig

class PrefConstants {
    companion object {
        const val SP_NAME_KEY = BuildConfig.APPLICATION_ID + "shared.prefs"
        const val SP_SECURED_NAME_KEY = BuildConfig.APPLICATION_ID + "secured.prefs"
        const val CASHIER_NAME = "cashier_name"

        // Settings
        const val USE_VSDC_SERVER = "use_vsdc_server"
        const val USE_ESDC_SERVER = "use_esdc_server"

        const val VSDC_ENDPOINT_URL = "vsdc_base_url"
        const val ESDC_ENDPOINT_URL = "esdc_base_url"

        const val APP_LOCALE: String = "application_locale"

        // Certificate
        const val CERT_TIN_OID = BuildConfig.APPLICATION_ID + ".tin_oid"
        const val CERT_COUNTRY = BuildConfig.APPLICATION_ID + ".current_country"
        const val CERT_ALIAS_VALUE = BuildConfig.APPLICATION_ID + ".cert_alias_value"
        const val CERT_SUBJECT = BuildConfig.APPLICATION_ID + ".cert_full_subject"
        const val CERT_CERT_PAC = BuildConfig.APPLICATION_ID + ".cert_pfx_pac"
        const val CERT_GLOBAL_PAC = BuildConfig.APPLICATION_ID + ".certificate_global_pac"

        const val IS_APP_CONFIGURED = BuildConfig.APPLICATION_ID + ".is_app_configured"

        //SDC Configuration
        const val ENV_LOGO_URL = "environment_logo_url"
        const val ENV_COUNTRY = "environment_country"
        const val ENV_NAME = "environment_name"
        const val ENV_UID = "env_uid_value"
        const val ENV_API_ADDRESS = "env_api_address"
        const val ENV_ESDC_API_URL = "esdc_api_url"
        const val ENV_ESDC_NAME = "esdc_env_name"


        // Invoice
        const val LAST_CREDENTIALS_TIME = "online.taxcore.credentials.timestamp"
    }
}
