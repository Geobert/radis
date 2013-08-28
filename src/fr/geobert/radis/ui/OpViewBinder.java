package fr.geobert.radis.ui;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import fr.geobert.radis.R;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.Formater;

import java.util.Date;

public abstract class OpViewBinder implements SimpleCursorAdapter.ViewBinder {
    protected Resources mRes;
    private CharSequence mSumColName;
    protected CharSequence mDateColName;
    private int mArrowIconId;
    protected Context mCtx;
    protected IOperationList activity;
    protected long mCurAccountId;

    protected int[] mCellStates = null;
    static final int STATE_MONTH_CELL = 1;
    static final int STATE_REGULAR_CELL = 2;
    static final int STATE_INFOS_CELL = 3;
    static final int STATE_MONTH_INFOS_CELL = 4;
    protected int selectedPosition = -1;

    public OpViewBinder(IOperationList context, CharSequence sumColName,
                        CharSequence dateColName, int arrowIconId, long curAccountId) {
        activity = context;
        mCtx = (Context) context;
        mRes = mCtx.getResources();
        mSumColName = sumColName;
        mDateColName = dateColName;
        mArrowIconId = arrowIconId;
        mCurAccountId = curAccountId;
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        String colName = cursor.getColumnName(columnIndex);
        if (colName.equals(mSumColName)) {
            TextView textView = ((TextView) view);
            ImageView i = (ImageView) ((LinearLayout) view.getParent()
                    .getParent()).findViewById(mArrowIconId);
            long sum = cursor.getLong(columnIndex);
            ImageView transImg = (ImageView) ((LinearLayout) view.getParent()
                    .getParent()).findViewById(R.id.op_trans_icon);
            final long transfertId = cursor.getLong(cursor
                    .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
            if (transfertId > 0) {
                transImg.setVisibility(View.VISIBLE);
                if (mCurAccountId != 0 && mCurAccountId == transfertId) {
                    sum = -sum;
                }
            } else {
                transImg.setVisibility(View.GONE);
            }
            if (sum >= 0.0) {
                textView.setTextColor(mRes.getColor(R.color.positiveSum));
                i.setImageResource(R.drawable.arrow_up16);
            } else {
                textView.setTextColor(mRes.getColor(R.color.op_alert));
                i.setImageResource(R.drawable.arrow_down16);
            }
            String txt = Formater.getSumFormater().format(sum / 100.0d);
            textView.setText(txt);
            return true;
        } else if (colName.equals(mDateColName)) {
            Date date = new Date(cursor.getLong(columnIndex));
            ((TextView) view).setText(Formater.getShortDateFormater(mCtx)
                    .format(date));
            return true;
        }
        return false;
    }

    protected void clearListeners(OpRowHolder h) {
        h.deleteBtn.setOnClickListener(null);
        h.varBtn.setOnClickListener(null);
        h.editBtn.setOnClickListener(null);
    }

    public void initCache(Cursor cursor) {
        mCellStates = cursor == null ? null : new int[cursor.getCount()];
    }

    public void increaseCache(Cursor c) {
        int[] tmp = mCellStates;
        initCache(c);
        System.arraycopy(tmp, 0, mCellStates, 0, tmp.length);
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    public void setCurrentAccountId(final long accountId) {
        this.mCurAccountId = accountId;
    }
}
