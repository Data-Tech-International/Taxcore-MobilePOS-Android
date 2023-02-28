package online.taxcore.pos.models

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Item : RealmObject() {
    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var name = ""
    var tax: RealmList<Taxes> = RealmList()
    var barcode: String = ""
    var price: Double = 0.0
    var quantity: Double = 1.0
    var type = "Catalog"

    var isSelected: Boolean = false

    var isFavorite: Boolean = false

}
