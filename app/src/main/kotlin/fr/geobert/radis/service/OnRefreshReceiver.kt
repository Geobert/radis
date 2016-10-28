package fr.geobert.radis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.geobert.radis.tools.UpdateDisplayInterface

class OnRefreshReceiver(private val mActivity: UpdateDisplayInterface) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        mActivity.updateDisplay(intent)
    }

}
