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
import fr.geobert.radis.ui.adapter.OperationsAdapter
import android.view.ViewStub
import fr.geobert.radis.tools.formatDate

public class OperationListFragment : BaseFragment(), UpdateDisplayInterface, LoaderManager.LoaderCallbacks<Cursor>, IOperationList {
    private var mOldChildCount: Int = -1

    private var checkingDashboard: CheckingOpDashboard? = null
    private var freshLoader: Boolean = false
    private var mListLayout: LinearLayoutManager by Delegates.notNull()
    private val TAG = "OperationListFragment"
    private val GET_OPS = 300
    private var mOperationsLoader: CursorLoader? = null
    private var mQuickAddController: QuickAddController? = null
    private var mOpListAdapter: OperationsAdapter? = null
    private var mListView: RecyclerView by Delegates.notNull()
    //    private val mStubView: ViewStub by Delegates.lazy { this.container.findViewById(android.R.id.empty) as ViewStub }
    private val mEmptyView: View by Delegates.lazy { this.container.findViewById(R.id.empty_textview) }
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
        processAccountChanged(mActivity.getCurrentAccountId()!!)
        return this.container
    }

    override fun onResume() {
        super<BaseFragment>.onResume()
        mOldChildCount = -1
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
        checkingDashboard?.onResume()
    }

    override fun onPause() {
        super<BaseFragment>.onPause()
        checkingDashboard?.onPause()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseFragment>.onSaveInstanceState(outState)
        mQuickAddController?.onSaveInstanceState(outState)
        if (mActivity.getCurrentAccountId() != null) {
            outState.putLong("mAccountId", mActivity.getCurrentAccountId()!!)
        }
    }

    override fun onDestroyView() {
        super<BaseFragment>.onDestroyView()
        Log.d("OperationListFragment", "onDestroyView")
        getLoaderManager().destroyLoader(GET_OPS)
    }

    private fun initOperationList() {
        mListView = container.findViewById(android.R.id.list) as RecyclerView
        mListLayout = LinearLayoutManager(mActivity)
        if (mListView.getLayoutManager() == null) {
            mListView.setLayoutManager(mListLayout)
        }
        mListView.setItemAnimator(DefaultItemAnimator())
        mListView.setHasFixedSize(true)
        mScrollLoader = OnOperationScrollLoader(this)
        mListView.setOnScrollListener(mScrollLoader)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        initQuickAdd()
        mQuickAddController?.onRestoreInstanceState(savedInstanceState)
    }

    fun processAccountChanged(itemId: Long): Boolean {
        mAccountManager.setCurrentAccountId(itemId)
        val startDate = Tools.createClearedCalendar()
        startDate.set(Calendar.DAY_OF_MONTH, startDate.getActualMinimum(Calendar.DAY_OF_MONTH))
        //startOpDate = startDate
        //        mScrollLoader.setStartDate(startDate)
        val q = mQuickAddController
        if (q != null) {
            q.setAccount(itemId)
            getMoreOperations(startDate)// getOperationsList()
            return true
        } else {
            initQuickAdd()
            getMoreOperations(startDate)// getOperationsList()
            return false
        }
    }

    override fun onAccountChanged(itemId: Long): Boolean {
        if (mAccountManager == null) {
            return false
        }
        Log.d("OperationListFragment", "onAccountChanged old account id : ${mAccountManager.getCurrentAccountId(getActivity())} / itemId : $itemId")
        if (mAccountManager.getCurrentAccountId(getActivity()) != itemId) {
            getLoaderManager().destroyLoader(GET_OPS)
            processAccountChanged(itemId)
        }
        return false
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor>? =
            when (i) {
                GET_OPS -> {
                    if (startOpDate == null) {
                        val startDate = Tools.createClearedCalendar()
                        startDate.set(Calendar.DAY_OF_MONTH, startDate.getActualMinimum(Calendar.DAY_OF_MONTH))
                        startOpDate = startDate
                    }
                    mOperationsLoader = OperationTable.getOpsWithStartDateLoader(mActivity, startOpDate,
                            mActivity.getCurrentAccountId())
                    mOperationsLoader
                }
                else -> null
            }

    fun setEmptyViewVisibility(visible: Boolean) {
        if (visible) {
            mListView.setVisibility(View.GONE)
            Log.d(TAG, "mEmptyView parent : ${mEmptyView.getParent()}")
            mEmptyView.setVisibility(View.VISIBLE)
        } else {
            mListView.setVisibility(View.VISIBLE)
            mEmptyView.setVisibility(View.GONE)
        }
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.getId()) {
            GET_OPS -> {
                var refresh = false
                Log.d("OperationListFragment", "onLoadFinished $mOpListAdapter")
                val adapter = mOpListAdapter
                if (adapter == null) {
                    val a = OperationsAdapter(mActivity, this, cursor, checkingDashboard)
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
                val itemCount = mOpListAdapter?.getItemCount()
                Log.d("OperationListFragment", "onLoadFinished item count : ${itemCount}")
                setEmptyViewVisibility(itemCount == 0)
                if (itemCount == 0) {
                    startOpDate = null
                }
                if (refresh || needRefreshSelection) {
                    needRefreshSelection = false
                    refreshSelection()
                } else {
                    mListView.post {
                        val curChildCount = mListLayout.getChildCount()
                        // Log.d(TAG, "onLoadFinished, old child count = $mOldChildCount, cur child count = $curChildCount, last visible = ${mListLayout.findLastCompletelyVisibleItemPosition()}")
                        if (curChildCount > 0 && mOldChildCount != curChildCount &&
                                curChildCount - 1 == mListLayout.findLastCompletelyVisibleItemPosition()) {
                            mOldChildCount = mListLayout.getChildCount()
                            mScrollLoader.onScrolled(mListView, 0, 0)
                        }
                    }
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
     * get the operations of current account, should be called after getAccountList
     */
    private fun getOperationsList() {
        if (mActivity.getCurrentAccountId() != null) {
            if (mOperationsLoader == null) {
                freshLoader = true
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
        getMoreOperations(null)// getOperationsList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.operations_list_menu, menu)
        val l = menu.findItem(R.id.checking_op).getActionView() as LinearLayout
        this.checkingDashboard = CheckingOpDashboard(getActivity() as MainActivity, l)
        checkingDashboard?.onResume()
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

    private fun adjustScroll(position: Int) {
        val half = mListLayout.getChildCount() / 2
        Log.d("adjustScroll", "half: $half, pos: $position, first: ${mListLayout.findFirstCompletelyVisibleItemPosition()}, last: ${mListLayout.findLastCompletelyVisibleItemPosition()}")
        if (mListLayout.findFirstCompletelyVisibleItemPosition() >= position) {
            if (mListLayout.findFirstCompletelyVisibleItemPosition() == position) {
                if (position - half > 0) {
                    // scroll in order to see fully expanded op row
                    mListView.smoothScrollToPosition(position - half)
                } else {
                    mScrollLoader.onScrolled(mListView, 0, 0);
                }
            } else {
                if (position - 1 > 0) {
                    // scroll in order to see fully expanded op row
                    mListView.smoothScrollToPosition(position - 1)
                } else {
                    mScrollLoader.onScrolled(mListView, 0, 0);
                }
            }
        } else if (mListLayout.findLastCompletelyVisibleItemPosition() <= position ) {
            if (mListLayout.findLastCompletelyVisibleItemPosition() == position) {
                if (position + 1 > mListLayout.getChildCount()) {
                    mListView.smoothScrollToPosition(position + 1)
                } else {
                    mScrollLoader.onScrolled(mListView, 0, 0);
                }
            } else {
                mListView.smoothScrollToPosition(position + half) // scroll in order to see fully expanded op row
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

                mListView.post(object : Runnable {
                    override fun run() {
                        adjustScroll(position)
                    }
                })
                //, 400)

                adapter.selectedPosition = position
            }
        }
    }

    override fun getMoreOperations(startDate: GregorianCalendar?) {
        Log.d("getMoreOperations", "startDate : " + startDate?.getTime()?.formatDate())
        if (startDate != null) {
            startOpDate = startDate
            getOperationsList()
        } else {
            // no op found with cur month and month - 1, try if there is one
            val c: Cursor?
            Log.d("getMoreOperations", "startOpDate : " + startOpDate?.getTime()?.formatDate())
            val start = startOpDate
            if (null == start) {
                c = OperationTable.fetchLastOp(mActivity, mActivity.getCurrentAccountId())
            } else {
                c = OperationTable.fetchLastOpSince(mActivity, mActivity.getCurrentAccountId(), start.getTimeInMillis())
            }
            Log.d("getMoreOperations", "cursor count : " + c?.getCount())
            if (c != null) {
                if (c.moveToFirst()) {
                    val date = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_DATE))
                    Log.d(TAG, "last chance date : " + Tools.getDateStr(date))
                    val d = GregorianCalendar()
                    d.setTimeInMillis(date)
                    d.set(Calendar.DAY_OF_MONTH, d.getMinimum(Calendar.DAY_OF_MONTH))
                    //                    mScrollLoader.setStartDate(d)
                    startOpDate = d
                    getOperationsList()
                } else if (mOpListAdapter?.getItemCount() == 0) {
                    setEmptyViewVisibility(true)
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
        getOperationsList() // TODO getMoreOperations(null)// getOperationsList() ?
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
                getOperationsList() // TODO getMoreOperations(null)// getOperationsList() ?
            }
        }
    }

    override fun getListLayoutManager(): LinearLayoutManager = mListLayout

    override fun getRecyclerView(): RecyclerView = mListView

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