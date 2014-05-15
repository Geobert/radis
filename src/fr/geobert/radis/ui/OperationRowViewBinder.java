package fr.geobert.radis.ui;

import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.geobert.radis.MainActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

import java.util.GregorianCalendar;


// class responsible for filling each operation row
class OperationRowViewBinder extends OpViewBinder {
    private GregorianCalendar date1 = new GregorianCalendar();
    private GregorianCalendar date2 = new GregorianCalendar();

    public OperationRowViewBinder(MainActivity mainActivity, IOperationList activity, Cursor c,
                                  CharSequence sumColName, CharSequence dateColName) {
        super(mainActivity, activity, sumColName, dateColName, R.id.op_icon, activity.getCurrentAccountId());
        initCache(c);
    }

    protected void fillTag(TextView textView, StringBuilder b, final Cursor cursor, final int columnIndex) {
        b.setLength(0);
        String s = cursor.getString(columnIndex);
        if (null != s) {
            b.append(s);
        } else {
            b.append('−');
        }
        b.append(" / ");
        s = cursor.getString(columnIndex + 1);
        if (null != s) {
            b.append(s);
        } else {
            b.append('−');
        }
        textView.setText(b.toString());
    }

    private long setSchedImg(Cursor cursor, ImageView i) {
        long res = cursor.getLong(cursor
                .getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID));
        if (res > 0) {
            i.setVisibility(View.VISIBLE);
        } else {
            i.setVisibility(View.GONE);
        }
        return res;
    }

    protected void configureCell(final Cursor cursor, OpRowHolder h) {
        final long schedId = setSchedImg(cursor, h.scheduledImg);
        h.checkedImg.setVisibility(cursor.getInt(cursor.getColumnIndex(OperationTable.KEY_OP_CHECKED)) == 1 ?
                View.VISIBLE : View.GONE);
        final int position = cursor.getPosition();
        final boolean needInfos = position == selectedPosition;
        final int op_date_idx = cursor.getColumnIndex(OperationTable.KEY_OP_DATE);
        boolean needMonth = false;
        date1.setTimeInMillis(cursor.getLong(op_date_idx));
        if (position == 0) {
            needMonth = true;
        } else {
            cursor.moveToPosition(position - 1);
            date2.setTimeInMillis(cursor.getLong(op_date_idx));
            if (date1.get(GregorianCalendar.MONTH) != date2.get(GregorianCalendar.MONTH)) {
                needMonth = true;
            }
            cursor.moveToPosition(position);
        }

        if (needInfos && needMonth) {
            mCellStates[position] = STATE_MONTH_INFOS_CELL;
        } else if (needInfos) {
            mCellStates[position] = STATE_INFOS_CELL;
        } else if (needMonth) {
            mCellStates[position] = STATE_MONTH_CELL;
        } else {
            mCellStates[position] = STATE_REGULAR_CELL;
        }

        if (needMonth) {
            h.month.setText(DateFormat.format("MMMM", date1));
        } else {
            h.month.setText(mCtx.getString(R.string.sum_at_selection));
        }

        if (needInfos) {
            h.sumAtSelection.setText(
                    Formater.getSumFormater().format((mCtx.getAccountManager().getCurrentAccountSum() +
                            operationList.computeSumFromCursor(cursor)) / 100.0d)
            );

            final Operation op = new Operation(cursor);
            h.editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO
//                    OperationEditorFragment.callMeForResult(context, op.mRowId, accountId);
                }
            });
            h.editBtn.setOnLongClickListener(Tools.createTooltip(R.string.op_edition));
            h.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    operationList.getDeleteConfirmationDialog(op).show(mCtx.getSupportFragmentManager(),
                            "deleteOpConfirm");
                }
            });
            h.deleteBtn.setOnLongClickListener(Tools.createTooltip(R.string.delete));

            int drawable;
            View.OnClickListener listener;
            View.OnLongClickListener longClickListener;
            if (schedId > 0) {
                drawable = R.drawable.edit_sched_48;
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO
//                        ScheduledOperationEditorFragment.callMeForResult(context, schedId, accountId,
//                                ScheduledOperationEditorFragment.ACTIVITY_SCH_OP_EDIT);
                    }
                };
                longClickListener = Tools.createTooltip(R.string.edit_scheduling);
            } else {
                drawable = R.drawable.sched_48;
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO
//                        ScheduledOperationEditorFragment.callMeForResult(context, op.mRowId, accountId,
//                                ScheduledOperationEditorFragment.ACTIVITY_SCH_OP_CONVERT);
                    }
                };
                longClickListener = Tools.createTooltip(R.string.convert_into_scheduling);
            }
            h.varBtn.setImageDrawable(mCtx.getResources().getDrawable(drawable));
            h.varBtn.setOnClickListener(listener);
            h.varBtn.setOnLongClickListener(longClickListener);
        } else {
            h.sumAtSelection.setText("");
            clearListeners(h);
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        final String colName = cursor.getColumnName(columnIndex);
        if (colName.equals(InfoTables.KEY_TAG_NAME)) {
            final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent().getParent()).getTag();
            fillTag((TextView) view, h.tagBuilder, cursor, columnIndex);
            configureCell(cursor, h);
            return true;
        } else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
            final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent().getParent()).getTag();
            TextView textView = h.opName;// ((TextView) view);
//            h.opName = textView;
            final long transfertId = cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
            if (transfertId > 0 && transfertId == operationList.getCurrentAccountId()) {
                textView.setText(cursor.getString(cursor.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)));
                return true;
            } else {
                String name = cursor.getString(cursor.getColumnIndex(colName));
                if (name != null) {
                    textView.setText(name);
                    return true;
                } else {
                    name = cursor.getString(cursor.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME));
                    if (name != null) {
                        textView.setText(name);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return super.setViewValue(view, cursor, columnIndex);
        }
    }
}
