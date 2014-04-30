package fr.geobert.radis.ui;

import android.database.Cursor;
import android.view.View;
import android.widget.CompoundButton;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.UpdateDisplayInterface;

import java.util.ArrayList;


// class responsible for filling each operation row
class CheckingOpRowViewBinder extends OperationRowViewBinder {

    private UpdateDisplayInterface updateListener = null;
    private ArrayList<Integer> mCheckedPosition = new ArrayList<Integer>();

    public CheckingOpRowViewBinder(IOperationList activity, Cursor c,
                                   CharSequence sumColName, CharSequence dateColName) {
        super(activity, c, sumColName, dateColName);
        if (activity instanceof UpdateDisplayInterface) {
            this.updateListener = (UpdateDisplayInterface) activity;
        }
    }

    @Override
    protected void configureCell(final Cursor cursor, OpRowHolder h) {
        final int position = cursor.getPosition();
        h.scheduledImg.setVisibility(View.GONE);
        h.checkedImg.setVisibility(View.GONE);
        mCellStates[position] = STATE_REGULAR_CELL;
        h.sumAtSelection.setText("");
        h.month.setText("");
        h.checkedBoxCont.setVisibility(View.VISIBLE);
        final long opId = cursor.getLong(0);
        final long sum = cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_SUM));
        final long accId = cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_ACCOUNT_ID));
        final long transAccId = cursor.getLong(cursor.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
        h.isCheckedBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                OperationTable.updateOpCheckedStatus(mCtx, opId, sum, accId, transAccId, b);
                if (b) {
                    mCheckedPosition.add(position);
                } else {
                    mCheckedPosition.remove(position);
                }
                if (updateListener != null) {
                    updateListener.updateDisplay(null);
                }
            }
        });
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        boolean checked = cursor.getInt(11) == 1 || mCheckedPosition.indexOf(cursor.getPosition()) > -1;
        final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent().getParent()).getTag();
        h.isCheckedBox.setOnCheckedChangeListener(null);
        h.isCheckedBox.setChecked(checked);
        configureCell(cursor, h);
        return super.setViewValue(view, cursor, columnIndex);
    }

    public void setCheckedPosition(ArrayList<Integer> checkedPosition) {
        this.mCheckedPosition = checkedPosition;
    }
}
