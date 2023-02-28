package online.taxcore.pos.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Cashier : RealmObject() {
    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var id: String = ""
    var name: String = ""
    var isChecked = false
}
