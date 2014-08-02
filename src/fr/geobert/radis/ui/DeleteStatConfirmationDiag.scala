package fr.geobert.radis.ui

import android.app.Dialog
import android.content.{Context, DialogInterface, Intent}
import android.os.Bundle
import android.support.v4.app.DialogFragment
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.tools.Tools


object DeleteStatConfirmationDiag {
  def apply(statId: Long): DeleteStatConfirmationDiag = {
    val frag = new DeleteStatConfirmationDiag
    val args = new Bundle
    args.putLong("statId", statId)
    frag.statId = statId
    frag.setArguments(args)
    frag
  }
}

class DeleteStatConfirmationDiag extends DialogFragment {
  implicit lazy val ctx: Context = getActivity
  private var statId: Long = 0L

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    super.onCreate(savedInstanceState)
    val args: Bundle = getArguments
    statId = args.getLong("statId")
    Tools.createDeleteConfirmationDialog(getActivity, new DialogInterface.OnClickListener {
      def onClick(dialogInterface: DialogInterface, i: Int) {
        if (StatisticTable.deleteStatistic(statId)) {
          getActivity.sendOrderedBroadcast(new Intent(Tools.INTENT_REFRESH_NEEDED), null)
        }
      }
    })
  }
}
