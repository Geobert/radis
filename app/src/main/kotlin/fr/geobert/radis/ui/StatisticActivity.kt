package fr.geobert.radis.ui

import fr.geobert.radis.BaseActivity
import kotlin.properties.Delegates
import fr.geobert.radis.R
import android.widget.TextView
import android.os.Bundle
import org.achartengine.ChartFactory
import org.achartengine.GraphicalView
import org.achartengine.chart.AbstractChart
import android.widget.LinearLayout
import android.view.Window

class StatisticActivity : BaseActivity() {
    class object {
        val ACCOUNT_NAME = "account_name"
        val FILTER = "filter"
        val TIME_SCALE = "time_scale"
    }
    val accountNameLbl: TextView by Delegates.lazy { findViewById(R.id.chart_account_name) as TextView }
    val filterLbl: TextView by Delegates.lazy { findViewById(R.id.filter_lbl) as TextView }
    val timeScaleLbl: TextView by Delegates.lazy { findViewById(R.id.time_scale_lbl) as TextView }
    val chartCont: LinearLayout by Delegates.lazy { findViewById(R.id.chart) as LinearLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.statistic_activity)

        val actionBar = getSupportActionBar()
        actionBar.setIcon(R.drawable.stat_48)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val extras = getIntent().getExtras()
        val title = extras.getString(ChartFactory.TITLE)
        if (title != null) {
            setTitle(title)
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        accountNameLbl.setText(extras.getString(ACCOUNT_NAME))
        filterLbl.setText(extras.getString(FILTER))
        timeScaleLbl.setText(extras.getString(TIME_SCALE))

        chartCont.addView(createCharteView(extras))
    }

    fun createCharteView(extras: Bundle): GraphicalView {
        val chart = extras.getSerializable(ChartFactory.CHART) as AbstractChart
        return GraphicalView(this, chart)
    }
}