package fr.geobert.radis.ui

import android.support.v4.app.DialogFragment
import fr.geobert.radis.data.Operation

import java.util.GregorianCalendar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

public trait IOperationList {
    public fun getMoreOperations(startDate: GregorianCalendar?)

    public fun getDeleteConfirmationDialog(op: Operation): DialogFragment

    public fun getListLayoutManager(): LinearLayoutManager

    public fun getRecyclerView(): RecyclerView
}
