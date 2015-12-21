package fr.geobert.radis.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InstallRadisServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, OnAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, 0)
        mgr.cancel(pi)
        mgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 5000,
                AlarmManager.INTERVAL_HALF_DAY, pi)
    }
}
