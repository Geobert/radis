package fr.geobert.radis.ui

import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import fr.geobert.radis.data.Operation
import hirondelle.date4j.DateTime

public trait IOperationList {
    public fun getMoreOperations(startDate: DateTime?)

    public fun getDeleteConfirmationDialog(op: Operation): DialogFragment

    public fun getListLayoutManager(): LinearLayoutManager

    public fun getRecyclerView(): RecyclerView
}
