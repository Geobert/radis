package fr.geobert.radis.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public class OnInsertionReceiver extends BroadcastReceiver {

	private UpdateDisplayInterface mActivity;

	public OnInsertionReceiver(UpdateDisplayInterface activity) {
		super();
		mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("OnInsertionReceiver", "onReceive intent : " + intent.getAction());
		mActivity.updateDisplay(intent);
	}

}
