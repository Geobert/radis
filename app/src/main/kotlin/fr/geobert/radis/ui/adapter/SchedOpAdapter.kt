package fr.geobert.radis.ui.adapter

import fr.geobert.radis.MainActivity
import fr.geobert.radis.ui.IOperationList
import android.database.Cursor
import fr.geobert.radis.data.ScheduledOperation
import android.view.View
import fr.geobert.radis.ui.adapter.BaseOperationAdapter.CellState
import fr.geobert.radis.ui.editor.ScheduledOperationEditor
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.R
import fr.geobert.radis.tools.Formater

public class SchedOpAdapter(act: MainActivity, opList: IOperationList, c: Cursor) :
        BaseOperationAdapter<ScheduledOperation>(act, opList, c) {

    override fun operationFactory(c: Cursor): ScheduledOperation = ScheduledOperation(c)

    override fun onBindViewHolder(viewHolder: OpRowHolder<ScheduledOperation>, pos: Int) {
        super<BaseOperationAdapter>.onBindViewHolder(viewHolder, pos)
        val op = operationAt(pos) as ScheduledOperation

        val b = StringBuilder()
        val periodicityUnit = op.mPeriodicityUnit
        val periodicity = op.mPeriodicity
        val endDate = op.mEndDate
        b.append(ScheduledOperation.getUnitStr(activity, periodicityUnit, periodicity));
        b.append(" - ");
        if (endDate.getTimeInMillis() > 0L) {
            b.append(Formater.getFullDateFormater().format(endDate.getTime()));
        } else {
            b.append(activity.getString(R.string.no_end_date));
        }
        viewHolder.tag.setText(b.toString());

        // date
        viewHolder.opDate.setText(Formater.getFullDateFormater().format(op.getDateObj()));

        configureCell(op, viewHolder, pos)
        doAnimations(viewHolder, pos)
    }

    private fun configureCell(op: ScheduledOperation, h: OpRowHolder<ScheduledOperation>, position: Int) {
        val needInfos = position == selectedPosition
        h.scheduledImg.setVisibility(View.GONE)
        mCellStates[position] = if (needInfos) {
            CellState.STATE_INFOS_CELL
        } else {
            CellState.STATE_REGULAR_CELL
        }
        h.sumAtSelection.setText("")
        h.month.setText("")
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
                    operationsList.getDeleteConfirmationDialog(op).show(activity.getSupportFragmentManager(),
                            "deleteOpConfirm");
                }
            });
            h.deleteBtn.setOnLongClickListener(Tools.createTooltip(R.string.delete));
            h.varBtn.setVisibility(View.GONE);
        } else {
            clearListeners(h);
        }
    }
}