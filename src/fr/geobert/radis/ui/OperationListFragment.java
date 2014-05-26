package fr.geobert.radis.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import fr.geobert.radis.BaseFragment;
import fr.geobert.radis.MainActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;
import fr.geobert.radis.ui.editor.OperationEditor;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class OperationListFragment extends BaseFragment implements
        UpdateDisplayInterface, LoaderCallbacks<Cursor>, IOperationList {
    private static final String TAG = "OperationListFragment";
    private static final int GET_OPS = 300;
    private CursorLoader mOperationsLoader;
    private QuickAddController mQuickAddController = null;
    private OperationsCursorAdapter mOpListCursorAdapter;
    private ListView mListView;
    private GregorianCalendar startOpDate; // start date of ops to get
    private OnOperationScrollLoader mScrollLoader;
    private long mProjectionDate;
    private long mLastSelectionId = -1;
    private int mLastSelectionPos = -1;
    private boolean needRefreshSelection = false;
    private LinearLayout container;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.container = (LinearLayout) inflater.inflate(R.layout.operation_list, container, false);

        setHasOptionsMenu(true);

        ActionBar supportActionBar = mActivity.getSupportActionBar();
        supportActionBar.setIcon(R.drawable.radis_no_disc_48);

        if (mActivity.getCurrentAccountId() != null) {
            supportActionBar.setSelectedNavigationItem(
                    mActivity.getAccountManager().getCurrentAccountPosition(mActivity));
        }

        initOperationList();
        initQuickAdd();
        onAccountChanged(mActivity.getCurrentAccountId());
        return this.container;
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountManager accMan = mActivity.getAccountManager();
        Long curDefaultAccId = accMan.mCurDefaultAccount;
        if (curDefaultAccId != null && curDefaultAccId != accMan.getDefaultAccountId(mActivity)) {
            accMan.mCurDefaultAccount = null;
            accMan.setCurrentAccountId(null);
            mLastSelectionId = -1;
        }
//        onFetchAllAccountCbk();
        if (mQuickAddController != null) {
            mQuickAddController.setAutoNegate(true);
            mQuickAddController.clearFocus();
            setQuickAddVisibility();
        }
    }

    private void initQuickAdd() {
        mQuickAddController = new QuickAddController(mActivity, container, mActivity);
        mQuickAddController.setAccount(mActivity.getCurrentAccountId());
        mQuickAddController.initViewBehavior();
        mQuickAddController.setAutoNegate(true);
        mQuickAddController.clearFocus();
        setQuickAddVisibility();
    }

    private void setQuickAddVisibility() {
        if (mQuickAddController != null) {
            boolean hideQuickAdd =
                    DBPrefsManager.getInstance(mActivity).getBoolean(RadisConfiguration.KEY_HIDE_OPS_QUICK_ADD);
            int visibility = View.VISIBLE;
            if (hideQuickAdd) {
                visibility = View.GONE;
            }
            mQuickAddController.setVisibility(visibility);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mQuickAddController != null) {
            mQuickAddController.onSaveInstanceState(outState);
        }
        if (mActivity.getCurrentAccountId() != null) {
            outState.putLong("mAccountId", mActivity.getCurrentAccountId());
        }
    }

    private void initOperationList() {
        mListView = (ListView) container.findViewById(android.R.id.list);
        mScrollLoader = new OnOperationScrollLoader(this);
        mListView.setEmptyView(container.findViewById(android.R.id.empty));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectOpAndAdjustOffset(i, false);
            }
        });
        mListView.setOnScrollListener(mScrollLoader);
        mListView.setCacheColorHint(getResources().getColor(android.R.color.transparent));
        mListView.setSelector(android.R.color.transparent);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        initQuickAdd();
        mQuickAddController.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onAccountChanged(long itemId) {
        if (mAccountManager == null) {
            return false;
        }
        mProjectionDate = mAccountManager.setCurrentAccountId(itemId);
        startOpDate = Tools.createClearedCalendar();
        startOpDate.set(Calendar.DAY_OF_MONTH, startOpDate.getActualMinimum(Calendar.DAY_OF_MONTH));
        if (null != mScrollLoader) {
            mScrollLoader.setStartDate(startOpDate);
        }
        if (mOpListCursorAdapter != null) {
            ((OperationRowViewBinder) mOpListCursorAdapter.getViewBinder()).setCurrentAccountId(itemId);
        }
        if (mQuickAddController != null) {
            mQuickAddController.setAccount(itemId);
            getOperationsList();
            return true;
        } else {
            if (null == mQuickAddController) {
                initQuickAdd();
            }
            getOperationsList();
            return false;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> res;
        switch (i) {
            case GET_OPS:
                res = OperationTable.getOpsBetweenDateLoader(mActivity, startOpDate, mActivity.getCurrentAccountId());
                mOperationsLoader = (CursorLoader) res;
                break;
            default:
                res = null;
        }
        return res;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case GET_OPS:
                boolean refresh = false;
                if (mOpListCursorAdapter == null) {
                    String[] from = new String[]{OperationTable.KEY_OP_DATE,
                            InfoTables.KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_SUM,
                            InfoTables.KEY_TAG_NAME, InfoTables.KEY_MODE_NAME, OperationTable.KEY_OP_CHECKED};

                    int[] to = new int[]{R.id.op_date, R.id.op_third_party, R.id.op_sum, R.id.op_infos};
                    mOpListCursorAdapter =
                            new OperationsCursorAdapter(mActivity, this, R.layout.operation_row, from, to, cursor,
                                    new OperationRowViewBinder(mActivity, this, cursor,
                                            OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE)
                            );
                    refresh = true;
                }
                mListView.setAdapter(mOpListCursorAdapter);
                Cursor old = mOpListCursorAdapter.swapCursor(cursor);
                if (old != null) {
                    ((OperationRowViewBinder) mOpListCursorAdapter.getViewBinder()).increaseCache(cursor);
                    old.close();
                }
//                if (mQuickAddController == null) {
//                    initQuickAdd();
//                } else {
//                    mQuickAddController.initViewBehavior();
//                    mQuickAddController.clearFocus();
//                }
                if (refresh || needRefreshSelection) {
                    needRefreshSelection = false;
                    refreshSelection();
                }
                break;
        }
    }

    public static void restart(Context ctx) {
        DbContentProvider.reinit(ctx);
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);
    }

    /**
     * get the operations of current account
     * should be called after getAccountList
     */
    private void getOperationsList() {
        Log.d(TAG, "getOperationsList mActivity.getCurrentAccountId() : " + mActivity.getCurrentAccountId());

        if (mActivity.getCurrentAccountId() != null) {
            Log.d(TAG, "getOperationsList mOperationsLoader  : " + mOperationsLoader);
            if (mOperationsLoader == null) {
                startOpDate = Tools.createClearedCalendar();
//                if (mScrollLoader == null) {
//                    initOperationList();
//                }
                assert mScrollLoader != null;
                mScrollLoader.setStartDate(startOpDate);
                startOpDate.set(Calendar.DAY_OF_MONTH, startOpDate.getActualMinimum(Calendar.DAY_OF_MONTH));
                Log.d(TAG, "startOpDate : " + Tools.getDateStr(startOpDate));
                getLoaderManager().initLoader(GET_OPS, null, this);
            } else {
                getLoaderManager().restartLoader(GET_OPS, null, this);
            }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode) {
//            case AccountEditorFragment.ACCOUNT_EDITOR:
//                if (resultCode == RESULT_OK) {
//                    updateAccountList();
//                } else { // back without filling an account
//                    this.wasBackWithoutAccountSaved = true;
//                }
//                break;
//            case OperationEditorFragment.OPERATION_EDITOR:
//                if (resultCode == RESULT_OK) {
//                    this.needRefreshSelection = true;
//                    this.mLastSelectionId = data.getLongExtra("opId", this.mLastSelectionId);
//                    long date = data.getLongExtra("opDate", 0);
//                    if (date > 0) {
//                        GregorianCalendar opDate = new GregorianCalendar();
//                        opDate.setTimeInMillis(date);
//                        opDate.set(Calendar.DAY_OF_MONTH, 1);
//                        GregorianCalendar today = Tools.createClearedCalendar();
//                        if (today.get(Calendar.MONTH) > opDate.get(Calendar.MONTH)) {
//                            this.startOpDate = opDate;
//                        }
//                    }
//                    mAccountManager.backupCurAccountId();
//                    updateAccountList();
//                    getOperationsList();
//                }
//                break;
//        }
//    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (!mActivity.isFinishing()) {
            switch (cursorLoader.getId()) {
                case GET_OPS:
                    Cursor old = mOpListCursorAdapter.swapCursor(null);
                    if (old != null) {
                        old.close();
                    }
                    mOperationsLoader = null;
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void updateDisplay(Intent intent) {
        getOperationsList();
    }

    @Override
    public void onFetchAllAccountCbk() {
//        getOperationsList();
//        if (mListView == null) {
//            initOperationList();
//        } else {
//            getOperationsList();
//        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.operations_list_menu, menu);
//        inflater.inflate(R.menu.common_menu, menu);
        if (Tools.DEBUG_MODE) {
            inflater.inflate(R.menu.debug_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.go_to_create_account:
//                AccountEditorFragment.callMeForResult(this, AccountEditorFragment.NO_ACCOUNT);
//                return true;
//            case R.id.go_to_edit_account:
//                long id = mAccountAdapter.getItemId(getSupportActionBar().getSelectedNavigationIndex());
//                AccountEditorFragment.callMeForResult(this, id);
//                return true;
            case R.id.create_operation:
                OperationEditor.callMeForResult(mActivity, OperationEditor.NO_OPERATION,
                        mActivity.getCurrentAccountId());
                return true;
//            case R.id.go_to_sch_op:
//                ScheduledOpListFragment.callMe(this, mAccountId);
//                return true;
//            case R.id.delete_account:
//                DeleteAccountConfirmationDialog.newInstance(mAccountId).show(getSupportFragmentManager(), "delAccount");
//                return true;
//            case R.id.go_to_op_checking:
//                CheckingOpFragment.callMe(this, mAccountId);
//                return true;
            default:
                return Tools.onDefaultOptionItemSelected(mActivity, item);
        }
    }

    public long computeSumFromCursor(Cursor op) {
        long sum = 0L;
        if (null != op) {
            final int dateIdx = op.getColumnIndex(OperationTable.KEY_OP_DATE);
            final int opSumIdx = op.getColumnIndex(OperationTable.KEY_OP_SUM);
            final int transIdx = op.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID);
            long opDate = op.getLong(dateIdx);
            Log.d(TAG,
                    "computeSumFromCursor mProjectionDate : "
                            + Tools.getDateStr(mProjectionDate) + "  opDate : " + Tools.getDateStr(opDate)
            );
            if (!op.isBeforeFirst() && !op.isAfterLast()) {
                final int origPos = op.getPosition();
                boolean canContinue;
                if (opDate <= mProjectionDate || mProjectionDate == 0) {
                    canContinue = op.moveToPrevious();
                    if (canContinue) {
                        opDate = op.getLong(dateIdx);
                        while (canContinue && (opDate <= mProjectionDate || mProjectionDate == 0)) {
                            long s = op.getLong(opSumIdx);
                            if (op.getLong(transIdx) == mActivity.getCurrentAccountId()) {
                                s = -s;
                            }
                            sum = sum + s;
                            canContinue = op.moveToPrevious();
                            if (canContinue) {
                                opDate = op.getLong(dateIdx);
                            }
                        }
                        sum = -sum;
                    }
                } else {
                    sum = op.getLong(opSumIdx);
                    canContinue = op.moveToNext();
                    if (canContinue) {
                        opDate = op.getLong(dateIdx);
                        while (canContinue && opDate > mProjectionDate) {
                            long s = op.getLong(opSumIdx);
                            if (op.getLong(transIdx) == mActivity.getCurrentAccountId()) {
                                s = -s;
                            }
                            sum = sum + s;
                            canContinue = op.moveToNext();
                            if (canContinue) {
                                opDate = op.getLong(dateIdx);
                            }
                        }
                    }
                }
                op.moveToPosition(origPos);
            }
        }
        Log.d(TAG, "computeSumFromCursor after sum = " + sum);
        return sum;
    }

    @Override
    public ListView getListView() {
        return mListView;
    }

    @Override
    public DialogFragment getDeleteConfirmationDialog(final Operation operation) {
        if (operation.mScheduledId > 0) {
            return DeleteOccurenceConfirmationDialog.newInstance(operation.mAccountId, operation.mRowId,
                    operation.mScheduledId, operation.getDate(), operation.mTransferAccountId, this);
        } else {
            return DeleteOpConfirmationDialog.newInstance(operation.mAccountId, operation.mRowId, this);
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void selectOpAndAdjustOffset(final int position, boolean delayScroll) {
        if (position != mLastSelectionPos) {
            mLastSelectionPos = position;
            mLastSelectionId = getListAdapter().getItemId(position);

            if (delayScroll) {
                mListView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mListView.setSelection(position);
                        mListView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListView.getFirstVisiblePosition() == position) {
                                    mListView.smoothScrollToPosition(position - 3); // scroll in order to see fully expanded op row
                                } else if (mListView.getLastVisiblePosition() == position) {
                                    mListView.smoothScrollToPosition(position + 3); // scroll in order to see fully expanded op row
                                }
                            }
                        });
                    }
                }, 400);
            } else {
                mListView.setSelection(position);
                mListView.smoothScrollToPosition(position + 3); // scroll in order to see fully expanded op row
            }
            OperationsCursorAdapter adapter = mOpListCursorAdapter;
            adapter.setSelectedPosition(position);
        }
    }

    @Override
    public void getMoreOperations(final GregorianCalendar startDate) {
        Log.d("getMoreOperations", "startDate : " + startDate);
        if (startDate != null) {
            startOpDate = startDate;
            getOperationsList();
        } else {
            // no op found with cur month and month - 1, try if there is one
            Cursor c;
            Log.d("getMoreOperations", "startOpDate : " + startOpDate);
            if (null == startOpDate) {
                c = OperationTable.fetchLastOp(mActivity, mActivity.getCurrentAccountId());
            } else {
                c = OperationTable.fetchLastOpSince(mActivity, mActivity.getCurrentAccountId(),
                        startOpDate.getTimeInMillis());
            }
            Log.d("getMoreOperations", "cursor : " + c);
            if (c != null) {
                if (c.moveToFirst()) {
                    long date = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_DATE));
                    Log.d(TAG, "last chance date : " + Tools.getDateStr(date));
                    startOpDate = new GregorianCalendar();
                    startOpDate.setTimeInMillis(date);
                    mScrollLoader.setStartDate(startOpDate);
                    getOperationsList();
                }
                c.close();
            }
        }
    }

    // TODO : to cleanup, not need this in interface as we have MainActivity now
    @Override
    public long getCurrentAccountId() {
        return mActivity.getCurrentAccountId();
    }

    private void refreshSelection() {
        if (mLastSelectionId == -1) {
            GregorianCalendar today = Tools.createClearedCalendar();
            Cursor c = findLastOpBeforeDate(today);
            if (c != null) {
                selectOpAndAdjustOffset(c.getPosition(), true);
            }
        } else {
            selectOpAndAdjustOffset(findOpPosition(mLastSelectionId), true);
        }
    }

    private OperationsCursorAdapter getListAdapter() {
        return (OperationsCursorAdapter) mListView.getAdapter();
    }

    private Cursor findLastOpBeforeDate(GregorianCalendar date) {
        Cursor ops = getListAdapter().getCursor();
        if (ops.moveToFirst()) {
            long dateLong = date.getTimeInMillis();
            do {
                long opDate = ops.getLong(ops.getColumnIndex(OperationTable.KEY_OP_DATE));
                if (opDate <= dateLong) {
                    break;
                }
            } while (ops.moveToNext());
        }
        return ops;
    }

    private int findOpPosition(final long id) {
        for (int i = 0; i < getListAdapter().getCount(); i++) {
            if (getListAdapter().getItemId(i) == id) {
                return i;
            }
        }
        return -1;
    }

    protected void afterDelUpdateSelection() {
        mOpListCursorAdapter.setSelectedPosition(-1);
        mLastSelectionId = -1;
        mLastSelectionPos = -1;
        needRefreshSelection = true;
        getOperationsList();
        mActivity.updateAccountList();
    }

    public void onOperationEditorResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            this.mLastSelectionId = data.getLongExtra("opId", this.mLastSelectionId);
            long date = data.getLongExtra("opDate", 0);
            if (date > 0) {
                GregorianCalendar opDate = new GregorianCalendar();
                opDate.setTimeInMillis(date);
                opDate.set(Calendar.DAY_OF_MONTH, 1);
                GregorianCalendar today = Tools.createClearedCalendar();
                if (today.get(Calendar.MONTH) > opDate.get(Calendar.MONTH)) {
                    this.startOpDate = opDate;
                }
                getOperationsList();
            }
        }
    }

    protected static class DeleteOpConfirmationDialog extends DialogFragment {
        private long accountId;
        private long operationId;
        static OperationListFragment parentFrag;

        public static DeleteOpConfirmationDialog newInstance(final long accountId, final long opId,
                                                             final OperationListFragment parentFrag) {
            DeleteOpConfirmationDialog frag = new DeleteOpConfirmationDialog();
            DeleteOpConfirmationDialog.parentFrag = parentFrag;
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            args.putLong("opId", opId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            this.accountId = args.getLong("accountId");
            this.operationId = args.getLong("opId");
            return Tools.createDeleteConfirmationDialog(getActivity(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (OperationTable.deleteOp(getActivity(), operationId, accountId)) {
                        parentFrag.afterDelUpdateSelection();
                    }

                }
            });
        }
    }

    protected static class DeleteOccurenceConfirmationDialog extends DialogFragment {
        static OperationListFragment parentFrag;

        public static DeleteOccurenceConfirmationDialog newInstance(final long accountId, final long opId,
                                                                    final long schId, final long date,
                                                                    final long transfertId,
                                                                    final OperationListFragment parentFrag) {
            DeleteOccurenceConfirmationDialog.parentFrag = parentFrag;
            DeleteOccurenceConfirmationDialog frag = new DeleteOccurenceConfirmationDialog();
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            args.putLong("opId", opId);
            args.putLong("schId", schId);
            args.putLong("date", date);
            args.putLong("transfertId", transfertId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            final long accountId = args.getLong("accountId");
            final long operationId = args.getLong("opId");
            final long schId = args.getLong("schId");
            final long date = args.getLong("date");
            final long transfertId = args.getLong("transfertId");
            Log.d(TAG, "date of op to del : " + Tools.getDateStr(date));
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.delete_recurring_op)
                    .setCancelable(true)
                    .setPositiveButton(R.string.del_only_current,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (OperationTable.deleteOp(getActivity(), operationId, accountId)) {
                                        parentFrag.afterDelUpdateSelection();
                                    }
                                }
                            }
                    )
                    .setNeutralButton(R.string.del_all_following,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int nbDel =
                                            OperationTable.deleteAllFutureOccurrences(getActivity(), accountId, schId,
                                                    date, transfertId);
                                    Log.d(TAG, "nbDel : " + nbDel);
                                    if (nbDel > 0) {
                                        parentFrag.afterDelUpdateSelection();
                                    }
                                }
                            }
                    )
                    .setNegativeButton(R.string.del_all_occurrences,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (OperationTable.deleteAllOccurrences(getActivity(), accountId, schId, transfertId) > 0) {
                                        parentFrag.afterDelUpdateSelection();
                                    }
                                }
                            }
                    );
            return builder.create();
        }
    }

    public static class DeleteAccountConfirmationDialog extends DialogFragment {
        private long accountId;

        public static DeleteAccountConfirmationDialog newInstance(final long accountId) {
            DeleteAccountConfirmationDialog frag = new DeleteAccountConfirmationDialog();
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            this.accountId = args.getLong("accountId");
            return Tools.createDeleteConfirmationDialog(getActivity(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (AccountTable.deleteAccount(getActivity(), accountId)) {
                        MainActivity.refreshAccountList(getActivity());
                    } else {
                        ((MainActivity) getActivity()).getAccountManager().setCurrentAccountId(null);
                    }
                }
            }, R.string.account_delete_confirmation);
        }
    }
}

