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

public abstract class BaseOperationAdapter(activity: MainActivity, opList: IOperationList, cursor: Cursor) :
        RecyclerView.Adapter<fr.geobert.radis.ui.adapter.OpRowHolder>() {
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

    var selectedPosition: Int = -1
    val resources = activity.getResources()

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): OpRowHolder {
        val l = LayoutInflater.from(view.getContext()).inflate(fr.geobert.radis.R.layout.operation_row, view, false)
        return OpRowHolder(l)
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
}