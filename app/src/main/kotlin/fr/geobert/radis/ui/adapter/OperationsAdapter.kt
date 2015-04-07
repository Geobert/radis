package fr.geobert.radis.ui.adapter

import android.database.Cursor
import android.text.format.DateFormat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.ui.CheckingOpDashboard
import fr.geobert.radis.ui.IOperationList
import fr.geobert.radis.ui.adapter.BaseOperationAdapter.CellState
import fr.geobert.radis.ui.editor.OperationEditor
import fr.geobert.radis.ui.editor.ScheduledOperationEditor
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.NoSuchElementException

public class OperationsAdapter(activity: MainActivity, opList: IOperationList, cursor: Cursor,
                               val checkingOpDashboard: CheckingOpDashboard?) :
        BaseOperationAdapter<Operation>(activity, opList, cursor) {
    var date1 = GregorianCalendar()
    var date2 = GregorianCalendar()

    override fun operationFactory(c: Cursor): Operation = Operation(c)

    override fun onBindViewHolder(viewHolder: OpRowHolder<Operation>, pos: Int) {
        super<BaseOperationAdapter>.onBindViewHolder(viewHolder, pos)
        val op = this.operationAt(pos)
        fillTag(viewHolder.tag, viewHolder.tagBuilder, op)
        configureCell(op, viewHolder, pos)
        viewHolder.isCheckedBox.setOnCheckedChangeListener(null)
        viewHolder.isCheckedBox.setChecked(op.mIsChecked)
        viewHolder.isCheckedBox.setOnCheckedChangeListener {(compoundButton, b) ->
            //            checkingOpDashboard?.onCheckedChanged(op, b)
            OperationTable.updateOpCheckedStatus(activity, op, b)
            op.mIsChecked = b
        }
        doAnimations(viewHolder, pos)
    }

    protected fun fillTag(textView: TextView, b: StringBuilder, op: Operation) {
        b.setLength(0)
        var s: String? = op.mTag
        if (null != s && s.isNotEmpty()) {
            b.append(s)
        } else {
            b.append('−')
        }
        b.append(" / ")
        s = op.mMode
        if (null != s && s.isNotEmpty()) {
            b.append(s)
        } else {
            b.append('−')
        }
        textView.setText(b.toString())
    }

    private fun setSchedImg(op: Operation, i: ImageView): Long {
        val res = op.mScheduledId
        i.setVisibility(if (res > 0L) View.VISIBLE else View.GONE)
        return res
    }

    private fun configureCell(operation: Operation, h: OpRowHolder<Operation>, position: Int) {
        val schedId = setSchedImg(operation, h.scheduledImg)
        val needInfos = position == selectedPosition
        var needMonth = false
        date1.setTimeInMillis(operation.getDate())
        if (position == 0) {
            needMonth = true
        } else {
            val other_op = operationAt(position - 1)
            date2.setTimeInMillis(other_op.getDate())
            if (date1.get(Calendar.MONTH) != date2.get(Calendar.MONTH)) {
                needMonth = true
            }
        }

        if (needInfos && needMonth) {
            mCellStates[position] = CellState.STATE_MONTH_INFOS_CELL
        } else if (needInfos) {
            mCellStates[position] = CellState.STATE_INFOS_CELL
        } else if (needMonth) {
            mCellStates[position] = CellState.STATE_MONTH_CELL
        } else {
            mCellStates[position] = CellState.STATE_REGULAR_CELL
        }

        h.month.setText(if (needMonth) DateFormat.format("MMMM", date1) else
            activity.getString(R.string.sum_at_selection))

        if (needInfos) {
            val currentAccountId = activity.getCurrentAccountId()
            val currentAccSum = activity.mAccountManager.currentAccountSum
            val sumFromPos = computeSumFromPosition(position)
            h.sumAtSelection.setText(((currentAccSum + sumFromPos).toDouble() / 100.0).formatSum())

            h.editBtn.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    OperationEditor.callMeForResult(activity, operation.mRowId, currentAccountId)
                }
            })
            h.editBtn.setOnLongClickListener(Tools.createTooltip(R.string.op_edition))
            h.deleteBtn.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    operationsList.getDeleteConfirmationDialog(operation).show(activity.getSupportFragmentManager(),
                            "deleteOpConfirm")
                }
            })
            h.deleteBtn.setOnLongClickListener(Tools.createTooltip(R.string.delete))

            val drawable: Int
            val listener: View.OnClickListener
            val longClickListener: View.OnLongClickListener
            if (schedId > 0) {
                drawable = R.drawable.edit_sched_48
                listener = object : View.OnClickListener {
                    override fun onClick(view: View) {
                        ScheduledOperationEditor.callMeForResult(activity, schedId, currentAccountId,
                                ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT)
                    }
                }
                longClickListener = Tools.createTooltip(R.string.edit_scheduling)
            } else {
                drawable = R.drawable.sched_48
                listener = object : View.OnClickListener {
                    override fun onClick(view: View) {
                        ScheduledOperationEditor.callMeForResult(activity, operation.mRowId, currentAccountId,
                                ScheduledOperationEditor.ACTIVITY_SCH_OP_CONVERT)
                    }
                }
                longClickListener = Tools.createTooltip(R.string.convert_into_scheduling)
            }
            h.varBtn.setImageDrawable(activity.getResources().getDrawable(drawable))
            h.varBtn.setOnClickListener(listener)
            h.varBtn.setOnLongClickListener(longClickListener)
        } else {
            h.sumAtSelection.setText("")
            clearListeners(h)
        }
    }

    fun computeSumFromPosition(pos: Int): Long {
        val op = operationAt(pos)
        val opDate = op.getDate()
        val currentAccountId = activity.getCurrentAccountId()
        val projectionDate = AccountTable.getProjectionDate()
        val s = if (opDate < projectionDate || projectionDate == 0L) {
            val opList = filterFrom(pos, true) { projectionDate == 0L || it.getDate() <= projectionDate }
            -opList.fold(0L) { s, op ->
                if (op.mTransferAccountId == currentAccountId) s - op.mSum else s + op.mSum
            }
        } else {
            val opList = filterFrom(pos, false) { it.getDate() > projectionDate }
            opList.fold(0L) { s, op ->
                if (op.mTransferAccountId == currentAccountId) s - op.mSum else s + op.mSum
            }
        }
        return s
    }

    fun findLastOpBeforeDatePos(date: java.util.GregorianCalendar): Int {
        try {
            return operations.indexOf(operations.first { it.getDate() <= date.getTimeInMillis() })
        } catch(e: NoSuchElementException) {
            return 0
        }
    }

}
