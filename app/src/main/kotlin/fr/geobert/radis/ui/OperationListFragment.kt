package fr.geobert.radis.ui

import fr.geobert.radis.BaseFragment
import fr.geobert.radis.tools.UpdateDisplayInterface
import android.database.Cursor
import android.content.Intent
import android.os.Bundle
import java.util.GregorianCalendar
import fr.geobert.radis.data.Operation
import android.support.v4.app.DialogFragment
import android.support.v4.content.CursorLoader
import kotlin.properties.Delegates
import android.widget.LinearLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import fr.geobert.radis.R
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.RadisConfiguration
import fr.geobert.radis.tools.Tools
import java.util.Calendar
import fr.geobert.radis.db.OperationTable
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.content.Context
import fr.geobert.radis.db.DbContentProvider
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import fr.geobert.radis.ui.editor.OperationEditor
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.app.AlertDialog
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.MainActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.DefaultItemAnimator
import kotlin.platform.platformStatic

public class OperationListFragment : BaseFragment(), UpdateDisplayInterface, LoaderManager.LoaderCallbacks<Cursor>, IOperationList {
    private var freshLoader: Boolean = false

    private var mListLayout: LinearLayoutManager by Delegates.notNull()
    private val TAG = "OperationListFragment"
    private val GET_OPS = 300
    private var mOperationsLoader: CursorLoader? = null
    private var mQuickAddController: QuickAddController? = null
    private var mOpListAdapter: fr.geobert.radis.ui.adapter.OperationsAdapter? = null
    private var mListView: RecyclerView by Delegates.notNull()
    private var startOpDate: GregorianCalendar? = null // start date of ops to get
    private var mScrollLoader: OnOperationScrollLoader by Delegates.notNull()
    private var mLastSelectionId = -1L
    private var mLastSelectionPos = -1
    private var needRefreshSelection = false
    private var container: LinearLayout by Delegates.notNull()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        super<BaseFragment>.onCreateView(inflater, container, savedInstanceState)
        this.container = inflater.inflate(R.layout.operation_list, container, false) as LinearLayout

        setHasOptionsMenu(true)

        val supportActionBar = mActivity.getSupportActionBar()
        supportActionBar.setLogo(R.drawable.radis_no_disc_48)

        if (mActivity.getCurrentAccountId() != null) {
            supportActionBar.setSelectedNavigationItem(mActivity.getAccountManager().getCurrentAccountPosition(mActivity))
        }

        initOperationList()
        initQuickAdd()
        onAccountChanged(mActivity.getCurrentAccountId()!!)
        return this.container
    }

    override fun onResume() {
        super<BaseFragment>.onResume()
        val accMan = mActivity.getAccountManager()
        val curDefaultAccId = accMan.mCurDefaultAccount
        if (curDefaultAccId != null && curDefaultAccId != accMan.getDefaultAccountId(mActivity)) {
            accMan.mCurDefaultAccount = null
            accMan.setCurrentAccountId(null)
            mLastSelectionId = (-1).toLong()
        }

        val q = mQuickAddController
        if (q != null) {
            q.setAutoNegate(true)
            q.clearFocus()
            setQuickAddVisibility()
        }
    }

    private fun initQuickAdd() {
        val q = QuickAddController(mActivity, container)
        q.setAccount(mActivity.getCurrentAccountId())
        q.initViewBehavior()
        q.setAutoNegate(true)
        q.clearFocus()
        mQuickAddController = q;
        setQuickAddVisibility()
    }

    private fun setQuickAddVisibility() {
        val q = mQuickAddController
        if (q != null) {
            val hideQuickAdd = DBPrefsManager.getInstance(mActivity).getBoolean(RadisConfiguration.KEY_HIDE_OPS_QUICK_ADD)
            var visibility = View.VISIBLE
            if (hideQuickAdd) {
                visibility = View.GONE
            }
            q.setVisibility(visibility)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super<BaseFragment>.onSaveInstanceState(outState)
        mQuickAddController?.onSaveInstanceState(outState)
        if (mActivity.getCurrentAccountId() != null) {
            outState!!.putLong("mAccountId", mActivity.getCurrentAccountId()!!)
        }
    }


    override fun onStop() {
        super<BaseFragment>.onStop()
        Log.d("OperationListFragment", "onStop")
        getLoaderManager().destroyLoader(GET_OPS)
    }

    private fun initOperationList() {
        mListView = container.findViewById(android.R.id.list) as RecyclerView
        mListLayout = LinearLayoutManager(mActivity)
        mListView.setLayoutManager(mListLayout)
        mListView.setItemAnimator(DefaultItemAnimator())
        mListView.setHasFixedSize(true)
        mScrollLoader = OnOperationScrollLoader(this, mListLayout)
        mListView.setOnScrollListener(mScrollLoader)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        initQuickAdd()
        mQuickAddController?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onAccountChanged(itemId: Long): Boolean {
        if (mAccountManager == null) {
            return false
        }
        val adapter = mOpListAdapter
        if (adapter != null)
            adapter.projectionDate = mAccountManager.setCurrentAccountId(itemId)
        val startDate = Tools.createClearedCalendar()
        startDate.set(Calendar.DAY_OF_MONTH, startDate.getActualMinimum(Calendar.DAY_OF_MONTH))
        startOpDate = startDate
        mScrollLoader.setStartDate(startDate)
        val q = mQuickAddController
        if (q != null) {
            q.setAccount(itemId)
            getOperationsList()
            return true
        } else {
            initQuickAdd()
            getOperationsList()
            return false
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor>? =
            when (i) {
                GET_OPS -> {
                    mOperationsLoader = OperationTable.getOpsWithStartDateLoader(mActivity, startOpDate,
                            mActivity.getCurrentAccountId())
                    mOperationsLoader
                }
                else -> null
            }


    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.getId()) {
            GET_OPS -> {
                var refresh = false
                Log.d("OperationListFragment", "onLoadFinished $mOpListAdapter")
                val adapter = mOpListAdapter
                if (adapter == null) {
                    val a = fr.geobert.radis.ui.adapter.OperationsAdapter(mActivity, this, cursor)
                    a.projectionDate = AccountTable.getProjectionDate()
                    mOpListAdapter = a
                    refresh = true
                    mListView.setAdapter(mOpListAdapter)
                } else {
                    adapter.increaseCache(cursor)
                    Log.d("OperationListFragment", "onLoadFinished fresh $freshLoader")
                    if (freshLoader)
                        mListView.setAdapter(adapter)
                }
                freshLoader = false
                if (refresh || needRefreshSelection) {
                    needRefreshSelection = false
                    refreshSelection()
                }
            }
        }
    }

    class object {
        platformStatic public fun restart(ctx: Context) {
            DbContentProvider.reinit(ctx)
            val intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ctx.startActivity(intent)
        }
    }

    /**
     * get the operations of current account
     * should be called after getAccountList
     */
    private fun getOperationsList() {
        Log.d(TAG, "getOperationsList mActivity.getCurrentAccountId() : " + mActivity.getCurrentAccountId())

        if (mActivity.getCurrentAccountId() != null) {
            Log.d(TAG, "getOperationsList mOperationsLoader  : " + mOperationsLoader)
            if (mOperationsLoader == null) {
                freshLoader = true
                val startOpDate = Tools.createClearedCalendar()
                mScrollLoader.setStartDate(startOpDate)
                startOpDate.set(Calendar.DAY_OF_MONTH, startOpDate!!.getActualMinimum(Calendar.DAY_OF_MONTH))
                this.startOpDate = startOpDate
                Log.d(TAG, "startOpDate : " + Tools.getDateStr(startOpDate))
                getLoaderManager().initLoader<Cursor>(GET_OPS, Bundle(), this)
            } else {
                getLoaderManager().restartLoader<Cursor>(GET_OPS, Bundle(), this)
            }
        }
    }


    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
        Log.d("OperationListFragment", "onLoaderReset")
        if (!mActivity.isFinishing()) {
            when (cursorLoader.getId()) {
                GET_OPS -> {
                    Log.d("OperationListFragment", "onLoaderReset doing reset")
                    mOpListAdapter?.reset()
                    mOperationsLoader = null
                }
                else -> {
                }
            }
        }
    }

    override fun updateDisplay(intent: Intent?) {
        getOperationsList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.operations_list_menu, menu)
        //        inflater.inflate(R.menu.common_menu, menu);
        if (Tools.DEBUG_MODE) {
            inflater.inflate(R.menu.debug_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.create_operation -> {
                OperationEditor.callMeForResult(mActivity, OperationEditor.NO_OPERATION,
                        mActivity.getCurrentAccountId())
                return true
            }
            else -> return Tools.onDefaultOptionItemSelected(mActivity, item)
        }
    }

    override fun getDeleteConfirmationDialog(op: Operation): DialogFragment {
        if (op.mScheduledId > 0) {
            return DeleteOccurrenceConfirmationDialog.newInstance(op.mAccountId, op.mRowId,
                    op.mScheduledId, op.getDate(), op.mTransferAccountId, this)
        } else {
            return DeleteOpConfirmationDialog.newInstance(op.mAccountId, op.mRowId, this)
        }
    }

    private fun selectOpAndAdjustOffset(position: Int, delayScroll: Boolean) {
        Log.d("selectOpAndAdjustOffset", "pos: $position, delayScroll: $delayScroll, lastPos: $mLastSelectionPos")
        if (position != mLastSelectionPos) {
            val adapter = mOpListAdapter
            if (adapter != null) {
                mLastSelectionPos = position
                mLastSelectionId = adapter.operationAt(position).mRowId

                if (delayScroll) {
                    mListView.postDelayed(object : Runnable {
                        override fun run() {
                            mListView.post(object : Runnable {
                                override fun run() {
                                    val half = mListLayout.getChildCount() / 2
                                    Log.d("selectOpAndAdjustOffset", "half: $half")
                                    if (mListLayout.findFirstCompletelyVisibleItemPosition() >= position) {
                                        mListView.smoothScrollToPosition(position - half) // scroll in order to see fully expanded op row
                                    } else if (mListLayout.findLastCompletelyVisibleItemPosition() <= position) {
                                        mListView.smoothScrollToPosition(position + half) // scroll in order to see fully expanded op row
                                    }
                                }
                            })
                        }
                    }, 400)
                } else {
                    mListView.smoothScrollToPosition(position + 3) // scroll in order to see fully expanded op row
                }
                adapter.selectedPosition = position
            }
        }
    }

    override fun getMoreOperations(startDate: GregorianCalendar?) {
        Log.d("getMoreOperations", "startDate : " + startDate)
        if (startDate != null) {
            startOpDate = startDate
            getOperationsList()
        } else {
            // no op found with cur month and month - 1, try if there is one
            val c: Cursor?
            Log.d("getMoreOperations", "startOpDate : " + startOpDate)
            if (null == startOpDate) {
                c = OperationTable.fetchLastOp(mActivity, mActivity.getCurrentAccountId()!!)
            } else {
                c = OperationTable.fetchLastOpSince(mActivity, mActivity.getCurrentAccountId()!!, startOpDate!!.getTimeInMillis())
            }
            Log.d("getMoreOperations", "cursor : " + c)
            if (c != null) {
                if (c.moveToFirst()) {
                    val date = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_DATE))
                    Log.d(TAG, "last chance date : " + Tools.getDateStr(date))
                    val d = GregorianCalendar()
                    d.setTimeInMillis(date)
                    mScrollLoader.setStartDate(d)
                    startOpDate = d
                    getOperationsList()
                }
                c.close()
            }
        }
    }

    private fun refreshSelection() {
        val adapter = mOpListAdapter
        if (adapter != null) {
            if (mLastSelectionId == -1L) {
                val today = Tools.createClearedCalendar()
                val pos = adapter.findLastOpBeforeDatePos(today)
                selectOpAndAdjustOffset(pos, true)
            } else {
                selectOpAndAdjustOffset(findOpPosition(mLastSelectionId), true)
            }
        } else {
            throw IllegalStateException()
        }
    }

    private fun findOpPosition(id: Long): Int {
        val adapter = mOpListAdapter
        if (adapter != null) {
            val count = adapter.getItemCount() - 1
            for (i in 0..count) {
                if (adapter.operationAt(i).mRowId == id) {
                    return i
                }
            }
        }
        return -1
    }

    protected fun afterDelUpdateSelection() {
        val adapter = mOpListAdapter
        if (adapter != null)
            adapter.selectedPosition = -1
        mLastSelectionId = -1L
        mLastSelectionPos = -1
        needRefreshSelection = true
        getOperationsList()
        mActivity.updateAccountList()
    }

    public fun onOperationEditorResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            this.mLastSelectionId = data.getLongExtra("opId", this.mLastSelectionId)
            val date = data.getLongExtra("opDate", 0)
            if (date > 0) {
                val opDate = GregorianCalendar()
                opDate.setTimeInMillis(date)
                opDate.set(Calendar.DAY_OF_MONTH, 1)
                val today = Tools.createClearedCalendar()
                if (today.get(Calendar.MONTH) > opDate.get(Calendar.MONTH)) {
                    this.startOpDate = opDate
                }
                getOperationsList()
            }
        }
    }

    public class DeleteOpConfirmationDialog : DialogFragment() {
        private var accountId: Long = 0
        private var operationId: Long = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreate(savedInstanceState)
            val args = getArguments()
            this.accountId = args.getLong("accountId")
            this.operationId = args.getLong("opId")
            return Tools.createDeleteConfirmationDialog(getActivity(), object : DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    if (OperationTable.deleteOp(getActivity(), operationId, accountId)) {
                        parentFrag.afterDelUpdateSelection()
                    }

                }
            })
        }

        class object {
            var parentFrag: OperationListFragment by Delegates.notNull()

            public fun newInstance(accountId: Long, opId: Long, parentFrag: OperationListFragment): DeleteOpConfirmationDialog {
                val frag = DeleteOpConfirmationDialog()
                DeleteOpConfirmationDialog.parentFrag = parentFrag
                val args = Bundle()
                args.putLong("accountId", accountId)
                args.putLong("opId", opId)
                frag.setArguments(args)
                return frag
            }
        }
    }

    public class DeleteOccurrenceConfirmationDialog : DialogFragment() {
        val TAG = "DeleteOccurrenceConfirmationDialog"

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreate(savedInstanceState)
            val args = getArguments()
            val accountId = args.getLong("accountId")
            val operationId = args.getLong("opId")
            val schId = args.getLong("schId")
            val date = args.getLong("date")
            val transfertId = args.getLong("transfertId")
            Log.d(TAG, "date of op to del : " + Tools.getDateStr(date))
            val builder = AlertDialog.Builder(getActivity())
            builder.setMessage(R.string.delete_recurring_op).setCancelable(true).setPositiveButton(R.string.del_only_current, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    if (OperationTable.deleteOp(getActivity(), operationId, accountId)) {
                        parentFrag.afterDelUpdateSelection()
                    }
                }
            }).setNeutralButton(R.string.del_all_following, object : DialogInterface.OnClickListener {

                override fun onClick(dialog: DialogInterface, which: Int) {
                    val nbDel = OperationTable.deleteAllFutureOccurrences(getActivity(), accountId, schId, date, transfertId)
                    Log.d(TAG, "nbDel : " + nbDel)
                    if (nbDel > 0) {
                        parentFrag.afterDelUpdateSelection()
                    }
                }
            }).setNegativeButton(R.string.del_all_occurrences, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    if (OperationTable.deleteAllOccurrences(getActivity(), accountId, schId, transfertId) > 0) {
                        parentFrag.afterDelUpdateSelection()
                    }
                }
            })
            return builder.create()
        }

        class object {
            var parentFrag: OperationListFragment by Delegates.notNull()

            public fun newInstance(accountId: Long, opId: Long, schId: Long, date: Long, transfertId: Long, parentFrag: OperationListFragment): DeleteOccurrenceConfirmationDialog {
                DeleteOccurrenceConfirmationDialog.parentFrag = parentFrag
                val frag = DeleteOccurrenceConfirmationDialog()
                val args = Bundle()
                args.putLong("accountId", accountId)
                args.putLong("opId", opId)
                args.putLong("schId", schId)
                args.putLong("date", date)
                args.putLong("transfertId", transfertId)
                frag.setArguments(args)
                return frag
            }
        }
    }

    public class DeleteAccountConfirmationDialog : DialogFragment() {
        private var accountId: Long = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val args = getArguments()
            this.accountId = args.getLong("accountId")
            return Tools.createDeleteConfirmationDialog(getActivity(), object : DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    if (AccountTable.deleteAccount(getActivity(), accountId)) {
                        MainActivity.refreshAccountList(getActivity())
                    } else {
                        (getActivity() as MainActivity).getAccountManager().setCurrentAccountId(null)
                    }
                }
            }, R.string.account_delete_confirmation)
        }

        class object {
            platformStatic public fun newInstance(accountId: Long): DeleteAccountConfirmationDialog {
                val frag = DeleteAccountConfirmationDialog()
                val args = Bundle()
                args.putLong("accountId", accountId)
                frag.setArguments(args)
                return frag
            }
        }
    }
}