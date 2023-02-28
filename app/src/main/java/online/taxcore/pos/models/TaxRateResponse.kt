package online.taxcore.pos.models

import com.google.gson.annotations.SerializedName
import online.taxcore.pos.utils.TCUtil

class TaxRateResponse {
    @SerializedName("CurrentTaxRateGroup")
    var taxRateGroup: TaxRateGroup? = null

    fun getTaxLabels(oid: String = ""): List<TaxItem>? {
        return taxRateGroup?.taxCategories?.flatMap { taxCategory: TaxCategory ->
            val symbol = if (oid.isBlank()) "" else TCUtil.getCurrency(oid)
            val taxValue = if (taxCategory.categoryType == "2") symbol else "%"
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

class TaxRateGroup {
    @SerializedName("GroupId")
    var groupId: Number? = null

    @SerializedName("TaxCategories")
    var taxCategories: List<TaxCategory> = arrayListOf(TaxCategory())
}

class TaxCategory {
    @SerializedName("CategoryId")
    var categoryId: Number? = null

    @SerializedName("Name")
    var name: String = ""

    @SerializedName("CategoryType")
    var categoryType: String = ""

    @SerializedName("TaxRates")
    var taxRates: List<TaxRate> = arrayListOf(TaxRate())
}

class TaxRate {
    @SerializedName("RateId")
    var rateId: Number? = null

    @SerializedName("Rate")
    var rate: Double = 0.0

    @SerializedName("Label")
    var label: String = ""
}
