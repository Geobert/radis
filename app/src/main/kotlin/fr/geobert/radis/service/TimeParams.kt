package fr.geobert.radis.service

import fr.geobert.radis.tools.Tools
import java.util.GregorianCalendar
import java.util.Calendar
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.RadisConfiguration
import android.content.Context
import kotlin.platform.platformStatic

data class TimeParams(val today: Long, val insertionDate: Long, val currentMonth: Long, val limitInsertionDate: Long) {
    class object {
        platformStatic fun computeTimeParams(ctx: Context): TimeParams {
            val dayOfMonth = Calendar.DAY_OF_MONTH
            val todayInMs = Tools.createClearedCalendar().getTimeInMillis()
            val currentMonth = GregorianCalendar()
            currentMonth.setTimeInMillis(todayInMs)
            val maxDayOfCurMonth = currentMonth.getActualMaximum(dayOfMonth);
            currentMonth.set(dayOfMonth, maxDayOfCurMonth)

            // manage February if insertionDayOfMonth is 29, 30 or 31
            val cfgInsertDay = DBPrefsManager.getInstance(ctx).getInt(RadisConfiguration.KEY_INSERTION_DATE,
                    Integer.parseInt(RadisConfiguration.DEFAULT_INSERTION_DATE)) as Int
            val insertionDayOfMonth = if (cfgInsertDay > maxDayOfCurMonth) maxDayOfCurMonth else cfgInsertDay
            val insertionDate = Tools.createClearedCalendar()
            insertionDate.set(dayOfMonth, insertionDayOfMonth)

            if (todayInMs > insertionDate.getTimeInMillis()) {
                // what is that for?
                insertionDate.add(Calendar.MONTH, 1)
            }

            val lastInsertDate = DBPrefsManager.getInstance(ctx).getLong(RadisConfiguration.KEY_LAST_INSERTION_DATE, 0) as Long
            if (lastInsertDate > insertionDate.getTimeInMillis()) {
                // can this happens?
                insertionDate.add(Calendar.MONTH, 1)
            }

            val limitInsertDate = GregorianCalendar()
            limitInsertDate.setTimeInMillis(insertionDate.getTimeInMillis())
            limitInsertDate.add(Calendar.MONTH, 1)
            limitInsertDate.set(dayOfMonth, limitInsertDate.getMaximum(dayOfMonth))

            return TimeParams(todayInMs, currentMonth.getTimeInMillis(),
                    insertionDate.getTimeInMillis(), limitInsertDate.getTimeInMillis())
        }
    }
}

