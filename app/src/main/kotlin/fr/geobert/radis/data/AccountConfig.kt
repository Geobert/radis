package fr.geobert.radis.data

import android.database.Cursor
import android.util.Log
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.forEach
import fr.geobert.radis.ui.ConfigFragment

public class AccountConfig() {
    val NB_PREFS = 6 // number of couples of prefs

    var overrideInsertDate: Boolean = false
    var overrideHideQuickAdd: Boolean = false
    var overrideInvertQuickAddComp: Boolean = false
    var overrideUseWeighedInfo: Boolean = false
    var overrideNbMonthsAhead: Boolean = false
    var overrideQuickAddAction: Boolean = false
    var insertDate: Int = ConfigFragment.DEFAULT_INSERTION_DATE.toInt()
    var hideQuickAdd: Boolean = false
    var invertQuickAddComp: Boolean = true
    var useWeighedInfo: Boolean = true
    var nbMonthsAhead: Int = ConfigFragment.DEFAULT_NB_MONTH_AHEAD
    var quickAddAction: Int = ConfigFragment.DEFAULT_QUICKADD_LONG_PRESS_ACTION

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

}
