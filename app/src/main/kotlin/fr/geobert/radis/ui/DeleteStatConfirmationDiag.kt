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
    frag.setArguments(args)
    return frag
}


class DeleteStatConfirmationDiag : DialogFragment() {
    val ctx: Context by Delegates.lazy { getActivity() }
    var statId: Long = 0L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)
        val args: Bundle = getArguments() as Bundle
        statId = args.getLong("statId")
        return Tools.createDeleteConfirmationDialog(getActivity(), object : DialogInterface.OnClickListener {
            override fun onClick(p0: DialogInterface, p1: Int) {
                if (StatisticTable.deleteStatistic(statId, ctx)) {
                    getActivity().sendOrderedBroadcast(Intent(Tools.INTENT_REFRESH_NEEDED), null)
                }
            }
        })
    }
}
