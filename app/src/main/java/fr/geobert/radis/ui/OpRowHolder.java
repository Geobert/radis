package fr.geobert.radis.ui;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import fr.geobert.radis.R;

// used in InnerViewBinder
// filled in OperationCursorAdapter
class OpRowHolder {
    public LinearLayout separator;
    public TextView month;
    public ImageView scheduledImg;
    public StringBuilder tagBuilder = new StringBuilder();
    public TextView sumAtSelection;
    public View actionsCont;
    public TextView opName;
    public ImageButton deleteBtn;
    public ImageButton editBtn;
    public ImageButton varBtn;
    public CheckBox isCheckedBox;
    public LinearLayout checkedBoxCont;
    public ImageView checkedImg;

    public OpRowHolder(View v) {
        this.separator = (LinearLayout) v.findViewById(R.id.separator);
        this.month = (TextView) v.findViewById(R.id.month);
        this.scheduledImg = (ImageView) v.findViewById(R.id.op_sch_icon);
        this.checkedImg = (ImageView) v.findViewById(R.id.op_checked_icon);
        this.sumAtSelection = (TextView) v.findViewById(R.id.today_amount);
        this.actionsCont = v.findViewById(R.id.actions_cont);
        this.deleteBtn = (ImageButton) this.actionsCont.findViewById(R.id.delete_op);
        this.editBtn = (ImageButton) this.actionsCont.findViewById(R.id.edit_op);
        this.varBtn = (ImageButton) this.actionsCont.findViewById(R.id.variable_action);
        this.opName = (TextView) v.findViewById(R.id.op_third_party);
        this.isCheckedBox = (CheckBox) v.findViewById(R.id.op_checkbox);
        this.checkedBoxCont = (LinearLayout) v.findViewById(R.id.checking_cont);
    }
}
