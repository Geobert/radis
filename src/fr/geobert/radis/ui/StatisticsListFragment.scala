package fr.geobert.radis.ui

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.{CursorLoader, Loader}
import android.support.v7.app.ActionBar
import android.view.View.OnTouchListener
import android.view._
import android.widget.ListView
import fr.geobert.radis.db.{DbContentProvider, StatisticTable}
import fr.geobert.radis.ui.editor.StatisticEditor
import fr.geobert.radis.{BaseFragment, R}
import org.scaloid.common.Implicits

class StatisticsListFragment extends BaseFragment with LoaderCallbacks[Cursor] with Implicits {
  implicit def ctx: FragmentActivity = getActivity

  private val STAT_LOADER = 2000
  private var mList: ListView = _
  private var mAdapter: StatListAdapter = _
  private var mLoader: Option[Loader[Cursor]] = None


  //  private var mOnRefreshReceiver: OnRefreshReceiver = _
  //  private var mOnInsertionIntentFilter: IntentFilter = _
  //
  //  override def onCreate(savedInstanceState: Bundle): Unit = {
  //    super.onCreate(savedInstanceState)
  //    mOnRefreshReceiver = new OnRefreshReceiver(this)
  //    mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_REFRESH_NEEDED)
  //  }
  //
  //
  //  override def onDestroy(): Unit = {
  //    super.onDestroy()
  //    if (null != mOnRefreshReceiver) {
  //      ctx.unregisterReceiver(mOnRefreshReceiver)
  //    }
  //  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    setHasOptionsMenu(true)
    val v = inflater.inflate(R.layout.statistics_fragment, container, false)
    mList = v.find(android.R.id.list)
    mList.setEmptyView(v.find(android.R.id.empty))
    //    mList.mCanScroll = false

    val actionbar: ActionBar = mActivity.getSupportActionBar
    actionbar.setDisplayHomeAsUpEnabled(true)
    actionbar.setIcon(R.drawable.stat_48)

    v
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.operations_list_menu, menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.create_operation =>
        StatisticEditor.callMeForResult()
        true
      case _ =>
        false
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    fetchStats()
  }

  private def fetchStats() {
    mLoader match {
      case None =>
        ctx.getSupportLoaderManager.initLoader(2000, null, this)
      case Some(loader) =>
        ctx.getSupportLoaderManager.restartLoader(2000, null, this)
    }
  }

  override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {}

  override def onFetchAllAccountCbk(): Unit = {}

  override def onAccountChanged(itemId: Long): Boolean = false

  override def updateDisplay(intent: Intent): Unit = {
    fetchStats()
  }

  override def onCreateLoader(p1: Int, p2: Bundle): Loader[Cursor] = {
    mLoader = Some(new CursorLoader(ctx, DbContentProvider.STATS_URI, StatisticTable.STAT_COLS, null, null, null))
    mLoader.get
  }

  override def onLoaderReset(p1: Loader[Cursor]): Unit = {
    mLoader match {
      case Some(loader) =>
        loader.reset()
      case _ =>
    }
  }

  override def onLoadFinished(loader: Loader[Cursor], cursor: Cursor): Unit = {
    if (mAdapter == null) {
      mAdapter = StatListAdapter(cursor)
      mList.setAdapter(mAdapter)
    } else {
      mAdapter.changeCursor(cursor)
    }
  }

  override def onSaveInstanceState(outState: Bundle): Unit = {

  }
}
