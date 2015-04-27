package fr.geobert.radis.data

import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.tools.readBoolean
import fr.geobert.radis.tools.writeBoolean
import kotlin.platform.platformStatic

public trait ImplParcelable : Parcelable {
    val parcels: List<MType<*>>
    override fun writeToParcel(p: Parcel, p1: Int) {
        parcels.forEach {
            when (it) {
                is MLong -> p.writeLong(it.get())
                is MInt -> p.writeInt(it.get())
                is MString -> p.writeString(it.get())
                is MBoolean -> p.writeBoolean(it.get())
            }
        }
    }

    fun readFromParcel(p: Parcel) {
        parcels.forEach {
            when (it) {
                is MBoolean -> it.set(p.readBoolean())
                is MInt -> it.set(p.readInt())
                is MLong -> it.set(p.readLong())
                is MString -> it.set(p.readString())
            }
        }
    }

    override fun describeContents(): Int = 0

}
