package online.taxcore.pos

import io.realm.DynamicRealm
import io.realm.RealmMigration

class MyMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var version: Long = oldVersion

        // DynamicRealm exposes an editable schema
        val schema = realm.schema

    }
}
