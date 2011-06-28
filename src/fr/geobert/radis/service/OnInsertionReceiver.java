package fr.geobert.radis.service;

import fr.geobert.radis.tools.UpdateDisplayInterface;
import fr.geobert.radis.tools.Tools;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnInsertionReceiver extends BroadcastReceiver {

	private UpdateDisplayInterface mActivity;

	public OnInsertionReceiver(UpdateDisplayInterface activity) {
		super();
		mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Tools.INTENT_OP_INSERTED)) {
			mActivity.updateDisplay(intent);
		}
	}

}
