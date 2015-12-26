package fr.geobert.radis.ui.adapter

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.IOperationList
import fr.geobert.radis.ui.OperationListFragment

public abstract class BaseOperationAdapter<T : Operation>(activity: MainActivity, opList: IOperationList, cursor: Cursor) :
        RecyclerView.Adapter<OpRowHolder<T>>() {
    var justClicked: Boolean = false

    val operationsList = opList
    val activity: MainActivity = activity
    protected var operations: MutableList<T> = cursor.map<T>({ operationFactory(it) })
    //    var mCellStates: Array<CellState?> = arrayOfNulls(operations.count())
    val needSeparator = opList is OperationListFragment


    protected abstract fun operationFactory(c: Cursor): T

    protected var prevExpandedPos: Int = -1
    private var _selectedPosition: Int = -1
    var selectedPosition: Int
        set(value) {
            if (value != _selectedPosition) {
                val tmpOldPos = selectedPosition
                if (_selectedPosition != -1) {
                    prevExpandedPos = _selectedPosition
                }
                _selectedPosition = value
                justClicked = value != -1
                notifyItemChanged(value)
                if (tmpOldPos != -1) {
                    notifyItemChanged(tmpOldPos)
                }
                val id = if (value > -1) operations[value].mRowId else -1
                operationsList.selectionChanged(value, id)
            }
        }
        get() {
            return _selectedPosition
        }
    val resources = activity.resources

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): OpRowHolder<T> {
        val l = LayoutInflater.from(view.context).inflate(R.layout.operation_row, view, false)
        val h = OpRowHolder(l, this)
        // HACK to workaround a glitch at the end of animation
        ExpandUpAnimation.mBg = h.separator.background;
        Tools.setViewBg(h.separator, null);
        Tools.setViewBg(h.separator, ExpandUpAnimation.mBg);
        // END HACK
        return h
    }

    override fun onBindViewHolder(viewHolder: OpRowHolder<T>, pos: Int) {
        val op = operationAt(pos)

        val transfertId: Long = op.mTransferAccountId
        val sum: Long = if (transfertId > 0) {
            viewHolder.transfertImg.visibility = View.VISIBLE
            val currentAccountId = activity.getCurrentAccountId()
            if (currentAccountId != 0L && currentAccountId == transfertId) {
                -op.mSum
            } else {
                op.mSum
            }
        } else {
            viewHolder.transfertImg.visibility = View.GONE
            op.mSum
        }
        if (sum >= 0.0) {
            viewHolder.opSum.setTextColor(resources.getColor(R.color.positiveSum))
            viewHolder.arrowImg.setImageResource(R.drawable.arrow_up16)
        } else {
            viewHolder.opSum.setTextColor(resources.getColor(R.color.op_alert))
            viewHolder.arrowImg.setImageResource(R.drawable.arrow_down16)
        }

        viewHolder.opSum.text = (sum.toDouble() / 100.0).formatSum()
        viewHolder.opDate.text = op.getDateObj().formatShortDate()

        // third party
        viewHolder.opName.text = if (activity.getCurrentAccountId() == transfertId) {
            op.mTransSrcAccName
        } else {
            op.mThirdParty
        };

        //        viewHolder.checkedImg.setVisibility(if (op.mIsChecked) View.VISIBLE else View.GONE)
    }

    override fun getItemCount(): Int = operations.count()

    fun operationAt(pos: Int): Operation = operations.elementAt(pos)

    fun filterFrom(pos: Int, up: Boolean, f: (Operation) -> Boolean): List<Operation> {
        val cut = if (up) {
            operations.reversed().drop(operations.count() - pos)
        } else {
            operations.drop(pos)
        }
        return cut.filter(f)
    }

    protected fun clearListeners(h: OpRowHolder<T>) {
        h.deleteBtn.setOnClickListener(null)
        h.varBtn.setOnClickListener(null)
        h.editBtn.setOnClickListener(null)
    }

    public fun increaseCache(c: Cursor) {
        //        val tmp = mCellStates
        operations.addAll(c.map { operationFactory(it) })
        //        mCellStates = arrayOfNulls(operations.count())
        //        if (tmp.count() <= mCellStates.count()) {
        //            System.arraycopy(tmp, 0, mCellStates, 0, tmp.count())
        //        }
        notifyDataSetChanged()
    }

    fun reset() {
        prevExpandedPos = -1
        _selectedPosition = -1
        operations.clear()
        //        mCellStates = arrayOfNulls(0)
    }

    // animations
    private fun animateSelectedPos(viewHolder: OpRowHolder<T>, pos: Int) {
        val op = operationAt(pos)
        viewHolder.view.setBackgroundResource(R.drawable.line_selected_gradient)
        //        Log.d(TAG, "animateSelectedPos: ${viewHolder.opDate.getText()}, pos:$pos, selected:$selectedPosition, selected:${op.isSelected}, prevPos:$prevExpandedPos")
        when (op.state) {
            CellState.STATE_MONTH_CELL ->
                expandSeparatorNoAnim(viewHolder)
            CellState.STATE_INFOS_CELL, CellState.STATE_MONTH_INFOS_CELL ->
                if (justClicked) {
                    justClicked = false
                    animateSeparator(viewHolder, true)
                    animateToolbar(viewHolder, true)
                    operationsList.getRecyclerView().post {
                        val l = operationsList.getListLayoutManager()
                        val firstIdx = l.findFirstVisibleItemPosition()
                        val lastIdx = l.findLastVisibleItemPosition()
                        Log.d(TAG, "firstIdx:$firstIdx, lastIdx:$lastIdx, prev:$prevExpandedPos, count:${itemCount}")
                        if (prevExpandedPos != -1 && prevExpandedPos < itemCount && (prevExpandedPos < firstIdx || prevExpandedPos > lastIdx)) {
                            operationAt(prevExpandedPos).isSelected = false
                            prevExpandedPos = -1
                        }
                    }
                } else {
                    expandToolbarNoAnim(viewHolder)
                    expandSeparatorNoAnim(viewHolder)
                }
        }
        op.isSelected = true
    }

    private fun animateNotSelectedPos(viewHolder: OpRowHolder<T>, pos: Int) {
        val op = operationAt(pos)
        viewHolder.view.setBackgroundResource(R.drawable.op_line)

        //        Log.d(TAG, "animateNotSelectedPos: ${viewHolder.opDate.getText()}, pos:$pos, selected:${op.isSelected}")//, prevPos:$prevExpandedPos")
        when (op.state) {
            CellState.STATE_MONTH_CELL -> {
                expandSeparatorNoAnim(viewHolder)
                if (op.isSelected) {
                    animateToolbar(viewHolder, false)
                } else {
                    collapseToolbarNoAnim(viewHolder)
                }
            }
            CellState.STATE_REGULAR_CELL -> {
                if (op.isSelected) {
                    animateSeparator(viewHolder, false)
                    animateToolbar(viewHolder, false)
                } else {
                    collapseSeparatorNoAnim(viewHolder)
                    collapseToolbarNoAnim(viewHolder)
                }
            }
        }
        op.isSelected = false

    }

    private val TAG = "BaseOperationAdapter"
    protected fun doAnimations(viewHolder: OpRowHolder<T>, pos: Int) {
        //        Log.d(TAG, "doAnimations: ${viewHolder.opDate.getText()}, pos:$pos")
        if (selectedPosition == pos) {
            animateSelectedPos(viewHolder, pos)
        } else {
            animateNotSelectedPos(viewHolder, pos)
        }
    }

    private fun animateSeparator(h: OpRowHolder<T>, expand: Boolean) {
        //        Log.d(TAG, "animateSeparator: ${h.opDate.getText()}, expand:$expand")
        if (needSeparator) {
            h.separator.clearAnimation()
            (h.separator.layoutParams as LinearLayout.LayoutParams).bottomMargin = -37
            val anim = ExpandUpAnimation(h.separator, 300, expand)
            h.separator.startAnimation(anim)
        }
    }

    private fun toolbarClearAnim(h: OpRowHolder<T>) {
        h.actionsCont.clearAnimation()
    }

    private fun animateToolbar(h: OpRowHolder<T>, expand: Boolean) {
        //        Log.d(TAG, "animateToolbar: ${h.opDate.getText()}, expand:$expand")
        toolbarClearAnim(h)
        val anim = ExpandAnimation(h.actionsCont, 300, expand)
        h.actionsCont.startAnimation(anim)
    }

    private fun collapseSeparatorNoAnim(h: OpRowHolder<T>) {
        //        Log.d(TAG, "collapseSeparatorNoAnim: ${h.opDate.getText()}")
        if (needSeparator) {
            h.separator.clearAnimation()
            (h.separator.layoutParams as LinearLayout.LayoutParams).bottomMargin = -50
            h.separator.visibility = View.GONE
        }
    }

    private fun collapseToolbarNoAnim(h: OpRowHolder<T>) {
        //        Log.d(TAG, "collapseToolbarNoAnim: ${h.opDate.getText()}")
        toolbarClearAnim(h)
        (h.actionsCont.layoutParams as LinearLayout.LayoutParams).bottomMargin = -84
        h.actionsCont.visibility = View.GONE
    }

    private fun expandSeparatorNoAnim(h: OpRowHolder<T>) {
        //        Log.d(TAG, "expandSeparatorNoAnim: ${h.opDate.getText()}")
        if (needSeparator) {
            h.separator.clearAnimation()
            (h.separator.layoutParams as LinearLayout.LayoutParams).bottomMargin = 0
            h.separator.visibility = View.VISIBLE
            ExpandUpAnimation.setChildrenVisibility(h.separator, View.VISIBLE)
            Tools.setViewBg(h.separator, ExpandUpAnimation.mBg)
        }
    }

    private fun expandToolbarNoAnim(h: OpRowHolder<T>) {
        //        Log.d(TAG, "expandToolbarNoAnim: ${h.opDate.getText()}")
        toolbarClearAnim(h)
        (h.actionsCont.layoutParams as LinearLayout.LayoutParams).bottomMargin = 0
        h.actionsCont.visibility = View.VISIBLE
    }

    private fun findOpPosBy(predicate: (op: T) -> Boolean): Int {
        var idx = 0
        operations.forEach {
            if (predicate(it)) {
                return idx
            } else {
                idx++
            }
        }
        return idx
    }

    public fun addOp(op: T): Int {
        var idx = findOpPosBy { o -> op.compareTo(o) > 0 }
        if (idx >= operations.size) {
            operations.add(op)
        } else {
            operations.add(idx, op)
        }
        notifyItemInserted(idx)
        return idx
    }

    fun delOp(opId: Long) {
        var idx = findOpPosBy { o -> o.mRowId == opId }
        operations.removeAt(idx)
        notifyItemRemoved(idx)

    }

    fun updateOp(op: T) {
        val idx = findOpPosBy { o -> o.mRowId == op.mRowId }
        var newIdx = findOpPosBy { o -> op.compareTo(o) > 0 }
        operations.removeAt(idx)
        if (idx < newIdx) newIdx--
        if (newIdx >= operations.size) {
            operations.add(op)
        } else {
            operations.add(newIdx, op)
        }
        if (idx == newIdx) {
            notifyItemChanged(idx)
        } else {
            notifyItemRemoved(idx)
            notifyItemInserted(newIdx)
        }
    }
}
