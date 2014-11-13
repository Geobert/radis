package fr.geobert.radis.ui

import android.support.v4.app.DialogFragment
import fr.geobert.radis.data.Operation

import java.util.GregorianCalendar

public trait IOperationList {
    public fun getMoreOperations(startDate: GregorianCalendar?)

    public fun getDeleteConfirmationDialog(op: Operation): DialogFragment

}
