package fr.geobert.radis.data

import android.os.Parcel
import kotlin.properties.Delegates

public class ExportCol(s: String = "") : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()

    var label: String by Delegates.mapVar(parcels)
    var toExport: Boolean by Delegates.mapVar(parcels)

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    init {
        label = s
        toExport = true
    }
}
