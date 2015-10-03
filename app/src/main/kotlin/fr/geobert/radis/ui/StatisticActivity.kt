package fr.geobert.radis.ui

import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import org.achartengine.ChartFactory
import org.achartengine.GraphicalView
import org.achartengine.chart.AbstractChart
import kotlin.properties.Delegates

class StatisticActivity : BaseActivity() {
    companion object {
        val ACCOUNT_NAME = "account_name"
        val FILTER = "filter"
        val TIME_SCALE = "time_scale"
    }

    val accountNameLbl: TextView by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.chart_account_name) as TextView }
    val filterLbl: TextView by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.filter_lbl) as TextView }
    val timeScaleLbl: TextView by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.time_scale_lbl) as TextView }
    val chartCont: LinearLayout by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.chart) as LinearLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        val title = extras?.getString(ChartFactory.TITLE)
        // setContentView need different timing according to the need of title
        if (title != null) {
            setContentView(R.layout.statistic_activity)
            setTitle(title)
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.statistic_activity)
        }

        setIconOnClick(object : View.OnClickListener {
            override fun onClick(view: View) {
                onBackPressed()
            }
        })
        mToolbar.menu.clear()
        setIcon(R.drawable.ok_48)


        accountNameLbl.text = extras.getString(ACCOUNT_NAME)
        filterLbl.text = extras.getString(FILTER)
        timeScaleLbl.text = extras.getString(TIME_SCALE)

        chartCont.addView(createCharteView(extras))
    }

    fun createCharteView(extras: Bundle): GraphicalView {
        val chart = extras.getSerializable(ChartFactory.CHART) as AbstractChart
        return GraphicalView(this, chart)
    }
}
