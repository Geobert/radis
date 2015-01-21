package fr.geobert.radis.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public class OnRefreshReceiver extends BroadcastReceiver {

    private UpdateDisplayInterface mActivity;

    public OnRefreshReceiver(UpdateDisplayInterface activity) {
        super();
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mActivity.updateDisplay(intent);
    }

}
