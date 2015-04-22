package fr.geobert.radis.ui.adapter

import android.view.View
import android.widget.TextView
import fr.geobert.radis.R
import android.widget.ImageButton
import android.widget.ImageView
import android.support.v7.widget.RecyclerView
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.ui.StatisticsListFragment

class StatRowHolder(val v: View,
                    val activity: StatisticsListFragment) : RecyclerView.ViewHolder(v), View.OnClickListener {
    val nameLbl: TextView = v.findViewById(R.id.chart_name) as TextView
    val accountNameLbl: TextView = v.findViewById(R.id.chart_account_name) as TextView
    val trashBtn: ImageButton = v.findViewById(R.id.chart_delete) as ImageButton
    val editBtn: ImageButton = v.findViewById(R.id.edit_chart) as ImageButton
    val chartType: ImageView = v.findViewById(R.id.chart_type) as ImageView
    val timeScale: TextView = v.findViewById(R.id.time_scale) as TextView
    val filterName: TextView = v.findViewById(R.id.filter_lbl) as TextView
    var stat: Statistic? = null

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(p0: View) {
        val s = stat
        if (s != null) {
            activity.startActivity(activity.createChartView(s))
        }
    }
}
