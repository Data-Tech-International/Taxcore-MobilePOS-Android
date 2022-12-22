package online.taxcore.pos.data.realm

import android.os.Parcel
import android.os.Parcelable
import io.realm.RealmObject

open class Taxes() : RealmObject(), Parcelable {
    var name = ""
    var code = ""
    var isChecked = false
    var rate = 0.0
    var value: String = ""

    constructor(parcel: Parcel) : this() {
        code = parcel.readString() ?: ""
        isChecked = parcel.readByte() != 0.toByte()
        name = parcel.readString() ?: ""
        rate = parcel.readDouble()
        value = parcel.readString() ?: ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(code)
        parcel.writeByte(if (isChecked) 1 else 0)
        parcel.writeString(name)
        parcel.writeDouble(rate)
        parcel.writeString(value)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Taxes> {
        override fun createFromParcel(parcel: Parcel) = Taxes(parcel)

        override fun newArray(size: Int): Array<Taxes?> = arrayOfNulls(size)
    }
}
