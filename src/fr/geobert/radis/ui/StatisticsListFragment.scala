package fr.geobert.radis.ui

import android.app.Activity
import android.app.LoaderManager.LoaderCallbacks
import android.content.{Intent, Loader}
import android.database.Cursor
import android.os.Bundle
import android.view._
import android.widget.ListView
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.ui.editor.StatisticEditor
import fr.geobert.radis.{BaseFragment, R}
import org.scaloid.common.Implicits

class StatisticsListFragment extends BaseFragment with Implicits with LoaderCallbacks[Cursor] {
  implicit def ctx: Activity = getActivity

  private var mList: ListView = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    setHasOptionsMenu(true)
    val v = inflater.inflate(R.layout.statistics_fragment, container, false)
    mList = v.find[ListView](android.R.id.list)
    mList.setAdapter(StatListAdapter(R.layout.statistic_row))
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

  override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {}

  override def onFetchAllAccountCbk(): Unit = {}

  override def onAccountChanged(itemId: Long): Boolean = false

  override def updateDisplay(intent: Intent): Unit = {}

  override def onCreateLoader(p1: Int, p2: Bundle): Loader[Cursor] = {
    null
  }

  override def onLoaderReset(p1: Loader[Cursor]): Unit = {}

  override def onLoadFinished(p1: Loader[Cursor], p2: Cursor): Unit = {}
}
