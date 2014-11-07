package fr.geobert.radis.ui.adapter

import android.view.View
import android.support.v7.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.CheckBox
import fr.geobert.radis.R

public class OpRowHolder(v: View) : RecyclerView.ViewHolder(v) {
    public var separator: LinearLayout = v.findViewById(R.id.separator) as LinearLayout
    public var month: TextView = v.findViewById(R.id.month) as TextView
    public var scheduledImg: ImageView = v.findViewById(R.id.op_sch_icon) as ImageView
    public var tagBuilder: StringBuilder = StringBuilder()
    public var sumAtSelection: TextView = v.findViewById(R.id.today_amount) as TextView
    public var actionsCont: View = v.findViewById(R.id.actions_cont)
    public var opName: TextView = v.findViewById(R.id.op_third_party) as TextView
    public var deleteBtn: ImageButton = this.actionsCont.findViewById(R.id.delete_op) as ImageButton
    public var editBtn: ImageButton = this.actionsCont.findViewById(R.id.edit_op) as ImageButton
    public var varBtn: ImageButton = this.actionsCont.findViewById(R.id.variable_action) as ImageButton
    public var isCheckedBox: CheckBox = v.findViewById(R.id.op_checkbox) as CheckBox
    public var checkedBoxCont: LinearLayout = v.findViewById(R.id.checking_cont) as LinearLayout
    public var checkedImg: ImageView = v.findViewById(R.id.op_checked_icon) as ImageView
    public var arrowImg: ImageView = v.findViewById(R.id.op_icon) as ImageView
    public var transfertImg: ImageView = v.findViewById(R.id.op_trans_icon) as ImageView
    public var opSum: TextView = v.findViewById(R.id.op_sum) as TextView
    public var opDate: TextView = v.findViewById(R.id.op_date) as TextView
    public var tag: TextView = v.findViewById(R.id.op_infos) as TextView
}