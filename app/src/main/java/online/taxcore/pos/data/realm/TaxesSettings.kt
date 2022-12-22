package online.taxcore.pos.data.realm

import io.realm.RealmObject

open class TaxesSettings : RealmObject() {
    var code: String = "" // tax label
    var isChecked = false
    var name: String = "-"
    var rate: Double = 0.0
    var value: String = ""
}
