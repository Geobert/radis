package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.forEach
import fr.geobert.radis.tools.readBoolean
import fr.geobert.radis.tools.writeBoolean
import fr.geobert.radis.ui.ConfigFragment
import kotlin.platform.platformStatic

public class AccountConfig() : ImplParcelable {
    val NB_PREFS = 6 // number of couples of prefs

    // internal properties, exposed outside by properties at the end of the class
    // MType are for Parcelable simplification
    private var _overrideInsertDate: MBoolean = MBoolean(false)
    private var _overrideHideQuickAdd: MBoolean = MBoolean(false)
    private var _overrideInvertQuickAddComp: MBoolean = MBoolean(false)
    private var _overrideUseWeighedInfo: MBoolean = MBoolean(false)
    private var _overrideNbMonthsAhead: MBoolean = MBoolean(false)
    private var _overrideQuickAddAction: MBoolean = MBoolean(false)
    private var _hideQuickAdd: MBoolean = MBoolean(false)
    private var _invertQuickAddComp: MBoolean = MBoolean(true)
    private var _useWeighedInfo: MBoolean = MBoolean(true)
    private var _insertDate: MInt = MInt(ConfigFragment.DEFAULT_INSERTION_DATE.toInt())
    private var _nbMonthsAhead: MInt = MInt(ConfigFragment.DEFAULT_NB_MONTH_AHEAD)
    private var _quickAddAction: MInt = MInt(ConfigFragment.DEFAULT_QUICKADD_LONG_PRESS_ACTION)

    override val parcels = listOf(_overrideInsertDate, _overrideHideQuickAdd, _overrideInvertQuickAddComp,
            _overrideUseWeighedInfo, _overrideNbMonthsAhead, _overrideQuickAddAction, _hideQuickAdd, _invertQuickAddComp,
            _useWeighedInfo, _insertDate, _nbMonthsAhead, _quickAddAction)

    constructor(cursor: Cursor) : this() {
        fun getIdx(s: String): Int = cursor.getColumnIndex(s)
        val keyIdx = getIdx(PreferenceTable.KEY_PREFS_NAME)
        val valIdx = getIdx((PreferenceTable.KEY_PREFS_VALUE))
        val activeIdx = getIdx(PreferenceTable.KEY_PREFS_IS_ACTIVE)

        fun getBoolean(c: Cursor) = c.getInt(valIdx) == 1

        //Log.d("PrefBug", "construct AccountConfig from cursor")
        cursor.forEach {
            val k = it.getString(keyIdx)
            val active = it.getInt(activeIdx) == 1
            //Log.d("PrefBug", "$k is $active")
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
        platformStatic public val CREATOR: Parcelable.Creator<AccountConfig> = object : Parcelable.Creator<AccountConfig> {
            override fun createFromParcel(p: Parcel): AccountConfig {
                return AccountConfig(p)
            }

            override fun newArray(size: Int): Array<AccountConfig?> {
                return arrayOfNulls(size)
            }
        }
    }

    // exposed interface to properties
    public var overrideInsertDate: Boolean
        get() = _overrideInsertDate.get()
        set(value) = _overrideInsertDate.set(value)
    public var overrideHideQuickAdd: Boolean
        get() = _overrideHideQuickAdd.get()
        set(value) = _overrideHideQuickAdd.set(value)
    public var overrideInvertQuickAddComp: Boolean
        get() = _overrideInvertQuickAddComp.get()
        set(value) = _overrideInvertQuickAddComp.set(value)
    public var overrideUseWeighedInfo: Boolean
        get() = _overrideUseWeighedInfo.get()
        set(value) = _overrideUseWeighedInfo.set(value)
    public var overrideNbMonthsAhead: Boolean
        get() = _overrideNbMonthsAhead.get()
        set(value) = _overrideNbMonthsAhead.set(value)
    public var overrideQuickAddAction: Boolean
        get() = _overrideQuickAddAction.get()
        set(value) = _overrideQuickAddAction.set(value)
    public var hideQuickAdd: Boolean
        get() = _hideQuickAdd.get()
        set(value) = _hideQuickAdd.set(value)
    public var invertQuickAddComp: Boolean
        get() = _invertQuickAddComp.get()
        set(value) = _invertQuickAddComp.set(value)
    public var useWeighedInfo: Boolean
        get() = _useWeighedInfo.get()
        set(value) = _useWeighedInfo.set(value)
    public var insertDate: Int
        get() = _insertDate.get()
        set(value) = _insertDate.set(value)
    public var nbMonthsAhead: Int
        get() = _nbMonthsAhead.get()
        set(value) = _nbMonthsAhead.set(value)
    public var quickAddAction: Int
        get() = _quickAddAction.get()
        set(value) = _quickAddAction.set(value)

}
