package fr.geobert.radis.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import fr.geobert.radis.R;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.ExpandAnimation;
import fr.geobert.radis.tools.ExpandUpAnimation;
import fr.geobert.radis.tools.Tools;

// class responsible for creating operation rows and expantion/collapse animation
class OperationsCursorAdapter extends SimpleCursorAdapter {
    private final IOperationList opListActivity;
    private int oldPos = -1;
    private OperationRowViewBinder mInnerViewBinder;
    private boolean mJustClicked = false;

    OperationsCursorAdapter(IOperationList context, int layout, String[] from, int[] to, Cursor cursor) {
        super((Context) context, layout, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        this.opListActivity = context;
        mInnerViewBinder =
                new OperationRowViewBinder(context, cursor, OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE);
        setViewBinder(mInnerViewBinder);
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        Cursor old = super.swapCursor(c);
        mInnerViewBinder.initCache(c);
        return old;
    }

    @Override
    public void changeCursor(Cursor c) {
        super.changeCursor(c);
        mInnerViewBinder.initCache(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = super.newView(context, cursor, parent);
        OpRowHolder h = new OpRowHolder();
        h.separator = (LinearLayout) v.findViewById(R.id.separator);
        h.month = (TextView) v.findViewById(R.id.month);
        h.scheduledImg = (ImageView) v.findViewById(R.id.op_sch_icon);
        h.sumAtSelection = (TextView) v.findViewById(R.id.today_amount);
        h.actionsCont = v.findViewById(R.id.actions_cont);
        h.deleteBtn = (ImageButton) h.actionsCont.findViewById(R.id.delete_op);
        h.editBtn = (ImageButton) h.actionsCont.findViewById(R.id.edit_op);
        h.varBtn = (ImageButton) h.actionsCont.findViewById(R.id.variable_action);
        v.setTag(h);
        // HACK to workaround a glitch at the end of animation
        ExpandUpAnimation.mBg = h.separator.getBackground();
        Tools.setViewBg(h.separator, null);
        Tools.setViewBg(h.separator, ExpandUpAnimation.mBg);
        // END HACK
        return v;
    }

    public void setSelectedPosition(int pos) {
        if (opListActivity.getLastSelectedPosition() != -1) {
            oldPos = opListActivity.getLastSelectedPosition();
        }
        opListActivity.setLastSelectedPosition(pos);
        if (pos != -1) {
            this.mJustClicked = true;
        }
        // inform the view of this change
        notifyDataSetChanged();
    }

    private void animateSeparator(OpRowHolder h) {
        h.separator.clearAnimation();
        ((LinearLayout.LayoutParams) h.separator.getLayoutParams()).bottomMargin = -37;
        ExpandUpAnimation anim = new ExpandUpAnimation(h.separator, 500);
        h.separator.startAnimation(anim);
    }

    private void animateToolbar(OpRowHolder h) {
        h.actionsCont.clearAnimation();
        ExpandAnimation anim = new ExpandAnimation(h.actionsCont, 500);
        h.actionsCont.startAnimation(anim);
    }

    private void collapseSeparatorNoAnim(OpRowHolder h) {
        h.separator.clearAnimation();
        ((LinearLayout.LayoutParams) h.separator.getLayoutParams()).bottomMargin = -50;
        h.separator.setVisibility(View.GONE);
    }

    private void collapseToolbarNoAnim(OpRowHolder h) {
        h.actionsCont.clearAnimation();
        ((LinearLayout.LayoutParams) h.actionsCont.getLayoutParams()).bottomMargin = -37;
        h.actionsCont.setVisibility(View.GONE);
    }

    private void expandSeparatorNoAnim(OpRowHolder h) {
        h.separator.clearAnimation();
        ((LinearLayout.LayoutParams) h.separator.getLayoutParams()).bottomMargin = 0;
        h.separator.setVisibility(View.VISIBLE);
        ExpandUpAnimation.setChildrenVisibility(h.separator, View.VISIBLE);
        Tools.setViewBg(h.separator, ExpandUpAnimation.mBg);
    }

    private void expandToolbarNoAnim(OpRowHolder h) {
        h.actionsCont.clearAnimation();
        ((LinearLayout.LayoutParams) h.actionsCont.getLayoutParams()).bottomMargin = 0;
        h.actionsCont.setVisibility(View.VISIBLE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        final int state = mInnerViewBinder.mCellStates[position];
        final OpRowHolder h = (OpRowHolder) v.getTag();
        if (opListActivity.getLastSelectedPosition() == position) {
            v.setBackgroundResource(R.drawable.line_selected_gradient);
            if (state == OperationRowViewBinder.STATE_MONTH_CELL) {
                expandSeparatorNoAnim(h);
            } else if (state == OperationRowViewBinder.STATE_INFOS_CELL) {
                if (mJustClicked) {
                    mJustClicked = false;
                    animateSeparator(h);
                    animateToolbar(h);
                    final ListView listView = opListActivity.getListView();
                    listView.post(new Runnable() {
                        @Override
                        public void run() {
                            final int firstIdx = listView.getFirstVisiblePosition();
                            final int lastIdx = listView.getLastVisiblePosition();
                            if (oldPos < firstIdx || oldPos > lastIdx) {
                                oldPos = -1;
                            }
                        }
                    });
                } else {
                    expandToolbarNoAnim(h);
                    expandSeparatorNoAnim(h);
                }
            } else if (state == OperationRowViewBinder.STATE_MONTH_INFOS_CELL) {
                expandSeparatorNoAnim(h);
                if (mJustClicked) {
                    mJustClicked = false;
                    animateToolbar(h);
                } else {
                    expandToolbarNoAnim(h);
                }
            }
        } else {
            v.setBackgroundResource(R.drawable.op_line);
            if (state == OperationRowViewBinder.STATE_MONTH_CELL) {
                expandSeparatorNoAnim(h);
                if (position == oldPos) {
                    animateToolbar(h);
                    oldPos = -1;
                } else {
                    collapseToolbarNoAnim(h);
                }
            } else if (state == OperationRowViewBinder.STATE_REGULAR_CELL) {
                if (position == oldPos) {
                    animateToolbar(h);
                    animateSeparator(h);
                    oldPos = -1;
                } else {
                    collapseSeparatorNoAnim(h);
                    collapseToolbarNoAnim(h);
                }
            }
        }
        return v;
    }
}
