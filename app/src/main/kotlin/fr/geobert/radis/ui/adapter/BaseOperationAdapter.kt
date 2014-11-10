package fr.geobert.radis.ui.adapter

import android.support.v7.widget.RecyclerView
import fr.geobert.radis.data.Operation
import android.database.Cursor
import fr.geobert.radis.tools.map
import android.view.View
import fr.geobert.radis.R
import fr.geobert.radis.MainActivity
import fr.geobert.radis.tools.Formater
import fr.geobert.radis.ui.IOperationList
import android.view.ViewGroup
import android.view.LayoutInflater
import fr.geobert.radis.tools.ExpandUpAnimation
import android.widget.LinearLayout
import fr.geobert.radis.tools.ExpandAnimation
import fr.geobert.radis.tools.Tools
import android.util.Log

public abstract class BaseOperationAdapter(activity: MainActivity, opList: IOperationList, cursor: Cursor) :
        RecyclerView.Adapter<fr.geobert.radis.ui.adapter.OpRowHolder>() {
    var justClicked: Boolean = false

    val operationsList = opList
    val activity: MainActivity = activity
    protected var operations: MutableCollection<Operation> = cursor.map<Operation>({ Operation(it) })
    var mCellStates: Array<CellState?> = arrayOfNulls(operations.count())

    enum class CellState {
        STATE_MONTH_CELL
        STATE_REGULAR_CELL
        STATE_INFOS_CELL
        STATE_MONTH_INFOS_CELL
    }

    protected var prevExpandedPos: Int = -1
    var selectedPosition: Int = -1
        set(value) {
            Log.d("set selection position", "value: $value - oldPos: $prevExpandedPos - selectedPosition: $selectedPosition")
            val tmpOldPos = selectedPosition
            if ($selectedPosition != -1) {
                prevExpandedPos = $selectedPosition
            }
            $selectedPosition = value

            if (value != -1) justClicked = true
            notifyItemChanged(value)
            if (tmpOldPos != -1) {
                notifyItemChanged(tmpOldPos)
            }
            Log.d("AFTER set selection position", "value: $value - oldPos: $prevExpandedPos - selectedPosition: $selectedPosition")
        }
    val resources = activity.getResources()

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): OpRowHolder {
        val l = LayoutInflater.from(view.getContext()).inflate(fr.geobert.radis.R.layout.operation_row, view, false)
        val h = OpRowHolder(l, this)
        // HACK to workaround a glitch at the end of animation
        ExpandUpAnimation.mBg = h.separator.getBackground();
        Tools.setViewBg(h.separator, null);
        Tools.setViewBg(h.separator, ExpandUpAnimation.mBg);
        // END HACK
        return h
    }

    override fun onBindViewHolder(viewHolder: OpRowHolder, pos: Int) {
        val op = operationAt(pos)

        val transfertId: Long = op.mTransferAccountId
        val sum: Long = if (transfertId > 0) {
            viewHolder.transfertImg.setVisibility(View.VISIBLE)
            val currentAccountId = activity.getCurrentAccountId()
            if (currentAccountId != 0L && currentAccountId == transfertId) {
                -op.mSum
            } else {
                op.mSum
            }
        } else {
            viewHolder.transfertImg.setVisibility(View.GONE)
            op.mSum
        }
        if (sum >= 0.0) {
            viewHolder.opSum.setTextColor(resources.getColor(R.color.positiveSum))
            viewHolder.arrowImg.setImageResource(R.drawable.arrow_up16)
        } else {
            viewHolder.opSum.setTextColor(resources.getColor(R.color.op_alert))
            viewHolder.arrowImg.setImageResource(R.drawable.arrow_down16)
        }

        viewHolder.opSum.setText(Formater.getSumFormater().format(sum.toDouble() / 100.0))
        viewHolder.opDate.setText(Formater.getShortDateFormater(activity).format(op.getDateObj()))

        // third party
        viewHolder.opName.setText(if (activity.getCurrentAccountId() == transfertId) {
            op.mTransSrcAccName
        } else {
            op.mThirdParty
        });
    }

    override fun getItemCount(): Int = operations.count()

    fun operationAt(pos: Int): Operation = operations.elementAt(pos)

    fun filterFrom(pos: Int, up: Boolean, f: (Operation) -> Boolean): List<Operation> {
        val cut = if (up) {
            operations.reverse().drop(operations.count() - pos - 1)
        } else {
            operations.drop(pos)
        }
        return cut.filter(f)
    }

    protected fun clearListeners(h: OpRowHolder) {
        h.deleteBtn.setOnClickListener(null)
        h.varBtn.setOnClickListener(null)
        h.editBtn.setOnClickListener(null)
    }

    public fun increaseCache(c: Cursor) {
        val tmp = mCellStates
        val oldCount = operations.count()
        operations = c.map { Operation(it) }
        mCellStates = arrayOfNulls(operations.count())
        System.arraycopy(tmp, 0, mCellStates, 0, tmp.count())
        notifyItemRangeInserted(oldCount, operations.count() - oldCount);
    }

    fun reset() {
        operations.clear()
        mCellStates = arrayOfNulls(0)
    }

    // animations
    protected fun doAnimations(viewHolder: OpRowHolder, pos: Int) {
        if (selectedPosition == pos) {
            if (selectedPosition != prevExpandedPos) {
                viewHolder.view.setBackgroundResource(R.drawable.line_selected_gradient)
                when (mCellStates[pos]) {
                    CellState.STATE_MONTH_CELL ->
                        expandSeparatorNoAnim(viewHolder)
                    CellState.STATE_INFOS_CELL ->
                        if (justClicked) {
                            justClicked = false
                            animateSeparator(viewHolder)
                            animateToolbar(viewHolder)
                            // TODO oldPos update ?
                        } else {
                            expandToolbarNoAnim(viewHolder)
                            expandSeparatorNoAnim(viewHolder)
                        }
                    CellState.STATE_MONTH_INFOS_CELL -> {
                        expandSeparatorNoAnim(viewHolder)
                        if (justClicked) {
                            justClicked = false
                            animateToolbar(viewHolder)
                        } else {
                            expandToolbarNoAnim(viewHolder)
                        }
                    }
                }
            }
        } else {
            viewHolder.view.setBackgroundResource(R.drawable.op_line)
            when (mCellStates[pos]) {
                CellState.STATE_MONTH_CELL -> {
                    expandSeparatorNoAnim(viewHolder)
                    if (pos == prevExpandedPos) {
                        animateToolbar(viewHolder)
                        prevExpandedPos = -1
                    } else {
                        collapseToolbarNoAnim(viewHolder)
                    }
                }
                CellState.STATE_REGULAR_CELL -> {
                    if (pos == prevExpandedPos) {
                        animateSeparator(viewHolder)
                        animateToolbar(viewHolder)
                        prevExpandedPos = -1
                    } else {
                        collapseSeparatorNoAnim(viewHolder)
                        collapseToolbarNoAnim(viewHolder)
                    }
                }
            }
        }
    }

    private fun animateSeparator(h: OpRowHolder) {
        h.separator.clearAnimation()
        (h.separator.getLayoutParams() as LinearLayout.LayoutParams).bottomMargin = -37
        val anim = ExpandUpAnimation(h.separator, 300)
        h.separator.startAnimation(anim)
    }

    private fun animateToolbar(h: OpRowHolder) {
        h.actionsCont.clearAnimation()
        val anim = ExpandAnimation(h.actionsCont, 300)
        h.actionsCont.startAnimation(anim)
    }

    private fun collapseSeparatorNoAnim(h: OpRowHolder) {
        h.separator.clearAnimation()
        (h.separator.getLayoutParams() as LinearLayout.LayoutParams).bottomMargin = -50
        h.separator.setVisibility(View.GONE)
    }

    private fun collapseToolbarNoAnim(h: OpRowHolder) {
        h.actionsCont.clearAnimation()
        (h.actionsCont.getLayoutParams() as LinearLayout.LayoutParams).bottomMargin = -37
        h.actionsCont.setVisibility(View.GONE)
    }

    private fun expandSeparatorNoAnim(h: OpRowHolder) {
        h.separator.clearAnimation()
        (h.separator.getLayoutParams() as LinearLayout.LayoutParams).bottomMargin = 0
        h.separator.setVisibility(View.VISIBLE)
        ExpandUpAnimation.setChildrenVisibility(h.separator, View.VISIBLE)
        Tools.setViewBg(h.separator, ExpandUpAnimation.mBg)
    }

    private fun expandToolbarNoAnim(h: OpRowHolder) {
        h.actionsCont.clearAnimation()
        (h.actionsCont.getLayoutParams() as LinearLayout.LayoutParams).bottomMargin = 0
        h.actionsCont.setVisibility(View.VISIBLE)
    }
}