package fr.geobert.radis.ui;

import android.support.v4.app.DialogFragment;
import fr.geobert.radis.data.Operation;

import java.util.GregorianCalendar;

public interface IOperationList {
    public void getMoreOperations(final GregorianCalendar startDate);

    public DialogFragment getDeleteConfirmationDialog(final Operation op);
}
