package fr.geobert.radis.ui

import fr.geobert.radis.{R, BaseFragment}
import android.content.Intent
import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}

class StatisticsFragment extends BaseFragment {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    setHasOptionsMenu(true)
    inflater.inflate(R.layout.statistics_fragment, container, false)
  }

  override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {}

  override def onFetchAllAccountCbk(): Unit = {}

  override def onAccountChanged(itemId: Long): Boolean = false

  override def updateDisplay(intent: Intent): Unit = {}
}
