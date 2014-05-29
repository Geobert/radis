package fr.geobert.radis.ui

import fr.geobert.radis.{R, BaseFragment}
import android.content.Intent
import android.os.Bundle
import android.view._
import fr.geobert.radis.ui.editor.StatisticEditor

class StatisticsListFragment extends BaseFragment {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    setHasOptionsMenu(true)
    inflater.inflate(R.layout.statistics_fragment, container, false)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.operations_list_menu, menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.create_operation =>
        StatisticEditor.callMeForResult(mActivity)
        true
      case _ =>
        false
    }
  }

  override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {}

  override def onFetchAllAccountCbk(): Unit = {}

  override def onAccountChanged(itemId: Long): Boolean = false

  override def updateDisplay(intent: Intent): Unit = {}
}
