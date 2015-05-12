package fr.geobert.radis.data

import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.tools.readBoolean
import fr.geobert.radis.tools.writeBoolean
import hirondelle.date4j.DateTime
import java.util.HashMap

public trait ImplParcelable : Parcelable {
    val parcels: HashMap<String, Any?>
    override fun writeToParcel(p: Parcel, flags: Int) {
        parcels.forEach {
            when (it.value) {
                is Boolean -> p.writeBoolean(it.value as Boolean)
                is Long -> p.writeLong(it.value as Long)
                is Int -> p.writeInt(it.value as Int)
                is String -> p.writeString(it.value as String)
                is DateTime -> {
                    val d = it.value as DateTime
                    p.writeInt(d.getYear())
                    p.writeInt(d.getMonth())
                    p.writeInt(d.getDay())
                }
                else -> throw RuntimeException()
            }
        }
    }

    open fun readFromParcel(p: Parcel) {
        parcels.forEach {
            when (it.value) {
                is Boolean -> parcels[it.key] = p.readBoolean()
                is Long -> parcels[it.key] = p.readLong()
                is Int -> parcels[it.key] = p.readInt()
                is String -> parcels[it.key] = p.readString()
                is DateTime -> parcels[it.key] = DateTime.forDateOnly(p.readInt(), p.readInt(), p.readInt())
            }
        }
    }

    override fun describeContents(): Int = 0

}
