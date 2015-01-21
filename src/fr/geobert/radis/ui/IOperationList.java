package fr.geobert.radis.ui;

import android.database.Cursor;
import android.support.v4.app.DialogFragment;
import android.widget.ListView;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.Operation;

import java.util.GregorianCalendar;

public interface IOperationList {
    public void getMoreOperations(final GregorianCalendar startDate);

    public long getCurrentAccountId();

    public long computeSumFromCursor(Cursor cursor);

    public ListView getListView();

    public DialogFragment getDeleteConfirmationDialog(final Operation op);

    public AccountManager getAccountManager();
}