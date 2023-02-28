package online.taxcore.pos

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration

class MyMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        var version: Long = oldVersion

        // DynamicRealm exposes an editable schema
        val schema = realm.schema

        // Migrate to version 1: Add new fields
        if (version == 0L) {
            val taxesSchema = schema.get("Taxes")!!
            taxesSchema
                .addField("name", String::class.java, FieldAttribute.REQUIRED)
                .addField("value", String::class.java, FieldAttribute.REQUIRED)
                .addField("rate", Double::class.java, FieldAttribute.REQUIRED)

            version++
        }
    }
}
