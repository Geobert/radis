package fr.geobert.radis.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.Formater
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.ui.adapter.SchedOpAdapter
import fr.geobert.radis.ui.editor.ScheduledOperationEditor

import java.util.GregorianCalendar
import kotlin.properties.Delegates
import android.support.v7.widget.LinearLayoutManager

public class ScheduledOpListFragment : BaseFragment(), LoaderCallbacks<Cursor>, IOperationList {
    private var mListView: RecyclerView by Delegates.notNull()
    private val mListLayout by Delegates.lazy { LinearLayoutManager(getActivity()) }
    private var mAdapter: SchedOpAdapter? = null
    private var mLoader: CursorLoader? = null
    private var mTotalLbl: TextView by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View {
        super<BaseFragment>.onCreateView(inflater, container, savedInstanceState)

        setHasOptionsMenu(true)

        val ll = inflater.inflate(R.layout.scheduled_list, container, false) as LinearLayout

        val actionbar = mActivity.getSupportActionBar()
        actionbar.setIcon(R.drawable.sched_48)

        mListView = ll.findViewById(android.R.id.list) as RecyclerView
        mTotalLbl = ll.findViewById(R.id.sch_op_sum_total) as TextView
        mListView.setHasFixedSize(true)


        //        mListView.setEmptyView(ll.findViewById(android.R.id.empty));
        //        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        //            @Override
        //            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //                selectOpAndAdjustOffset(i);
        //            }
        //        });

        return ll
    }

    private fun selectOpAndAdjustOffset(position: Int) {
        val adapter = mAdapter
        if (adapter != null) {
            adapter.selectedPosition = position
            mListView.smoothScrollToPosition(position + 2) // scroll in order to see fully expanded op row
        }
    }

    private fun fetchSchOpsOfAccount() {
        if (mLoader == null) {
            getLoaderManager().initLoader<Cursor>(GET_SCH_OPS_OF_ACCOUNT, null, this)
        } else {
            getLoaderManager().restartLoader<Cursor>(GET_SCH_OPS_OF_ACCOUNT, null, this)
        }
    }

    //    private void populateAccountSpinner() {
    //        Cursor c = mAccountManager.getAllAccountsCursor();
    //        if (c != null && c.moveToFirst()) {
    //            mAccountAdapter = new SimpleCursorAdapter(this, R.layout.sch_account_row, c,
    //                    new String[]{AccountTable.KEY_ACCOUNT_NAME},
    //                    new int[]{android.R.id.text1}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
    //            mAccountManager.setSimpleCursorAdapter(mAccountAdapter);
    //            if (mCurrentAccount != 0) {
    //                int pos = 0;
    //                while (pos < mAccountAdapter.getCount()) {
    //                    long id = mAccountAdapter.getItemId(pos);
    //                    if (id == mCurrentAccount) {
    //                        mActivity.getSupportActionBar().setSelectedNavigationItem(pos);
    //                        break;
    //                    } else {
    //                        pos++;
    //                    }
    //                }
    //            }
    //
    //            getSupportActionBar().setListNavigationCallbacks(mAccountAdapter, new ActionBar.OnNavigationListener() {
    //                @Override
    //                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    //                    mCurrentAccount = itemId;
    //                    fetchSchOpsOfAccount();
    //                    return true;
    //                }
    //            });
    //        }
    //    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        // nothing ?
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseFragment>.onSaveInstanceState(outState)
        outState.putLong("mCurrentAccount", mActivity.getCurrentAccountId())
    }

    override fun onResume() {
        super<BaseFragment>.onResume()
        mAccountManager.fetchAllAccounts(mActivity, false, object : Runnable {
            override fun run() {
                //                populateAccountSpinner();
                fetchSchOpsOfAccount()
            }
        })
    }

    override fun onAccountChanged(itemId: Long): Boolean {
        fetchSchOpsOfAccount()
        return true
    }

    private fun deleteSchOp(delAllOccurrences: Boolean, opId: Long) {
        val cursorOp = ScheduledOperationTable.fetchOneScheduledOp(mActivity, opId)
        val transId = cursorOp.getLong(cursorOp.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))
        var needRefresh = false
        if (delAllOccurrences) {
            ScheduledOperationTable.deleteAllOccurences(mActivity, opId)
            needRefresh = true
            MainActivity.refreshAccountList(mActivity)
        }
        if (ScheduledOperationTable.deleteScheduledOp(mActivity, opId)) {
            needRefresh = true
        }
        if (needRefresh) {
            val req: Int
            if (mActivity.getCurrentAccountId() == 0L) {
                req = GET_ALL_SCH_OPS
            } else {
                req = GET_SCH_OPS_OF_ACCOUNT
            }
            if (mLoader != null) {
                getLoaderManager().restartLoader<Cursor>(req, null, this)
            } else {
                getLoaderManager().initLoader<Cursor>(req, null, this)
            }
            if (transId > 0) {
                AccountTable.consolidateSums(mActivity, transId)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.getItemId()) {
            R.id.create_operation -> {
                ScheduledOperationEditor.callMeForResult(mActivity, 0,
                        mActivity.getCurrentAccountId(), ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE)
                return true
            }
            else -> return Tools.onDefaultOptionItemSelected(mActivity, item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.scheduled_list_menu, menu)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? {
        val currentAccountId = mActivity.getCurrentAccountId()
        when (id) {
            GET_ALL_SCH_OPS -> mLoader = CursorLoader(mActivity, DbContentProvider.SCHEDULED_JOINED_OP_URI,
                    ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null, null,
                    ScheduledOperationTable.SCHEDULED_OP_ORDERING)
            GET_SCH_OPS_OF_ACCOUNT -> mLoader = CursorLoader(mActivity, DbContentProvider.SCHEDULED_JOINED_OP_URI,
                    ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY,
                    "sch.${ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID} = ? OR sch.${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = ?",
                    array(java.lang.Long.toString(currentAccountId), java.lang.Long.toString(currentAccountId)),
                    ScheduledOperationTable.SCHEDULED_OP_ORDERING)
            else -> {
            }
        }
        return mLoader
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, data: Cursor) {
        if (mAdapter == null) {
            mAdapter = SchedOpAdapter(mActivity, this, data)
            mListView.setAdapter(mAdapter)
        }
        computeTotal(data)
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
                Formater.getSumFormater().format(credit.toDouble() / 100.0),
                Formater.getSumFormater().format(debit.toDouble() / 100.0),
                Formater.getSumFormater().format((credit + debit).toDouble() / 100.0)))
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
    }

    override fun getMoreOperations(startDate: GregorianCalendar) {
        // not needed
    }

    override fun getDeleteConfirmationDialog(op: Operation): DialogFragment {
        return DeleteOpConfirmationDialog.newInstance(op.mRowId, this)
    }

    override fun updateDisplay(intent: Intent) {
    }

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

        class object {
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

    class object {
        public val CURRENT_ACCOUNT: String = "accountId"
        private val TAG = "ScheduleOpList"

        // dialog ids
        private val GET_ALL_SCH_OPS = 900
        private val GET_SCH_OPS_OF_ACCOUNT = 910

        public fun callMe(ctx: Context, currentAccountId: Long) {
            val i = Intent(ctx, javaClass<ScheduledOpListFragment>())
            i.putExtra(CURRENT_ACCOUNT, currentAccountId)
            ctx.startActivity(i)
        }
    }
}
