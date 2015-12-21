package fr.geobert.radis.ui.adapter

import fr.geobert.radis.MainActivity
import fr.geobert.radis.ui.IOperationList
import android.database.Cursor
import fr.geobert.radis.data.ScheduledOperation
import android.view.View
import fr.geobert.radis.ui.adapter.CellState
import fr.geobert.radis.ui.editor.ScheduledOperationEditor
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.R
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.formatDate

public class SchedOpAdapter(act: MainActivity, opList: IOperationList, c: Cursor) :
        BaseOperationAdapter<ScheduledOperation>(act, opList, c) {

    override fun operationFactory(c: Cursor): ScheduledOperation = ScheduledOperation(c)

    override fun onBindViewHolder(viewHolder: OpRowHolder<ScheduledOperation>, pos: Int) {
        super.onBindViewHolder(viewHolder, pos)
        val op = operationAt(pos) as ScheduledOperation

        val b = StringBuilder()
        val periodicityUnit = op.mPeriodicityUnit
        val periodicity = op.mPeriodicity
        val endDate = op.mEndDate
        b.append(ScheduledOperation.getUnitStr(activity, periodicityUnit, periodicity));
        b.append(" - ");
        if (endDate.getMilliseconds(TIME_ZONE) > 0L) {
            b.append(endDate.formatDate());
        } else {
            b.append(activity.getString(R.string.no_end_date));
        }
        viewHolder.tag.text = b.toString();

        // date
        viewHolder.opDate.text = op.getDateObj().formatDate();

        viewHolder.isCheckedBox.visibility = View.GONE

        configureCell(op, viewHolder, pos)
        doAnimations(viewHolder, pos)
    }

    private fun configureCell(op: ScheduledOperation, h: OpRowHolder<ScheduledOperation>, position: Int) {
        val needInfos = position == selectedPosition
        h.scheduledImg.visibility = View.GONE
        op.state = if (needInfos) {
            CellState.STATE_INFOS_CELL
        } else {
            CellState.STATE_REGULAR_CELL
        }
        h.sumAtSelection.text = ""
        h.month.text = ""
        if (needInfos) {
            h.editBtn.setOnClickListener(object : View.OnClickListener {

                override fun onClick(view: View) {
                    ScheduledOperationEditor.callMeForResult(activity, op.mRowId,
                            activity.getCurrentAccountId(), ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT)
                }
            });
            h.editBtn.setOnLongClickListener(Tools.createTooltip(R.string.edit_scheduling));
            h.deleteBtn.setOnClickListener(object : View.OnClickListener {

                override fun onClick(view: View) {
                    operationsList.getDeleteConfirmationDialog(op).show(activity.supportFragmentManager,
                            "deleteOpConfirm");
                }
            });
            h.deleteBtn.setOnLongClickListener(Tools.createTooltip(R.string.delete));
            h.varBtn.visibility = View.GONE;
        } else {
            clearListeners(h);
        }
    }
}
