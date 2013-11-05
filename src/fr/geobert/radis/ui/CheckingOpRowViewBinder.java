package fr.geobert.radis.ui;

import android.database.Cursor;
import android.view.View;
import android.widget.CompoundButton;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.UpdateDisplayInterface;


// class responsible for filling each operation row
class CheckingOpRowViewBinder extends OperationRowViewBinder {

    private UpdateDisplayInterface updateListener = null;

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
        h.isCheckedBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                OperationTable.updateOpCheckedStatus(mCtx, cursor, b);
                if (updateListener != null) {
                    updateListener.updateDisplay(null);
                }
            }
        });
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        boolean checked = cursor.getInt(11) == 1;
        final OpRowHolder h = (OpRowHolder) ((View) view.getParent().getParent().getParent()).getTag();
        configureCell(cursor, h);
        h.isCheckedBox.setChecked(checked);
        return super.setViewValue(view, cursor, columnIndex);
    }
}
