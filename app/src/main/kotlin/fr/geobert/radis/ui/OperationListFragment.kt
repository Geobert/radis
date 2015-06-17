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
import fr.geobert.radis.ui.editor.EditorResultDependent
import fr.geobert.radis.ui.editor.OperationEditor
import hirondelle.date4j.DateTime
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class OperationListFragment : BaseFragment(), UpdateDisplayInterface, LoaderManager.LoaderCallbacks<Cursor>,
        IOperationList {
    private var mOldChildCount: Int = -1
    private var freshLoader: Boolean = false
    private var mListLayout: LinearLayoutManager? = null
    private val TAG = "OperationListFragment"
    private val GET_OPS = 300
    private var mOperationsLoader: CursorLoader? = null
    private var mQuickAddController: QuickAddController? = null
    private var mOpListAdapter: OperationsAdapter? = null
    private var earliestOpDate: DateTime? = null // start date of ops to get
    private var mScrollLoader: OnOperationScrollLoader by Delegates.notNull()
    private var mLastSelectionId = -1L
    private var mLastSelectionPos = -1
    private var needRefreshSelection = false

    private var operation_list: RecyclerView by Delegates.notNull()
    private var empty_textview: View by Delegates.notNull()
    private var container: LinearLayout by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        super<BaseFragment>.onCreateView(inflater, container, savedInstanceState)
        this.container = inflater.inflate(R.layout.operation_list, container, false) as LinearLayout
        operation_list = this.container.findViewById(R.id.operation_list) as RecyclerView
        empty_textview = this.container.findViewById(R.id.empty_textview)
        return this.container
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super<BaseFragment>.onActivityCreated(savedInstanceState)
        setupIcon()
        setMenu(R.menu.operations_list_menu)
        initOperationList()
        if (savedInstanceState != null) {
            mQuickAddController?.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun setupIcon() = setIcon(R.drawable.radis_no_disc_48)

    override fun onResume() {
        super<BaseFragment>.onResume()
        Log.d(TAG, "onResume")

        initOperationList()

        mOldChildCount = -1
        val accMan = mActivity.mAccountManager

        // fix crash if radis is killed by android and back
        mActivity.mAccountSpinner.setSelection(mActivity.mAccountManager.getCurrentAccountPosition(mActivity))
        initQuickAdd()
        processAccountChanged(mActivity.getCurrentAccountId())

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
        if (mQuickAddController == null) {
            val c = container
            val q = QuickAddController(mActivity, c)
            q.initViewBehavior()
            q.setAutoNegate(true)
            q.clearFocus()
            mQuickAddController = q;
            setQuickAddVisibility()
        }
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
        Log.d(TAG, "initOperationList, mListLayout:$mListLayout, opListLayMan:${operation_list.getLayoutManager()}, oplist:${operation_list}")
        //        if (mListLayout == null) {
        mListLayout = LinearLayoutManager(mActivity)
        operation_list.setLayoutManager(mListLayout)
        operation_list.setItemAnimator(DefaultItemAnimator())
        operation_list.setHasFixedSize(true)
        mScrollLoader = OnOperationScrollLoader(this)
        operation_list.clearOnScrollListeners()
        operation_list.addOnScrollListener(mScrollLoader)
        //        }
    }

    //    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    //        initQuickAdd()
    //        mQuickAddController?.onRestoreInstanceState(savedInstanceState)
    //    }

    fun processAccountChanged(itemId: Long) {
        Log.d(TAG, "processAccountChanged to $itemId")
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

    fun initialStartDate() = DateTime.today(TIME_ZONE).minusMonth(1).getStartOfMonth()

    override fun onAccountChanged(itemId: Long): Boolean {
        Log.d("OperationListFragment", "onAccountChanged old account id : ${mAccountManager.getCurrentAccountId(getActivity())} / itemId : $itemId")
        if (mAccountManager.getCurrentAccountId(getActivity()) != itemId) {
            getLoaderManager().destroyLoader(GET_OPS)
            processAccountChanged(itemId)
        }
        return false
    }

    fun setupEmptyViewVisibility(visible: Boolean) {
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
                val adapter = mOpListAdapter
                Log.d("OperationListFragment", "onLoadFinished feshLoader:$freshLoader, adapter:$adapter, layout:$mListLayout")
                if (adapter == null) {
                    val a = OperationsAdapter(mActivity, this, cursor)
                    mOpListAdapter = a
                    refresh = true
                    operation_list.setAdapter(mOpListAdapter)
                } else {
                    adapter.increaseCache(cursor)
                    if (freshLoader) {
                        // TODO why this is needed?
                        operation_list.setAdapter(adapter)
                        operation_list.setLayoutManager(mListLayout)
                        adapter.notifyDataSetChanged()
                    }
                }
                freshLoader = false
                val itemCount = mOpListAdapter?.getItemCount()
                Log.d("OperationListFragment", "onLoadFinished item count : ${itemCount} / cursor.count:${cursor.getCount()}")
                setupEmptyViewVisibility(itemCount == 0)
                if (refresh || needRefreshSelection) {
                    needRefreshSelection = false
                    refreshSelection()
                }
                operation_list.post {
                    val curChildCount = mListLayout?.getChildCount() ?: 0
                    val lastVisibleItemPos = mListLayout?.findLastCompletelyVisibleItemPosition()
                    Log.d(TAG, "onLoadFinished, old child count = $mOldChildCount, cur child count = $curChildCount, last visible = $lastVisibleItemPos")
                    if (itemCount == 0 || (mOldChildCount != curChildCount && curChildCount - 1 == lastVisibleItemPos)) {
                        mOldChildCount = curChildCount
                        needRefreshSelection = true
                        mScrollLoader.onScrolled(operation_list, 0, 0)
                    }
                }

            }
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor>? =
            when (i) {
                GET_OPS -> {
                    val latestDate = bundle.getLong("latestDate", 0)
                    if (latestDate == 0L) {
                        mOperationsLoader = OperationTable.getOpsWithStartDateLoader(mActivity, bundle.getLong("earliestDate"),
                                mActivity.getCurrentAccountId())
                    } else {
                        mOperationsLoader = OperationTable.getOpsBetweenDateLoader(mActivity, bundle.getLong("earliestDate"), latestDate,
                                mActivity.getCurrentAccountId())
                    }
                    mOperationsLoader
                }
                else -> null
            }

    /**
     * get the operations of current account, should be called after getAccountList
     */
    private fun getOperationsList() {
        //        Log.d("getOperationsList", "earliestDate : $earliestOpDate / mOperationsLoader : $mOperationsLoader ")
        val b = Bundle()
        b.putLong("earliestDate", earliestOpDate!!.getMilliseconds(TIME_ZONE))
        val a = mOpListAdapter
        if (a != null && a.getItemCount() > 0) {
            val o = a.operationAt(a.getItemCount() - 1)
            b.putLong("latestDate", o.getDate())
        }
        if (mOperationsLoader == null) {
            freshLoader = true
            getLoaderManager().initLoader<Cursor>(GET_OPS, b, this)
        } else {
            getLoaderManager().restartLoader<Cursor>(GET_OPS, b, this)
        }
    }

    override fun getMoreOperations(startDate: DateTime?) {
        if (isAdded()) {
            Log.d(TAG, "startDate : " + startDate?.formatDateLong())
            if (startDate != null) {
                // date specified, use it
                earliestOpDate = startDate
                getOperationsList()
            } else {
                // no op found with cur month and month - 1, try if there is one
                Log.d(TAG, "earliestOpDate : " + earliestOpDate?.formatDateLong())
                val start = earliestOpDate
                val c = if (null == start) {
                    OperationTable.fetchLastOp(mActivity, mActivity.getCurrentAccountId())
                } else {
                    OperationTable.fetchLastOpSince(mActivity, mActivity.getCurrentAccountId(),
                            start.getMilliseconds(TIME_ZONE))
                }
                if (c != null) {
                    Log.d(TAG, "cursor count : ${c.getCount()} / mOpListAdapter?.getItemCount():${mOpListAdapter?.getItemCount()}")
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
                        setupEmptyViewVisibility(true)
                    } else {
                        getOperationsList()
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
        val op: Operation? = intent?.getParcelableExtra("operation")
        val a = mOpListAdapter
        if (op == null || a == null) {
            getMoreOperations(initialStartDate())
        } else {
            val last = mLastSelectionPos
            val pos = a.addOp(op)
            setupEmptyViewVisibility(a.getItemCount() == 0)
            Log.d(TAG, "updateDisplay pos:$pos, last:$last, count:${a.getItemCount()}")
            mLastSelectionPos = -1
            mLastSelectionId = op.mRowId
            a.notifyItemChanged(last + if (pos <= last) 1 else 0)
            if (pos > 0) a.notifyItemChanged(pos - 1)
            if (pos < a.getItemCount() - 1) a.notifyItemChanged(pos + 1)
            refreshSelection()
            adjustScroll(pos)
        }
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
        val l = mListLayout
        if (l != null) {
            val half = l.getChildCount() / 2
            Log.d("adjustScroll", "half: $half, pos: $position, first: ${l.findFirstCompletelyVisibleItemPosition()}, last: ${l.findLastCompletelyVisibleItemPosition()}")
            if (l.findFirstCompletelyVisibleItemPosition() >= position) {
                if (l.findFirstCompletelyVisibleItemPosition() == position) {
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
            } else if (l.findLastCompletelyVisibleItemPosition() <= position ) {
                if (l.findLastCompletelyVisibleItemPosition() == position) {
                    if (position + 1 > l.getChildCount()) {
                        operation_list.smoothScrollToPosition(position + 1)
                    } else {
                        mScrollLoader.onScrolled(operation_list, 0, 0);
                    }
                } else {
                    operation_list.smoothScrollToPosition(position + half) // scroll in order to see fully expanded op row
                }
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
            Log.d(TAG, "refreshSelection, mLastSelectionId: $mLastSelectionId")
            if (mLastSelectionId == -1L) {
                val today = Tools.createClearedCalendar()
                val pos = adapter.findLastOpBeforeDatePos(today)
                Log.d(TAG, "refreshSelection, pos: $pos")
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

    protected fun afterDelUpdateSelection(opId: Long) {
        mLastSelectionId = -1L
        mLastSelectionPos = -1
        needRefreshSelection = true
        val adapter = mOpListAdapter
        Log.d(TAG, "afterDelUpdateSelection, adapter:$adapter, opId:$opId")
        if (adapter != null) {
            if (opId > 0) {
                //                mAccountManager.setCurrentAccountSum()
                adapter.selectedPosition = -1
                adapter.delOp(opId)
                setupEmptyViewVisibility(adapter.getItemCount() == 0)
                operation_list.post {
                    refreshSelection()
                }
            } else {
                adapter.reset()
                getMoreOperations(initialStartDate())
            }
        }
        mActivity.updateAccountList()
    }

    override public fun onOperationEditorResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val op: Operation = data.getParcelableExtra("operation")
            mLastSelectionId = op.mRowId
            val adap = mOpListAdapter
            if (adap == null) {
                mAccountManager.refreshCurrentAccount()
                getMoreOperations(initialStartDate())
            } else {
                when (requestCode) {
                    OperationEditor.OPERATION_CREATOR -> {
                        adap.addOp(op)
                        mAccountManager.refreshCurrentAccount()
                        refreshSelection()
                        setupEmptyViewVisibility(adap.getItemCount() == 0)
                    }
                    OperationEditor.OPERATION_EDITOR -> {
                        adap.updateOp(op)
                        mAccountManager.refreshCurrentAccount()
                        refreshSelection()
                    }
                }
            }

        }
    }

    override fun getListLayoutManager(): LinearLayoutManager = mListLayout!!

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
            return Tools.createDeleteConfirmationDialog(getActivity(), { d, i ->
                if (OperationTable.deleteOp(getActivity(), operationId, accountId)) {
                    parentFrag.afterDelUpdateSelection(operationId)
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
                                parentFrag.afterDelUpdateSelection(operationId)
                            }
                        }
                    }).
                    setNeutralButton(R.string.del_all_following, object : DialogInterface.OnClickListener {

                        override fun onClick(dialog: DialogInterface, which: Int) {
                            val nbDel = OperationTable.deleteAllFutureOccurrences(getActivity(), accountId, schId, date, transfertId)
                            Log.d(TAG, "nbDel : " + nbDel)
                            if (nbDel > 0) {
                                parentFrag.afterDelUpdateSelection(-1)
                            }
                        }
                    }).
                    setNegativeButton(R.string.del_all_occurrences, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            if (OperationTable.deleteAllOccurrences(getActivity(), accountId, schId, transfertId) > 0) {
                                parentFrag.afterDelUpdateSelection(-1)
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
            val accountName = args.getString("accountName")
            return Tools.createDeleteConfirmationDialog(getActivity(),
                    getActivity().getString(R.string.account_delete_confirmation).format(accountName),
                    getString(R.string.delete_account_title).format(accountName),
                    { d, i ->
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
                    })
        }

        companion object {
            platformStatic public fun newInstance(accountId: Long, accountName: String): DeleteAccountConfirmationDialog {
                val frag = DeleteAccountConfirmationDialog()
                val args = Bundle()
                args.putLong("accountId", accountId)
                args.putString("accountName", accountName)
                frag.setArguments(args)
                return frag
            }
        }
    }
}
