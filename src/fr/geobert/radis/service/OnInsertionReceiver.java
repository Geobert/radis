package fr.geobert.radis.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public class OnInsertionReceiver extends BroadcastReceiver {

    private UpdateDisplayInterface mActivity;

    public OnInsertionReceiver(UpdateDisplayInterface activity) {
        super();
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mActivity.updateDisplay(intent);
    }

}
