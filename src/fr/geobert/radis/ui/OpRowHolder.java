package fr.geobert.radis.ui;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
}
