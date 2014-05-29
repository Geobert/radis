package fr.geobert.radis.ui.editor

import fr.geobert.radis.{R, MainActivity, BaseActivity}
import android.os.Bundle
import android.content.Intent
import fr.geobert.radis.db.StatisticTable
import android.widget._
import fr.geobert.radis.tools.FindView
import android.view.View
import android.util.Log
import fr.geobert.radis.tools.ViewUtils._
import fr.geobert.radis.data.Statistic

object StatisticEditor extends BaseActivity {
  def callMeForResult(activity: MainActivity, statId: Long = 0) = {
    val intent = new Intent(activity, classOf[StatisticEditor])
    intent.putExtra(StatisticTable.KEY_STAT_ID, statId)
    activity.startActivityForResult(intent, 0)
  }
}

class StatisticEditor extends BaseActivity with FindView {
  private var mNameEdt: EditText = null
  private var mAccountSpin: Spinner = null
  private var mFilterSpin: Spinner = null
  private var mPeriodSpin: Spinner = null
  private var mxLastCont: LinearLayout = null
  private var mAbsDateCont: LinearLayout = null
  private var mStartDate: Button = null
  private var mEndDate: Button = null
  private var mPieBtn: ImageButton = null
  private var mBarBtn: ImageButton = null
  private var mLineBtn: ImageButton = null

  private var mChartType = Statistic.CHART_PIE

  override protected def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.statistics_editor_fragment)

    mNameEdt = findView[EditText](R.id.stat_name_edt)
    mAccountSpin = findView[Spinner](R.id.stat_account_spinner)
    mFilterSpin = findView[Spinner](R.id.stat_filter_spinner)
    mPeriodSpin = findView[Spinner](R.id.stat_period_spinner)
    mxLastCont = findView[LinearLayout](R.id.absolute_date_cont)
    mAbsDateCont = findView[LinearLayout](R.id.x_last_cont)
    mPieBtn = findView[ImageButton](R.id.pie_btn)
    mBarBtn = findView[ImageButton](R.id.bar_btn)
    mLineBtn = findView[ImageButton](R.id.line_btn)
    mStartDate = findView[Button](R.id.start_date_btn)
    mEndDate = findView[Button](R.id.end_date_btn)

    mPieBtn.onClick { view: View =>
      mChartType = Statistic.CHART_PIE
      mPieBtn.setPressed(true)
      mBarBtn.setPressed(false)
      mLineBtn.setPressed(false)
    }

    mBarBtn.onClick { view: View =>
      mChartType = Statistic.CHART_BAR
      mPieBtn.setPressed(false)
      mBarBtn.setPressed(true)
      mLineBtn.setPressed(false)
    }

    mLineBtn.onClick { view: View =>
      mChartType = Statistic.CHART_LINE
      mPieBtn.setPressed(false)
      mBarBtn.setPressed(false)
      mLineBtn.setPressed(true)
    }
  }
}
