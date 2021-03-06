package fr.geobert.radis.ui.editor

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.util.Log
import android.view.MenuItem
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.Tools

public class OperationEditor : CommonOpEditor() {
    protected var mOriginalOp: Operation? = null
    private val mEditFragment by lazy { OperationEditFragment() }

    override fun fetchOrCreateCurrentOp(cbk: (Operation) -> Unit) {
        if (mRowId > 0) {
            setTitle(R.string.op_edition)
            onOpFetchedCbks.add(cbk)
            fetchOp(GET_OP)
        } else {
            setTitle(R.string.op_creation)
            val op = Operation()
            op.mAccountId = mCurAccountId
            mCurrentOp = op
            cbk(op)
        }
    }

    override fun inflateFragment() {
        supportFragmentManager.beginTransaction().add(R.id.fragment_cont, mEditFragment).commit()
    }

    //    override fun populateFields() = mEditFragment.populateCommonFields(mCurrentOp as Operation)

    private fun onOkClicked() {
        val op = mCurrentOp
        if ((mRowId <= 0 || mOriginalOp != null) && op != null) {
            val errMsg = StringBuilder()
            if (mEditFragment.isFormValid(errMsg)) {
                fillOperationWithInputs(op)
                saveOpAndExit()
            } else {
                Tools.popError(this, errMsg.toString(), null)
            }
        }
    }

    private fun setResAndExit() {
        val res = Intent()
        val op = mCurrentOp
        if (op != null) {
            mCurrentOp?.mRowId = mRowId
            res.putExtra("operation", op)
            setResult(Activity.RESULT_OK, res)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun saveOpAndExit() {
        val op = mCurrentOp
        Log.d(TAG, "saveOpAndExit, mRowId : " + mRowId)
        if (op != null) {
            if (mRowId <= 0) {
                mRowId = OperationTable.createOp(this, op, op.mAccountId)
                setResAndExit()
            } else {
                val origOp = mOriginalOp
                if (origOp != null) {
                    if (op.equals(origOp)) {
                        setResAndExit()
                    } else {
                        if (op.mScheduledId > 0 && !op.equalsButDate(origOp)) {
                            UpdateScheduledOp.newInstance(op, mPreviousSum, mRowId).show(supportFragmentManager, "dialog")
                        } else {
                            OperationTable.updateOp(this, mRowId, op, origOp)
                            setResAndExit()
                        }
                    }
                }
            }
        }
    }

    override fun fillOperationWithInputs(operation: Operation) {
        mEditFragment.fillOperationWithInputs(operation)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.confirm -> {
                onOkClicked()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("originalOp", mOriginalOp)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mOriginalOp = savedInstanceState.getParcelable<Operation>("originalOp")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateLoader(id: Int, arg1: Bundle): Loader<Cursor> {
        var l: CursorLoader? = null
        if (l == null) {
            when (id) {
                GET_OP -> l = CursorLoader(this, Uri.parse("${DbContentProvider.OPERATION_JOINED_URI}/$mRowId"),
                        OperationTable.OP_COLS_QUERY, null, null, null)

                else -> {
                }
            }
        }
        return l as CursorLoader
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.id) {
            GET_OP -> {
                val op = if (data.moveToFirst()) {
                    mOriginalOp = Operation(data)
                    Operation(data)
                } else {
                    val o = Operation()
                    o.mAccountId = mCurAccountId
                    o
                }
                mCurrentOp = op
                onOpFetchedCbks.forEach { it(op) }
            }
            else -> {
            }
        }

    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }

    public class UpdateScheduledOp : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreateDialog(savedInstanceState)
            val act = activity as OperationEditor
            val args = arguments
            val currentOp = args.getParcelable<Operation>("currentOp")
            val previousSum = args.getLong("previousSum")
            val rowId = args.getLong("rowId")
            val builder = AlertDialog.Builder(act)
            builder.setMessage(R.string.ask_update_scheduling).setCancelable(false).setPositiveButton(R.string.update, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    val op = ScheduledOperation(currentOp, currentOp.mAccountId)
                    if (ScheduledOperationTable.updateScheduledOp(act, currentOp.mScheduledId, op, true)) {
                        AccountTable.updateProjection(act, act.mCurAccountId, op.mSum, previousSum, op.getDate(), -1L)
                    }
                    ScheduledOperationTable.updateAllOccurences(activity, op, previousSum, currentOp.mScheduledId)
                    act.setResAndExit()
                }
            }).setNeutralButton(R.string.disconnect, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    currentOp.mScheduledId = 0
                    OperationTable.updateOp(act, rowId, currentOp, act.mOriginalOp!!)
                    act.setResAndExit()
                }
            }).setNegativeButton(R.string.cancel, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    dialog.cancel()
                }
            })
            return builder.create()
        }

        companion object {
            public fun newInstance(currentOp: Operation, previousSum: Long, rowId: Long): UpdateScheduledOp {
                val frag = UpdateScheduledOp()
                val args = Bundle()
                args.putLong("previousSum", previousSum)
                args.putLong("rowId", rowId)
                args.putParcelable("currentOp", currentOp)
                frag.arguments = args
                return frag
            }
        }
    }

    companion object {
        public val NO_OPERATION: Long = 0
        public val OPERATION_EDITOR: Int = 2000
        public val OPERATION_CREATOR: Int = 2001
        private val TAG = "OperationEditor"
        private val GET_OP = 610

        public fun callMeForResult(context: BaseActivity, opId: Long, accountId: Long) {
            val intent = Intent(context, OperationEditor::class.java)
            intent.putExtra(CommonOpEditor.PARAM_OP_ID, opId)
            intent.putExtra(AccountEditor.PARAM_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, if (opId == NO_OPERATION) OPERATION_CREATOR else OPERATION_EDITOR)
        }
    }
}

