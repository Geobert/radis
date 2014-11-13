package fr.geobert.radis.ui.adapter

import android.view.View
import android.support.v7.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.CheckBox
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation

public class OpRowHolder<T : Operation>(val view: View, val adapter: BaseOperationAdapter<T>) : RecyclerView.ViewHolder(view), View.OnClickListener {
    public var separator: LinearLayout = view.findViewById(R.id.separator) as LinearLayout
    public var month: TextView = view.findViewById(R.id.month) as TextView
    public var scheduledImg: ImageView = view.findViewById(R.id.op_sch_icon) as ImageView
    public var tagBuilder: StringBuilder = StringBuilder()
    public var sumAtSelection: TextView = view.findViewById(R.id.today_amount) as TextView
    public var actionsCont: View = view.findViewById(R.id.actions_cont)
    public var opName: TextView = view.findViewById(R.id.op_third_party) as TextView
    public var deleteBtn: ImageButton = this.actionsCont.findViewById(R.id.delete_op) as ImageButton
    public var editBtn: ImageButton = this.actionsCont.findViewById(R.id.edit_op) as ImageButton
    public var varBtn: ImageButton = this.actionsCont.findViewById(R.id.variable_action) as ImageButton
    public var isCheckedBox: CheckBox = view.findViewById(R.id.op_checkbox) as CheckBox
    public var checkedBoxCont: LinearLayout = view.findViewById(R.id.checking_cont) as LinearLayout
    public var checkedImg: ImageView = view.findViewById(R.id.op_checked_icon) as ImageView
    public var arrowImg: ImageView = view.findViewById(R.id.op_icon) as ImageView
    public var transfertImg: ImageView = view.findViewById(R.id.op_trans_icon) as ImageView
    public var opSum: TextView = view.findViewById(R.id.op_sum) as TextView
    public var opDate: TextView = view.findViewById(R.id.op_date) as TextView
    public var tag: TextView = view.findViewById(R.id.op_infos) as TextView

    {
        view.setOnClickListener(this)
    }

    override fun onClick(p0: View) {
        adapter.selectedPosition = getPosition()
    }

}