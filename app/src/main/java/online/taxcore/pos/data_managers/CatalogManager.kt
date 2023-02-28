package online.taxcore.pos.data_managers

import com.vicpin.krealmextensions.*
import io.realm.Case
import io.realm.Realm
import io.realm.kotlin.where
import online.taxcore.pos.extensions.roundTo2DecimalPlaces
import online.taxcore.pos.models.Item
import online.taxcore.pos.models.Taxes

object CatalogManager {

    var itemName = ""
    var unitPrice = ""
    var gtinNum = ""
    var appliedTaxes = emptyArray<String>()

    fun hasCatalogItems(): Boolean {
        return Item().count() > 0L
    }

    fun loadCatalogItems(): MutableList<Item> {
        return Item().queryAll().toMutableList()
    }

    fun replaceCatalog(catalogList: List<Item>) {
        // Remove all items form Realm
        Item().deleteAll()
        Taxes().deleteAll()

        // save new Items Realm
        catalogList.saveAll()
    }

    fun loadFilteredItems(): MutableList<Item> {
        val catalogItems = Item().query {
            if (itemName.isNotEmpty()) {
                contains("name", itemName, Case.INSENSITIVE)
            }

            if (unitPrice.isNotEmpty()) {
                equalTo("price", unitPrice.roundTo2DecimalPlaces().toDouble())
            }

            if (gtinNum.isNotEmpty()) {
                contains("barcode", gtinNum, Case.INSENSITIVE)
            }

            if (appliedTaxes.isNotEmpty()) {
                // `in`("tax.code", appliedTaxes)
                for (appliedTax in appliedTaxes) {
                    contains("tax.code", appliedTax)
                }
            }
        }

        return catalogItems.toMutableList()
    }

    fun toggleFavoriteItem(item: Item?, onSuccess: () -> Unit) {

        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                val currentItem = realm.where<Item>().equalTo("uuid", item?.uuid).findFirst()
                // update is favorite
                currentItem?.let { itm ->
                    itm.isFavorite = item?.isFavorite!!
                }
                onSuccess()
            }
        } catch (e: Exception) {
            print(e)
        } finally {
            realm.close()
        }
    }

    fun getFilterItemsByNameAndBarcode(pattern: String) =
        if (isBarcode(pattern)) {
            Item().query {
                contains("barcode", pattern)
            }.reversed().toMutableList()
        } else {
            Item().query {
                contains("name", pattern, Case.INSENSITIVE)
            }.reversed().toMutableList()
        }

    private fun isBarcode(pattern: String): Boolean {
        return try {
            pattern.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }


    fun resetFilter() {
        itemName = ""
        unitPrice = ""
        gtinNum = ""
        appliedTaxes = emptyArray()
    }

}
