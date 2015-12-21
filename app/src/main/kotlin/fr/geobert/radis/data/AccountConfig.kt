package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.forEach
import fr.geobert.radis.ui.ConfigFragment
import kotlin.properties.getValue
import kotlin.properties.setValue


public class AccountConfig() : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()
    public var overrideInsertDate: Boolean by parcels
    public var overrideHideQuickAdd: Boolean by parcels
    public var overrideInvertQuickAddComp: Boolean by parcels
    public var overrideUseWeighedInfo: Boolean by parcels
    public var overrideNbMonthsAhead: Boolean by parcels
    public var overrideQuickAddAction: Boolean by parcels
    public var hideQuickAdd: Boolean by parcels
    public var invertQuickAddComp: Boolean by parcels
    public var useWeighedInfo: Boolean by parcels
    public var insertDate: Int by parcels
    public var nbMonthsAhead: Int by parcels
    public var quickAddAction: Int by parcels

    init {
        overrideInsertDate = false
        overrideHideQuickAdd = false
        overrideInvertQuickAddComp = false
        overrideUseWeighedInfo = false
        overrideNbMonthsAhead = false
        overrideQuickAddAction = false
        hideQuickAdd = false
        invertQuickAddComp = true
        useWeighedInfo = true
        insertDate = ConfigFragment.DEFAULT_INSERTION_DATE.toInt()
        nbMonthsAhead = ConfigFragment.DEFAULT_NB_MONTH_AHEAD
        quickAddAction = ConfigFragment.DEFAULT_QUICKADD_LONG_PRESS_ACTION
    }

    constructor(cursor: Cursor) : this() {
        fun getIdx(s: String): Int = cursor.getColumnIndex(s)
        val keyIdx = getIdx(PreferenceTable.KEY_PREFS_NAME)
        val valIdx = getIdx((PreferenceTable.KEY_PREFS_VALUE))
        val activeIdx = getIdx(PreferenceTable.KEY_PREFS_IS_ACTIVE)

        fun getBoolean(c: Cursor) = c.getInt(valIdx) == 1

        cursor.forEach {
            val k = it.getString(keyIdx)
            val active = it.getInt(activeIdx) == 1
            when (k) {
                ConfigFragment.KEY_INSERTION_DATE -> {
                    overrideInsertDate = active
                    insertDate = it.getInt(valIdx)
                }
                ConfigFragment.KEY_HIDE_OPS_QUICK_ADD -> {
                    overrideHideQuickAdd = active
                    hideQuickAdd = getBoolean(it)
                }
                ConfigFragment.KEY_USE_WEIGHTED_INFOS -> {
                    overrideUseWeighedInfo = active
                    useWeighedInfo = getBoolean(it)
                }
                ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD -> {
                    overrideInvertQuickAddComp = active
                    invertQuickAddComp = getBoolean(it)
                }
                ConfigFragment.KEY_NB_MONTH_AHEAD -> {
                    overrideNbMonthsAhead = active
                    nbMonthsAhead = it.getInt(valIdx)
                }
                ConfigFragment.KEY_QUICKADD_ACTION -> {
                    overrideQuickAddAction = active
                    quickAddAction = it.getInt(valIdx)
                }
            }
        }
    }

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    companion object {
        public val CREATOR: Parcelable.Creator<AccountConfig> = object : Parcelable.Creator<AccountConfig> {
            override fun createFromParcel(p: Parcel): AccountConfig {
                return AccountConfig(p)
            }

            override fun newArray(size: Int): Array<AccountConfig?> {
                return arrayOfNulls(size)
            }
        }
    }
}
