package fr.geobert.radis.ui

import android.database.Cursor
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v7.app.ActionBar
import android.widget.ListView
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.ui.editor.StatisticEditor
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.R
import kotlin.properties.Delegates
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.content.Intent
import android.support.v4.content.CursorLoader
import android.widget.ListAdapter

class StatisticsListFragment : BaseFragment(), LoaderCallbacks<Cursor> {
    val ctx: FragmentActivity by Delegates.lazy { getActivity() }
    private val STAT_LOADER = 2000
    private var mList: ListView by Delegates.notNull()
    private var mAdapter: StatListAdapter? = null
    private var mLoader: Loader<Cursor>? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super<BaseFragment>.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        val v = inflater?.inflate(R.layout.statistics_fragment, container, false) as View
        mList = v.findViewById(android.R.id.list) as ListView
        mList.setEmptyView(v.findViewById(android.R.id.empty))

        val actionbar: ActionBar = mActivity?.getSupportActionBar() as ActionBar
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setIcon(R.drawable.stat_48)
        return v
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?): Unit {
        super<BaseFragment>.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.operations_list_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.getItemId()) {
                R.id.create_operation -> {
                    StatisticEditor.callMeForResult(ctx)
                    true
                }
                else ->
                    false
            }

    override fun onResume(): Unit {
        super<BaseFragment>.onResume()
        fetchStats()
    }

    private fun fetchStats() {
        when (mLoader) {
            null ->
                ctx.getSupportLoaderManager()?.initLoader(2000, null, this)
            else ->
                ctx.getSupportLoaderManager()?.restartLoader(2000, null, this)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
    }

    override fun onFetchAllAccountCbk() {
    }

    override fun onAccountChanged(itemId: Long): Boolean = false

    override fun updateDisplay(intent: Intent?) {
        fetchStats()
    }

    override fun onCreateLoader(p1: Int, p2: Bundle?): Loader<Cursor>? {
        mLoader = CursorLoader(ctx, DbContentProvider.STATS_URI, StatisticTable.STAT_COLS, null, null, null)
        return mLoader
    }

    override fun onLoaderReset(p1: Loader<Cursor>?): Unit {
        mLoader?.reset()
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?): Unit {
        if (cursor != null)
            if (mAdapter == null) {
                mAdapter = StatListAdapter(ctx, cursor)
                mList.setAdapter(mAdapter as ListAdapter)
            } else {
                mAdapter?.changeCursor(cursor)
            }
    }

    override fun onSaveInstanceState(outState: Bundle?): Unit {

    }
}
