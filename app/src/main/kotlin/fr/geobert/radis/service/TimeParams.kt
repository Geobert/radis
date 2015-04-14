package fr.geobert.radis.service

import android.content.Context
import android.util.Log
import fr.geobert.radis.data.Account
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.plusMonth
import fr.geobert.radis.ui.ConfigFragment
import hirondelle.date4j.DateTime
import kotlin.platform.platformStatic

data class TimeParams(val today: Long, val insertionDate: Long, val currentMonth: Long, val limitInsertionDate: Long) {
    companion object {
        platformStatic fun computeTimeParams(ctx: Context, account: Account): TimeParams {
            val today = DateTime.today(TIME_ZONE)
            val todayInMs = today.getMilliseconds(TIME_ZONE)
            val currentMonth = today.getEndOfMonth()
            val maxDayOfCurMonth = currentMonth.getDay()
            val prefs = DBPrefsManager.getInstance(ctx)

            // manage February if insertionDayOfMonth is 29, 30 or 31
            val cfgInsertDay = if (account.overrideInsertDate) account.insertDate else
                prefs.getInt(ConfigFragment.KEY_INSERTION_DATE, ConfigFragment.DEFAULT_INSERTION_DATE.toInt())
            Log.d("PrefBug", "cfgInsertDay: $cfgInsertDay / maxDayOfCurMonth = $maxDayOfCurMonth")
            val insertionDayOfMonth = if (cfgInsertDay > maxDayOfCurMonth) maxDayOfCurMonth else cfgInsertDay
            var insertionDate = DateTime.forDateOnly(today.getYear(), today.getMonth(), insertionDayOfMonth)

            //            if (todayInMs > insertionDate.getMilliseconds(TIME_ZONE)) {
            //                // what is that for?
            //                insertionDate.add(Calendar.MONTH, 1)
            //            }

            val lastInsertDate = if (account.overrideInsertDate) account.lastInsertDate else
                prefs.getLong(ConfigFragment.KEY_LAST_INSERTION_DATE, 0)
            if (lastInsertDate > insertionDate.getMilliseconds(TIME_ZONE)) {
                // can this happens?
                insertionDate = insertionDate.plusMonth(1)
            }

            val nbMonthAhead = if (account.overrideNbMonthsAhead) account.nbMonthsAhead else
                prefs.getInt(ConfigFragment.KEY_NB_MONTH_AHEAD, ConfigFragment.DEFAULT_NB_MONTH_AHEAD)
            Log.d("PrefBug", "nbMonthAhead = $nbMonthAhead / account.overrideNbMonthsAhead = ${account.overrideNbMonthsAhead}")
            val limitInsertDate = insertionDate.plusMonth(nbMonthAhead).getEndOfMonth()

            return TimeParams(todayInMs, insertionDate.getMilliseconds(TIME_ZONE),
                    currentMonth.getMilliseconds(TIME_ZONE), limitInsertDate.getMilliseconds(TIME_ZONE))
        }
    }
}

