package fr.geobert.radis.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.IntentCompat
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
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.adapter.OperationsAdapter
import fr.geobert.radis.ui.editor.OperationEditor
import hirondelle.date4j.DateTime
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class OperationListFragment : BaseFragment(), UpdateDisplayInterface, LoaderManager.LoaderCallbacks<Cursor>, IOperationList {
    private var mOldChildCount: Int = -1

    //    private var checkingDashboard: CheckingOpDashboard? = null
    private var freshLoader: Boolean = false
    private var mListLayout: LinearLayoutManager by Delegates.notNull()
    private val TAG = "OperationListFragment"
    private val GET_OPS = 300
    private var mOperationsLoader: CursorLoader? = null
    private var mQuickAddController: QuickAddController? = null
    private var mOpListAdapter: OperationsAdapter? = null
    //private var operation_list: RecyclerView by Delegates.notNull()
    private var earliestOpDate: DateTime? = null // start date of ops to get
    private var mScrollLoader: OnOperationScrollLoader by Delegates.notNull()
    private var mLastSelectionId = -1L
    private var mLastSelectionPos = -1
    private var needRefreshSelection = false
    private var container: LinearLayout by Delegates.notNull()

    // TODO kotlinx.android
    private var operation_list: RecyclerView by Delegates.notNull()
    private var empty_textview: View by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        super<BaseFragment>.onCreateView(inflater, container, savedInstanceState)
        val c = inflater.inflate(R.layout.operation_list, container, false) as LinearLayout
        this.container = c

        // TODO kotlinx
        operation_list = c.findViewById (R.id.operation_list) as RecyclerView
        empty_textview = c.findViewById (R.id.empty_textview)

        setMenu(R.menu.operations_list_menu)
        setIcon(R.drawable.radis_no_disc_48)
        mActivity.mAccountSpinner.setSelection(mActivity.mAccountManager.getCurrentAccountPosition(mActivity))
        // TODO : useless in actual form
        //        checkingDashboard = CheckingOpDashboard(getActivity() as MainActivity, l)
        //        checkingDashboard?.onResume()
        initOperationList()
        initQuickAdd()
        processAccountChanged(mActivity.getCurrentAccountId())
        return c
    }

    override fun onResume() {
        super<BaseFragment>.onResume()
        mOldChildCount = -1
        val accMan = mActivity.mAccountManager
        val curDefaultAccId = accMan.mCurDefaultAccount
        if (curDefaultAccId != null && curDefaultAccId != accMan.getDefaultAccountId(mActivity)) {
            accMan.mCurDefaultAccount = null
            accMan.setCurrentAccountId(null, mActivity)
            mLastSelectionId = -1L
        }

        val q = mQuickAddController
        if (q != null) {
            q.setAutoNegate(true)
            q.clearFocus()
            refreshQuickAdd()
        }
        //        checkingDashboard?.onResume()
    }

    override fun onPause() {
        super<BaseFragment>.onPause()
        //        checkingDashboard?.onPause()
    }

    private fun initQuickAdd() {
        val c = container
        val q = QuickAddController(mActivity, c)
        q.initViewBehavior()
        q.setAutoNegate(true)
        q.clearFocus()
        mQuickAddController = q;
        setQuickAddVisibility()
    }

    fun setQuickAddVisibility() {
        val q = mQuickAddController
        if (q != null) {
            val config = mAccountManager.mCurAccountConfig
            val hideQuickAdd = if (config?.overrideHideQuickAdd ?: false) config?.hideQuickAdd ?: false else
                DBPrefsManager.getInstance(mActivity).getBoolean(ConfigFragment.KEY_HIDE_OPS_QUICK_ADD, false)
            q.setVisibility(if (hideQuickAdd) View.GONE else View.VISIBLE)
        }
    }

    fun refreshQuickAdd() {
        setQuickAddVisibility()
        mQuickAddController?.setupListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseFragment>.onSaveInstanceState(outState)
        mQuickAddController?.onSaveInstanceState(outState)
        outState.putLong("mAccountId", mActivity.getCurrentAccountId())
    }

    override fun onDestroyView() {
        super<BaseFragment>.onDestroyView()
        Log.d("OperationListFragment", "onDestroyView")
        getLoaderManager().destroyLoader(GET_OPS)
    }

    private fun initOperationList() {
        mListLayout = LinearLayoutManager(mActivity)
        operation_list.setLayoutManager(mListLayout)
        operation_list.setItemAnimator(DefaultItemAnimator())
        operation_list.setHasFixedSize(true)
        mScrollLoader = OnOperationScrollLoader(this)
        operation_list.setOnScrollListener(mScrollLoader)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        initQuickAdd()
        mQuickAddController?.onRestoreInstanceState(savedInstanceState)
    }

    fun processAccountChanged(itemId: Long) {
        mAccountManager.setCurrentAccountId(itemId, mActivity)
        earliestOpDate = null
        mOldChildCount = -1
        //        mScrollLoader.setStartDate(startDate)
        val q = mQuickAddController
        if (q == null) {
            initQuickAdd()
        }
        getMoreOperations(initialStartDate())
    }

    fun initialStartDate() = DateTime.today(TIME_ZONE).minusMonth(1)

    override fun onAccountChanged(itemId: Long): Boolean {
        Log.d("OperationListFragment", "onAccountChanged old account id : ${mAccountManager.getCurrentAccountId(getActivity())} / itemId : $itemId")
        if (mAccountManager.getCurrentAccountId(getActivity()) != itemId) {
            getLoaderManager().destroyLoader(GET_OPS)
            processAccountChanged(itemId)
        }
        return false
    }

    fun setEmptyViewVisibility(visible: Boolean) {
        if (visible) {
            operation_list.setVisibility(View.GONE)
            Log.d(TAG, "mEmptyView parent : ${empty_textview.getParent()}")
            empty_textview.setVisibility(View.VISIBLE)
        } else {
            operation_list.setVisibility(View.VISIBLE)
            empty_textview.setVisibility(View.GONE)
        }
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.getId()) {
            GET_OPS -> {
                var refresh = false
                Log.d("OperationListFragment", "onLoadFinished $mOpListAdapter")
                val adapter = mOpListAdapter
                if (adapter == null) {
                    val a = OperationsAdapter(mActivity, this, cursor)
                    mOpListAdapter = a
                    refresh = true
                    operation_list.setAdapter(mOpListAdapter)
                } else {
                    adapter.increaseCache(cursor)
                    Log.d("OperationListFragment", "onLoadFinished fresh $freshLoader")
                    if (freshLoader) // TODO why this is needed?
                        operation_list.setAdapter(adapter)
                }
                freshLoader = false
                val itemCount = mOpListAdapter?.getItemCount()
                Log.d("OperationListFragment", "onLoadFinished item count : ${itemCount}")
                setEmptyViewVisibility(itemCount == 0)
                if (refresh || needRefreshSelection) {
                    needRefreshSelection = false
                    refreshSelection()
                }
                operation_list.post {
                    val curChildCount = mListLayout.getChildCount()
                    //Log.d(TAG, "onLoadFinished, old child count = $mOldChildCount, cur child count = $curChildCount, last visible = ${mListLayout.findLastCompletelyVisibleItemPosition()}")
                    if (curChildCount > 0 && mOldChildCount != curChildCount &&
                            curChildCount - 1 == mListLayout.findLastCompletelyVisibleItemPosition()) {
                        mOldChildCount = mListLayout.getChildCount()
                        mScrollLoader.onScrolled(operation_list, 0, 0)
                    }
                }

            }
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor>? =
            when (i) {
                GET_OPS -> {
                    mOperationsLoader = OperationTable.getOpsWithStartDateLoader(mActivity, bundle.getLong("date"),
                            mActivity.getCurrentAccountId())
                    mOperationsLoader
                }
                else -> null
            }

    /**
     * get the operations of current account, should be called after getAccountList
     */
    private fun getOperationsList() {
        Log.d("getOperationsList", "earliestDate : $earliestOpDate")
        val b = Bundle()
        b.putLong("date", earliestOpDate!!.getMilliseconds(TIME_ZONE))
        if (mOperationsLoader == null) {
            freshLoader = true
            getLoaderManager().initLoader<Cursor>(GET_OPS, b, this)
        } else {
            getLoaderManager().restartLoader<Cursor>(GET_OPS, b, this)
        }
    }

    override fun getMoreOperations(startDate: DateTime?) {
        if (isAdded()) {
            Log.d("getMoreOperations", "startDate : " + startDate?.formatDateLong())
            if (startDate != null) {
                // date specified, use it
                earliestOpDate = startDate
                getOperationsList()
            } else {
                // no op found with cur month and month - 1, try if there is one
                Log.d("getMoreOperations", "earliestOpDate : " + earliestOpDate?.formatDateLong())
                val start = earliestOpDate
                val c = if (null == start) {
                    OperationTable.fetchLastOp(mActivity, mActivity.getCurrentAccountId())
                } else {
                    OperationTable.fetchLastOpSince(mActivity, mActivity.getCurrentAccountId(),
                            start.getMilliseconds(TIME_ZONE))
                }
                if (c != null) {
                    Log.d("getMoreOperations", "cursor count : " + c.getCount())
                    if (c.moveToFirst()) {
                        val date = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_DATE))
                        Log.d(TAG, "last chance date : " + Tools.getDateStr(date))
                        val d = DateTime.forInstant(date, TIME_ZONE).getStartOfMonth()
                        Log.d(TAG, "last chance date verif : " + d.format("DD/MM/YYYY"))
                        //mScrollLoader.setStartDate(d)
                        earliestOpDate = d
                        getOperationsList()
                    } else if (start == null) {
                        earliestOpDate = DateTime.today(TIME_ZONE).getStartOfMonth()
                        getOperationsList()
                    } else if (mOpListAdapter?.getItemCount() == 0) {
                        setEmptyViewVisibility(true)
                    }
                    c.close()
                }
            }
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
        Log.d("OperationListFragment", "onLoaderReset")
        if (!mActivity.isFinishing()) {
            when (cursorLoader.getId()) {
                GET_OPS -> {
                    Log.d("OperationListFragment", "onLoaderReset doing reset")
                    needRefreshSelection = true
                    mLastSelectionId = -1L
                    mLastSelectionPos = -1
                    mOpListAdapter?.reset()
                    mOperationsLoader = null
                }
                else -> {
                }
            }
        }
    }

    override fun updateDisplay(intent: Intent?) {
        getMoreOperations(initialStartDate())
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
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

    private fun adjustScroll(position: Int) {
        val half = mListLayout.getChildCount() / 2
        Log.d("adjustScroll", "half: $half, pos: $position, first: ${mListLayout.findFirstCompletelyVisibleItemPosition()}, last: ${mListLayout.findLastCompletelyVisibleItemPosition()}")
        if (mListLayout.findFirstCompletelyVisibleItemPosition() >= position) {
            if (mListLayout.findFirstCompletelyVisibleItemPosition() == position) {
                if (position - half > 0) {
                    // scroll in order to see fully expanded op row
                    operation_list.smoothScrollToPosition(position - half)
                } else {
                    mScrollLoader.onScrolled(operation_list, 0, 0);
                }
            } else {
                if (position - 1 > 0) {
                    // scroll in order to see fully expanded op row
                    operation_list.smoothScrollToPosition(position - 1)
                } else {
                    mScrollLoader.onScrolled(operation_list, 0, 0);
                }
            }
        } else if (mListLayout.findLastCompletelyVisibleItemPosition() <= position ) {
            if (mListLayout.findLastCompletelyVisibleItemPosition() == position) {
                if (position + 1 > mListLayout.getChildCount()) {
                    operation_list.smoothScrollToPosition(position + 1)
                } else {
                    mScrollLoader.onScrolled(operation_list, 0, 0);
                }
            } else {
                operation_list.smoothScrollToPosition(position + half) // scroll in order to see fully expanded op row
            }
        }
    }

    private fun selectOpAndAdjustOffset(position: Int) {
        val adapter = mOpListAdapter
        if (adapter != null && adapter.getItemCount() > 0) {
            Log.d("selectOpAndAdjustOffset", "pos: $position, lastPos: $mLastSelectionPos")
            if (position != mLastSelectionPos) {
                mLastSelectionPos = position
                mLastSelectionId = adapter.operationAt(position).mRowId

                operation_list.post(object : Runnable {
                    override fun run() {
                        adjustScroll(position)
                    }
                })
                //, 400)

                adapter.selectedPosition = position
            }
        }
    }

    private fun refreshSelection() {
        val adapter = mOpListAdapter
        if (adapter != null) {
            if (mLastSelectionId == -1L) {
                val today = Tools.createClearedCalendar()
                val pos = adapter.findLastOpBeforeDatePos(today)
                selectOpAndAdjustOffset(pos)
            } else {
                selectOpAndAdjustOffset(findOpPosition(mLastSelectionId))
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
        getMoreOperations(initialStartDate())// getOperationsList() ?
        mActivity.updateAccountList()
    }

    override public fun onOperationEditorResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            this.mLastSelectionId = data.getLongExtra("opId", this.mLastSelectionId)
            val date = data.getLongExtra("opDate", 0)
            if (date > 0) {
                //                val opDate = DateTime.forInstant(date, TIME_ZONE).getStartOfMonth()
                //                val today = DateTime.today(TIME_ZONE)
                //                if (today.getMonth() > opDate.getMonth()) {
                //                    getMoreOperations(opDate)
                //                } else {
                //                    getMoreOperations(initialStartDate())
                //                }
                getMoreOperations(initialStartDate())
            }
        }
    }

    override fun getListLayoutManager(): LinearLayoutManager = mListLayout

    override fun getRecyclerView(): RecyclerView = operation_list

    companion object {
        platformStatic public fun restart(ctx: Context) {
            DbContentProvider.reinit(ctx)
            val intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK)
            ctx.startActivity(intent)
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

        companion object {
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
            builder.setMessage(R.string.delete_recurring_op).setCancelable(true).
                    setPositiveButton(R.string.del_only_current, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            if (OperationTable.deleteOp(getActivity(), operationId, accountId)) {
                                parentFrag.afterDelUpdateSelection()
                            }
                        }
                    }).
                    setNeutralButton(R.string.del_all_following, object : DialogInterface.OnClickListener {

                        override fun onClick(dialog: DialogInterface, which: Int) {
                            val nbDel = OperationTable.deleteAllFutureOccurrences(getActivity(), accountId, schId, date, transfertId)
                            Log.d(TAG, "nbDel : " + nbDel)
                            if (nbDel > 0) {
                                parentFrag.afterDelUpdateSelection()
                            }
                        }
                    }).
                    setNegativeButton(R.string.del_all_occurrences, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            if (OperationTable.deleteAllOccurrences(getActivity(), accountId, schId, transfertId) > 0) {
                                parentFrag.afterDelUpdateSelection()
                            }
                        }
                    })
            return builder.create()
        }

        companion object {
            var parentFrag: OperationListFragment by Delegates.notNull()

            public fun newInstance(accountId: Long, opId: Long, schId: Long, date: Long, transfertId: Long,
                                   parentFrag: OperationListFragment): DeleteOccurrenceConfirmationDialog {
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
                        val act = getActivity() as MainActivity
                        val accMan = act.mAccountManager
                        // attempt to fix Fatal Exception: java.lang.IllegalStateException
                        // couldn't move cursor to position 4
                        if (accountId == accMan.getCurrentAccountId(act)) {
                            accMan.setCurrentAccountId(null, getActivity())
                        }
                        if (accountId == accMan.getDefaultAccountId(act)) {
                            accMan.mCurDefaultAccount = null
                            accMan.setCurrentAccountId(null, getActivity())
                            DBPrefsManager.getInstance(act).put(ConfigFragment.KEY_DEFAULT_ACCOUNT, null)
                        }
                        MainActivity.refreshAccountList(getActivity())
                    } else {
                        (getActivity() as MainActivity).mAccountManager.setCurrentAccountId(null, getActivity())
                    }
                }
            }, R.string.account_delete_confirmation)
        }

        companion object {
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
