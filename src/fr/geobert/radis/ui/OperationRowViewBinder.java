package fr.geobert.radis.ui;

import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.ui.editor.OperationEditor;
import fr.geobert.radis.ui.editor.ScheduledOperationEditor;

import java.util.GregorianCalendar;


// class responsible for filling each operation row
class OperationRowViewBinder extends OpViewBinder {
    private GregorianCalendar date1 = new GregorianCalendar();
    private GregorianCalendar date2 = new GregorianCalendar();

    public OperationRowViewBinder(IOperationList activity, Cursor c,
                                  CharSequence sumColName, CharSequence dateColName) {
        super(activity, sumColName, dateColName, R.id.op_icon, activity.getCurrentAccountId());
        initCache(c);
    }

    private void fillTag(TextView textView, StringBuilder b, final Cursor cursor, final int columnIndex) {
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

    private void configureCell(final Cursor cursor, OpRowHolder h) {
        final long schedId = setSchedImg(cursor, h.scheduledImg);
        final int position = cursor.getPosition();
        final boolean needInfos = position == selectedPosition;
        boolean needMonth = false;
        date1.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_DATE)));
        if (position == 0) {
            needMonth = true;
        } else {
            cursor.moveToPosition(position - 1);
            date2.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_DATE)));
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
                    Formater.getSumFormater().format((AccountManager.getInstance().getCurrentAccountSum() +
                            activity.computeSumFromCursor(cursor)) / 100.0d));

            final BaseActivity context = (BaseActivity) activity;
            final long opId = cursor.getLong(0);
            final long accountId = AccountManager.getInstance().getCurrentAccountId(context);
            h.editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OperationEditor.callMeForResult(context, opId, accountId);
                }
            });
            h.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.getDeleteConfirmationDialog(accountId, opId).show(context.getSupportFragmentManager(),
                            "deleteOpConfirm");
                }
            });

            int drawable;
            View.OnClickListener listener;
            if (schedId > 0) {
                drawable = R.drawable.edit_sched_48;
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ScheduledOperationEditor.callMeForResult(context, schedId, accountId,
                                ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT);
                    }
                };
            } else {
                drawable = R.drawable.sched_48;
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ScheduledOperationEditor.callMeForResult(context, opId, accountId,
                                ScheduledOperationEditor.ACTIVITY_SCH_OP_CONVERT);
                    }
                };
            }
            h.varBtn.setImageDrawable(context.getResources().getDrawable(drawable));
            h.varBtn.setOnClickListener(listener);
        } else {
            h.sumAtSelection.setText("");
            clearListeners(h);
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        final String colName = cursor.getColumnName(columnIndex);
        if (colName.equals(InfoTables.KEY_TAG_NAME)) {
            final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent()).getTag();
            fillTag((TextView) view, h.tagBuilder, cursor, columnIndex);
            configureCell(cursor, h);
            return true;
        } else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
            final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent()).getTag();
            TextView textView = h.opName;// ((TextView) view);
//            h.opName = textView;
            final long transfertId = cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
            if (transfertId > 0 && transfertId == activity.getCurrentAccountId()) {
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
