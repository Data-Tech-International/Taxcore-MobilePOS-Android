package online.taxcore.pos.data.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Cert : RealmObject() {
    @PrimaryKey
    var uuid: String = UUID.randomUUID().toString()
    var uid: String = ""
    var name: String = ""
    var pfxData: String = ""

    fun displayName(): String {
        val cName = this.name
                .substring(2)
                .removeSuffix(".nochain.p12")

        return "$cName.p12".toLowerCase(Locale.ROOT)
    }
}
