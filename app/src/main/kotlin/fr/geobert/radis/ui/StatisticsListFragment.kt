package fr.geobert.radis.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.R
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.service.OnRefreshReceiver
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.ui.adapter.StatisticAdapter
import fr.geobert.radis.ui.editor.StatisticEditor
import kotlin.properties.Delegates

class StatisticsListFragment : BaseFragment(), LoaderCallbacks<Cursor> {
    val ctx: FragmentActivity by lazy(LazyThreadSafetyMode.NONE) { activity }
    private val STAT_LOADER = 2020
    private var mContainer: View by Delegates.notNull()
    private var mList: RecyclerView by Delegates.notNull()
    private val mEmptyView: View by lazy(LazyThreadSafetyMode.NONE) { mContainer.findViewById(R.id.empty_textview) }
    private var mAdapter: StatisticAdapter? = null
    private var mLoader: Loader<Cursor>? = null
    private val mOnRefreshReceiver by lazy(LazyThreadSafetyMode.NONE) { OnRefreshReceiver(this) }

    override fun setupIcon() = setIcon(R.drawable.stat_48)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val v = inflater?.inflate(R.layout.statistics_list_fragment, container, false) as View
        mContainer = v
        mList = v.findViewById(R.id.operation_list) as RecyclerView
        mList.layoutManager = android.support.v7.widget.LinearLayoutManager(activity)
        mList.setHasFixedSize(true)
        mList.itemAnimator = DefaultItemAnimator()

        setupIcon()
        setMenu(R.menu.operations_list_menu)

        mActivity.registerReceiver(mOnRefreshReceiver, IntentFilter(Tools.INTENT_REFRESH_STAT))
        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActivity.unregisterReceiver(mOnRefreshReceiver)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean =
            when (item?.itemId) {
                R.id.create_operation -> {
                    StatisticEditor.callMeForResult(ctx)
                    true
                }
                else ->
                    false
            }

    override fun onResume(): Unit {
        super.onResume()
        fetchStats()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            fetchStats()
        }
    }

    override fun onOperationEditorResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            fetchStats()
        }
    }

    private fun fetchStats() {
        when (mLoader) {
            null ->
                ctx.supportLoaderManager?.initLoader(STAT_LOADER, Bundle(), this)
            else ->
                ctx.supportLoaderManager?.restartLoader(STAT_LOADER, Bundle(), this)
        }
    }

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

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor): Unit {
        val a = mAdapter
        if (a == null) {
            mAdapter = StatisticAdapter(cursor, this)
            mList.adapter = mAdapter
        } else {
            a.swapCursor(cursor)
        }

        if (mAdapter?.itemCount == 0) {
            mList.visibility = View.GONE
            mEmptyView.visibility = View.VISIBLE
        } else {
            mList.visibility = View.VISIBLE
            mEmptyView.visibility = View.GONE
        }
    }
}
