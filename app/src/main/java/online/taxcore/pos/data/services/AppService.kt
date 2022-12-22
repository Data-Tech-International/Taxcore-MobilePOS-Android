package online.taxcore.pos.data.services

import online.taxcore.pos.AppSession
import online.taxcore.pos.data.local.TaxesManager

object AppService {
    fun resetConfiguration(onReset: () -> Unit) {
        TaxesManager.removeAllTaxes()

        AppSession.isAppConfigured = false
        AppSession.shouldAskForConfiguration = true

        onReset()
    }
}
