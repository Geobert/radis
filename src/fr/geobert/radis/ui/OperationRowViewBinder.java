package fr.geobert.radis.ui;

import android.app.Activity;
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
import fr.geobert.radis.tools.OpViewBinder;
import fr.geobert.radis.ui.editor.OperationEditor;
import fr.geobert.radis.ui.editor.ScheduledOperationEditor;

import java.util.GregorianCalendar;


// class responsible for filling each operation row
class OperationRowViewBinder extends OpViewBinder {
    private GregorianCalendar date1 = new GregorianCalendar();
    private GregorianCalendar date2 = new GregorianCalendar();
    private IOperationList activity;


    int[] mCellStates = null;
    static final int STATE_MONTH_CELL = 1;
    static final int STATE_REGULAR_CELL = 2;
    static final int STATE_INFOS_CELL = 3;
    static final int STATE_MONTH_INFOS_CELL = 4;

    public OperationRowViewBinder(IOperationList activity, Cursor c,
                                  CharSequence sumColName, CharSequence dateColName) {
        super((Activity) activity, sumColName, dateColName, R.id.op_icon, activity.getCurrentAccountId());
        this.activity = activity;
        initCache(c);
    }

    public void initCache(Cursor cursor) {
        mCellStates = cursor == null ? null : new int[cursor.getCount()];
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

    private boolean setSchedImg(Cursor cursor, ImageView i) {
        boolean res;
        if (cursor.getLong(cursor
                .getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)) > 0) {
            i.setVisibility(View.VISIBLE);
            res = true;
        } else {
            i.setVisibility(View.GONE);
            res = false;
        }
        return res;
    }

    private void clearListeners(OpRowHolder h) {
        h.deleteBtn.setOnClickListener(null);
        h.varBtn.setOnClickListener(null);
        h.editBtn.setOnClickListener(null);
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        final String colName = cursor.getColumnName(columnIndex);
        if (colName.equals(InfoTables.KEY_TAG_NAME)) {
            final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent()).getTag();
            fillTag((TextView) view, h.tagBuilder, cursor, columnIndex);

            final boolean isSched = setSchedImg(cursor, h.scheduledImg);
            final int position = cursor.getPosition();
            final boolean needInfos = position == activity.getLastSelectedPosition();
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
                if (isSched) {
                    drawable = R.drawable.edit_sched_48;
                    listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ScheduledOperationEditor.callMeForResult(context, opId, accountId,
                                    ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT);
                        }
                    };
                } else {
                    drawable = R.drawable.sched_48;
                    listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // TODO
                        }
                    };
                }
                h.varBtn.setImageDrawable(context.getResources().getDrawable(drawable));
                h.varBtn.setOnClickListener(listener);
            } else {
                h.sumAtSelection.setText("");
                clearListeners(h);
            }

            return true;
        } else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
            final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent()).getTag();
            TextView textView = ((TextView) view);
            h.opName = textView;
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

    public void increaseCache(Cursor c) {
        int[] tmp = mCellStates;
        initCache(c);
        System.arraycopy(tmp, 0, mCellStates, 0, tmp.length);
    }
}
