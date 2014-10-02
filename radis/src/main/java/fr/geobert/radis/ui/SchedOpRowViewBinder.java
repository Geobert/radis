package fr.geobert.radis.ui;

import android.database.Cursor;
import android.view.View;
import android.widget.TextView;
import fr.geobert.radis.MainActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.ui.editor.ScheduledOperationEditor;

import java.util.Date;


// class responsible for filling each operation row
class SchedOpRowViewBinder extends OpViewBinder {

    public SchedOpRowViewBinder(MainActivity mainActivity, IOperationList activity, Cursor c,
                                CharSequence sumColName, CharSequence dateColName) {
        super(mainActivity, activity, sumColName, dateColName, R.id.op_icon, activity.getCurrentAccountId());
        initCache(c);
    }

    private void configureCell(final Cursor cursor, OpRowHolder h) {
        final int position = cursor.getPosition();
        final boolean needInfos = position == selectedPosition;
        h.scheduledImg.setVisibility(View.GONE);
        if (needInfos) {
            mCellStates[position] = STATE_INFOS_CELL;
        } else {
            mCellStates[position] = STATE_REGULAR_CELL;
        }
        h.sumAtSelection.setText("");
        h.month.setText("");
        if (needInfos) {
            final Operation op = new Operation(cursor);
            h.editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ScheduledOperationEditor.callMeForResult(mActivity, op.mRowId, mActivity.getCurrentAccountId(),
                            ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT);
                }
            });
            h.editBtn.setOnLongClickListener(Tools.createTooltip(R.string.edit_scheduling));
            h.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    operationList.getDeleteConfirmationDialog(op).show(mCtx.getSupportFragmentManager(),
                            "deleteOpConfirm");
                }
            });
            h.deleteBtn.setOnLongClickListener(Tools.createTooltip(R.string.delete));
            h.varBtn.setVisibility(View.GONE);
        } else {
            clearListeners(h);
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        final String colName = cursor.getColumnName(columnIndex);
        final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent().getParent()).getTag();
        if (colName.equals(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT)) {
            StringBuilder b = new StringBuilder();
            int periodicityUnit = cursor.getInt(columnIndex);
            int periodicity = cursor.getInt(columnIndex - 1);
            long endDate = cursor.getLong(columnIndex - 2);
            b.append(ScheduledOperation.getUnitStr(mCtx, periodicityUnit, periodicity));
            b.append(" - ");
            if (endDate > 0) {
                b.append(Formater.getFullDateFormater().format(
                        new Date(endDate)));
            } else {
                b.append(mCtx.getString(R.string.no_end_date));
            }
            ((TextView) view).setText(b.toString());
            configureCell(cursor, h);
            return true;
        } else if (colName.equals(mDateColName)) {
            Date date = new Date(cursor.getLong(columnIndex));
            ((TextView) view).setText(Formater.getFullDateFormater().format(date));
            return true;
        } else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
            if (operationList.getCurrentAccountId() == cursor.getLong(cursor
                    .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))) {
                h.opName.setText(cursor.getString(cursor
                        .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)));
            } else {
                h.opName.setText(cursor.getString(columnIndex));
            }
            return true;
        } else {
            return super.setViewValue(view, cursor, columnIndex);
        }
    }
}
