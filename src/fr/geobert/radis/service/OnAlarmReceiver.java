package fr.geobert.radis.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnAlarmReceiver extends BroadcastReceiver {

	private Object startServiceLock;

	@Override
	public void onReceive(Context context, Intent intent) {		
		synchronized (startServiceLock) {
			RadisService.acquireStaticLock(context);
			context.startService(new Intent(context, RadisService.class));			
		}
	}

}
