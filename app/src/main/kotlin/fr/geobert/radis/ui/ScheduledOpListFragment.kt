package fr.geobert.radis.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.ui.adapter.SchedOpAdapter
import fr.geobert.radis.ui.editor.ScheduledOperationEditor
import hirondelle.date4j.DateTime
import kotlin.properties.Delegates

public class ScheduledOpListFragment : BaseFragment(), LoaderCallbacks<Cursor>, IOperationList {
    private var mContainer: LinearLayout by Delegates.notNull()
    private var mListView: RecyclerView by Delegates.notNull()
    private var mEmptyView: View by Delegates.notNull()
    private var mListLayout: LinearLayoutManager by Delegates.notNull()
    private var mAdapter: SchedOpAdapter? = null
    private var mLoader: CursorLoader? = null
    private var mTotalLbl: TextView by Delegates.notNull()

    override fun setupIcon() = setIcon(R.drawable.sched_48)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        super<BaseFragment>.onCreateView(inflater, container, savedInstanceState)

        val ll = inflater.inflate(R.layout.scheduled_list, container, false) as LinearLayout
        mContainer = ll
        mEmptyView = ll.findViewById(R.id.empty_textview)
        setupIcon()
        setMenu(R.menu.scheduled_list_menu)

        mListView = ll.findViewById(R.id.operation_list) as RecyclerView
        mTotalLbl = ll.findViewById(R.id.sch_op_sum_total) as TextView
        mListView.setHasFixedSize(true)
        mListLayout = LinearLayoutManager(getActivity())
        //        if (mListView.getLayoutManager() == null) {
        mListView.setLayoutManager(mListLayout)
        //        }
        mListView.setItemAnimator(DefaultItemAnimator())
        return ll
    }

    private fun fetchSchOpsOfAccount() {
        Log.d("ScheduledOpListFragment", "fetchSchOpsOfAccount mLoader:$mLoader")
        if (mLoader == null) {
            getLoaderManager().initLoader<Cursor>(GET_SCH_OPS_OF_ACCOUNT, Bundle(), this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseFragment>.onSaveInstanceState(outState)
        outState.putLong("mCurrentAccount", mActivity.getCurrentAccountId())
    }

    override fun onResume() {
        super<BaseFragment>.onResume()
        Log.d("ScheduledOpListFragment", "onResume")
        mAccountManager.fetchAllAccounts(false, { fetchSchOpsOfAccount() })
    }

    override fun onAccountChanged(itemId: Long): Boolean {
        if (mAccountManager.getCurrentAccountId(getActivity()) != itemId) {
            mAccountManager.setCurrentAccountId(itemId, getActivity())
            val req = if (mActivity.getCurrentAccountId() == 0L) {
                GET_ALL_SCH_OPS
            } else {
                GET_SCH_OPS_OF_ACCOUNT
            }
            getLoaderManager().destroyLoader(req)
            fetchSchOpsOfAccount()
        }
        return true
    }

    private fun deleteSchOp(delAllOccurrences: Boolean, opId: Long) {
        val cursorOp = ScheduledOperationTable.fetchOneScheduledOp(mActivity, opId)
        val transId = cursorOp.getLong(cursorOp.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))
        var needRefresh = false
        if (delAllOccurrences) {
            ScheduledOperationTable.deleteAllOccurences(mActivity, opId)
            MainActivity.refreshAccountList(mActivity)
            needRefresh = true
        }
        if (ScheduledOperationTable.deleteScheduledOp(mActivity, opId)) {
            needRefresh = true
            mAdapter?.delOp(opId)
            setupListVisibility()
        }
        if (needRefresh) {
            if (transId > 0) {
                AccountTable.consolidateSums(mActivity, transId)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.create_operation -> {
                ScheduledOperationEditor.callMeForResult(mActivity, 0,
                        mActivity.getCurrentAccountId(), ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE)
                return true
            }
            else -> return Tools.onDefaultOptionItemSelected(mActivity, item)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? {
        val currentAccountId = mActivity.getCurrentAccountId()
        Log.d("SchedOpList", "onCreateLoader account id : $currentAccountId")
        when (id) {
            GET_ALL_SCH_OPS -> mLoader = CursorLoader(mActivity, DbContentProvider.SCHEDULED_JOINED_OP_URI,
                    ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null, null,
                    ScheduledOperationTable.SCHEDULED_OP_ORDERING)
            GET_SCH_OPS_OF_ACCOUNT -> mLoader = CursorLoader(mActivity, DbContentProvider.SCHEDULED_JOINED_OP_URI,
                    ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY,
                    "sch.${ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID} = ? OR sch.${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = ?",
                    arrayOf(java.lang.Long.toString(currentAccountId), java.lang.Long.toString(currentAccountId)),
                    ScheduledOperationTable.SCHEDULED_OP_ORDERING)
            else -> {
            }
        }
        return mLoader
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, data: Cursor) {
        val adapter = mAdapter
        if (adapter == null) {
            mAdapter = SchedOpAdapter(mActivity, this, data)
            mListView.setAdapter(mAdapter)
        } else {
            adapter.increaseCache(data)
        }
        setupListVisibility()
        computeTotal(data)
    }

    private fun setupListVisibility() {
        if (mAdapter?.getItemCount() == 0) {
            mListView.setVisibility(View.GONE)
            mEmptyView.setVisibility(View.VISIBLE)
        } else {
            mListView.setVisibility(View.VISIBLE)
            mEmptyView.setVisibility(View.GONE)
        }
    }

    private fun computeTotal(data: Cursor?) {
        var credit: Long = 0
        var debit: Long = 0
        if (data != null && data.getCount() > 0 && data.moveToFirst()) {
            do {
                var s = data.getLong(data.getColumnIndex(OperationTable.KEY_OP_SUM))
                val transId = data.getLong(data.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))
                if (transId == mActivity.getCurrentAccountId()) {
                    s = -s
                }
                if (s >= 0) {
                    credit += s
                } else {
                    debit += s
                }
            } while (data.moveToNext())
        }
        mTotalLbl.setText(getString(R.string.sched_op_total_sum).format(
                (credit.toDouble() / 100.0).formatSum(),
                (debit.toDouble() / 100.0).formatSum(),
                ((credit + debit).toDouble() / 100.0).formatSum()))
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
        mAdapter?.reset()
        mLoader = null
    }

    override fun getMoreOperations(startDate: DateTime?) {
        // not needed
    }

    override fun getDeleteConfirmationDialog(op: Operation): DialogFragment {
        return DeleteOpConfirmationDialog.newInstance(op.mRowId, this)
    }

    override fun updateDisplay(intent: Intent?) {
        fetchSchOpsOfAccount()
    }

    override public fun onOperationEditorResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("ScheduledOpListFragment", "onOperationEditorResult req:$requestCode, result:$resultCode, adapter:$mAdapter")
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE -> {
                    val op: ScheduledOperation = data.getParcelableExtra("operation")
                    Log.d("ScheduledOpListFragment", "onOperationEditorResult ADD")
                    mAdapter?.addOp(op)
                    setupListVisibility()
                }
                ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT -> {
                    val op: ScheduledOperation = data.getParcelableExtra("operation")
                    Log.d("ScheduledOpListFragment", "onOperationEditorResult UPDATE")
                    mAdapter?.updateOp(op)
                }
                else -> Log.d("ScheduledOpListFragment", "onOperationEditorResult ELSE:$requestCode")
            }
        }
    }

    override fun getListLayoutManager(): LinearLayoutManager = mListLayout

    override fun getRecyclerView(): RecyclerView = mListView

    public class DeleteOpConfirmationDialog : DialogFragment() {
        private var operationId: Long = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreate(savedInstanceState)
            val args = getArguments()
            this.operationId = args.getLong("opId")
            val builder = AlertDialog.Builder(getActivity())
            builder.setMessage(R.string.ask_delete_occurrences).setCancelable(false).
                    setPositiveButton(R.string.del_all_occurrences, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            parentFrag.deleteSchOp(true, operationId)
                        }
                    }).setNeutralButton(R.string.cancel_delete_occurrences, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    parentFrag.deleteSchOp(false, operationId)
                }
            }).setNegativeButton(R.string.cancel_sch_deletion, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    dialog.cancel()
                }
            })
            return builder.create()
        }

        companion object {
            var parentFrag: ScheduledOpListFragment by Delegates.notNull()

            public fun newInstance(opId: Long, parentFrag: ScheduledOpListFragment): DeleteOpConfirmationDialog {
                DeleteOpConfirmationDialog.parentFrag = parentFrag
                val frag = DeleteOpConfirmationDialog()
                val args = Bundle()
                args.putLong("opId", opId)
                frag.setArguments(args)
                return frag
            }
        }

    }

    companion object {
        //        public val CURRENT_ACCOUNT: String = "accountId"
        //        private val TAG = "ScheduleOpList"

        // dialog ids
        private val GET_ALL_SCH_OPS = 900
        private val GET_SCH_OPS_OF_ACCOUNT = 910
    }
}
