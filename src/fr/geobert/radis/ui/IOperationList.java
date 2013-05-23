package fr.geobert.radis.ui;

import android.database.Cursor;
import android.widget.ListView;

import java.util.GregorianCalendar;

public interface IOperationList {
    public void getMoreOperations(final GregorianCalendar startDate);

    public int getLastSelectedPosition();

    public void setLastSelectedPosition(final int position);

    public long getCurrentAccountId();

    public long computeSumFromCursor(Cursor cursor);

    public ListView getListView();
}
