package fr.geobert.radis.service;

import fr.geobert.radis.RadisListActivity;
import fr.geobert.radis.tools.Tools;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnInsertionReceiver extends BroadcastReceiver {

	private RadisListActivity mActivity;

	public OnInsertionReceiver(RadisListActivity activity) {
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
