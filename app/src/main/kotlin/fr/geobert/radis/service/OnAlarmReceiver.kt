package fr.geobert.radis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

public class OnAlarmReceiver : BroadcastReceiver() {

    private val startServiceLock = Object()

    override fun onReceive(context: Context, intent: Intent) {
        synchronized (startServiceLock) {
            RadisService.acquireStaticLock(context)
            context.startService(Intent(context, RadisService::class.java))
        }
    }

}
