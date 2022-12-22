package online.taxcore.pos.data.local

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import online.taxcore.pos.data.models.TaxItem
import online.taxcore.pos.data.realm.TaxesSettings


object TaxesManager {

    var list: ArrayList<TaxesSettings> = arrayListOf()

    private fun addItem(position: Int, taxes: TaxesSettings) {
        return list.add(position, taxes)
    }

    fun replaceActiveTaxItems(taxItems: List<TaxItem>) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                // remove a item field
                realm.where<TaxesSettings>().findAll().deleteAllFromRealm()

                // Add a appliedTaxes
                taxItems.forEach { taxItem ->
                    val taxes = realm.createObject<TaxesSettings>()
                    with(taxes) {
                        code = taxItem.label
                        name = taxItem.name
                        rate = taxItem.rate
                        value = taxItem.value
                    }
                    addItem(0, taxes)
                }
            }
        } catch (e: Exception) {
            print(e)
        } finally {
            realm.close()
        }
    }

    fun addItemToDatabase(taxItem: TaxItem) {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                // Add a appliedTaxes
                val taxes = realm.createObject<TaxesSettings>()
                with(taxes) {
                    code = taxItem.label
                    name = taxItem.name
                    rate = taxItem.rate
                    value = taxItem.value
                }
                addItem(0, taxes)
            }
        } catch (e: Exception) {
            print(e)
        } finally {
            realm.close()
        }
    }

    fun getAllTaxes(): ArrayList<TaxesSettings> {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                list = realm.copyFromRealm(realm.where<TaxesSettings>().findAll()) as ArrayList<TaxesSettings>
            }
            return list
        } catch (e: Exception) {
            print(e)
        } finally {
            realm.close()
        }
        return arrayListOf()
    }

    fun removeAllTaxes() {
        val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction {
                // remove a item field
                realm.where<TaxesSettings>().findAll().deleteAllFromRealm()
            }
        } catch (e: Exception) {
            print(e)
        } finally {
            realm.close()
        }
    }
}
