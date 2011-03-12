package fr.geobert.radis;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InstallRadisServiceReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, OnAlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

		mgr.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(),
				5000, pi); // TODO : AlarmManager.INTERVAL_DAY
		Log.d("Radis", "Radis alarm installed via " + intent.getAction());
		
	}
}
