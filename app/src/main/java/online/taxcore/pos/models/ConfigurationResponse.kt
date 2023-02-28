package online.taxcore.pos.models

import com.google.gson.annotations.SerializedName
import online.taxcore.pos.utils.TCUtil

data class ConfigurationResponse(
    val OrganizationName: String,
    val ServerTimeZone: String,
    val Street: String,
    val City: String,
    val Country: String,
    val Endpoints: EndpointsResponse,
    val EnvironmentName: String,
    val Logo: String,
    val NTPServer: String
) {

    @SerializedName("ActiveTaxRateGroup")
    var taxRateGroup: List<TaxRateGroup>? = null

    fun getTaxItems(): List<TaxItem>? {
        // FIXME: 3/1/21 - This needs fixing to take labels only valid after spec date
        return taxRateGroup?.first()?.taxCategories?.flatMap { taxCategory: TaxCategory ->
            val taxValue =
                if (taxCategory.categoryType == "2") TCUtil.getCurrencyBy(Country) else "%"
            taxCategory.taxRates.map { taxRate: TaxRate ->
                TaxItem(
                    name = taxCategory.name,
                    label = taxRate.label,
                    rate = taxRate.rate,
                    value = taxValue
                )
            }
        }
    }
}

data class EndpointsResponse(
    val TaxpayerAdminPoortal: String,
    val TaxCoreApi: String,
    val VSDC: String,
    val Root: String
)
