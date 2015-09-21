package fr.geobert.radis.ui.adapter

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.support.v4.widget.CursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import fr.geobert.radis.R
import fr.geobert.radis.data.AccountConfig
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.tools.convertNonAscii

public class InfoAdapter(private val mCtx: Activity, private val tableUri: Uri,
                         private val colName: String, private val mIsQuickAdd: Boolean, val account: AccountConfig) :
        CursorAdapter(mCtx, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
    private var mCurrentConstraint: String? = null

    override fun convertToString(cursor: Cursor): CharSequence {
        return cursor.getString(cursor.getColumnIndex(colName))
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val text = convertToString(cursor).toString()
        (view as TextView).text = text
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.completion_line, parent, false)

        return view
    }

    override fun runQueryOnBackgroundThread(constraint: CharSequence?): Cursor {
        mCurrentConstraint = if (constraint != null) convertNonAscii(constraint.toString()) else null
        if (filterQueryProvider != null) {
            return filterQueryProvider.runQuery(constraint)
        }
        return InfoTables.fetchMatchingInfo(mCtx, tableUri, colName, mCurrentConstraint, mIsQuickAdd, account)
    }
}
