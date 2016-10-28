package fr.geobert.radis.data

import android.os.Parcel

class ExportCol(s: String = "") : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()

    var label: String by parcels
    var toExport: Boolean by parcels

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    init {
        label = s
        toExport = true
    }
}
