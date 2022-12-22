package online.taxcore.pos.data.models

import online.taxcore.pos.utils.TCUtil

class StatusResponse {
    var sdcDateTime: String = ""
    var supportedLanguages: List<String> = arrayListOf()
    var uid: String = ""
    var taxCoreApi: String = ""
    var allTaxRates: List<CurrentTaxRate> = arrayListOf()
    var currentTaxRates: CurrentTaxRate? = null
    var gsc: List<String> = arrayListOf()

    fun getTaxLabels(countryCode: String = ""): List<TaxItem>? {
        val currencySymbol = TCUtil.getCurrencyBy(countryCode)
        return currentTaxRates?.taxCategories?.map { taxCategory ->
            val taxValue =
                if (taxCategory.categoryType == "AmountPerQuantity") currencySymbol else "%"

            TaxItem(
                name = taxCategory.name,
                label = taxCategory.taxRates.first().label, // FIXME: 29.7.21. Map all taxRates
                rate = taxCategory.taxRates.first().rate, // FIXME: 29.7.21. Map all taxRates
                value = taxValue,
            )
        }
    }

    fun hasErrors(): Boolean {
        val errorCodes = arrayListOf<String>()
        return this.gsc.any { it.startsWith("2") }
    }
}

class CurrentTaxRate {
    val validFrom: String = ""
    val groupId: Number? = null
    val taxCategories: List<TaxCategory2> = arrayListOf()
}

class TaxCategory2 {
    var name: String = ""
    var categoryType: String = ""
    var taxRates: List<TaxRate2> = arrayListOf(TaxRate2())
    var orderId: Number = 0
}

class TaxRate2 {
    var rate: Double = 0.0
    var label: String = ""
}
