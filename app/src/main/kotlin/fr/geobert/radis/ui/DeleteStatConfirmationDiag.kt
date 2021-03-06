package fr.geobert.radis.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import fr.geobert.radis.tools.Tools
import kotlin.properties.Delegates
import android.app.Dialog
import android.content.DialogInterface
import fr.geobert.radis.db.StatisticTable
import android.content.Intent


public fun DeleteStatConfirmationDiag(statId: Long): DeleteStatConfirmationDiag {
    val frag = DeleteStatConfirmationDiag()
    val args = Bundle()
    args.putLong("statId", statId)
    frag.statId = statId
    frag.arguments = args
    return frag
}


class DeleteStatConfirmationDiag : DialogFragment() {
    val ctx: Context by lazy(LazyThreadSafetyMode.NONE) { activity }
    var statId: Long = 0L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        val args: Bundle = arguments
        statId = args.getLong("statId")
        return Tools.createDeleteConfirmationDialog(activity, { d: DialogInterface, i: Int ->
            if (StatisticTable.deleteStatistic(statId, ctx)) {
                activity.sendOrderedBroadcast(Intent(Tools.INTENT_REFRESH_STAT), null)
            }
        })
    }
}
