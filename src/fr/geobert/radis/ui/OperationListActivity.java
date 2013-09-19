package fr.geobert.radis.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.PrefsManager;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;
import fr.geobert.radis.ui.editor.AccountEditor;
import fr.geobert.radis.ui.editor.OperationEditor;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;

public class OperationListActivity extends BaseActivity implements
        UpdateDisplayInterface, LoaderCallbacks<Cursor>, IOperationList {
    // robotium test set this to true to activate database cleaning on launch
    public static boolean ROBOTIUM_MODE = false;

    public static final String INTENT_UPDATE_OP_LIST = "fr.geobert.radis.UPDATE_OP_LIST";
    public static final String INTENT_UPDATE_ACC_LIST = "fr.geobert.radis.UPDATE_ACC_LIST";
    private static final String TAG = "OperationListActivity";
    private static final int GET_OPS = 300;
    private boolean mFirstStart = true;
    private OnInsertionReceiver mOnInsertionReceiver;
    private IntentFilter mOnInsertionIntentFilter;
    private CursorLoader mOperationsLoader;
    private SimpleCursorAdapter mAccountAdapter;
    private int redColor;
    private int greenColor;
    private QuickAddController mQuickAddController = null;
    private OperationsCursorAdapter mOpListCursorAdapter;
    private ListView mListView;
    private GregorianCalendar startOpDate; // start date of ops to get
    private Long mAccountId = null;
    private OnOperationScrollLoader mScrollLoader;
    private long mProjectionDate;
    private long mLastSelectionId = -1;
    private int mLastSelectionPos = -1;
    private boolean needRefreshSelection = false;

    public static void refreshAccountList(final Context ctx) {
        Intent intent = new Intent(INTENT_UPDATE_ACC_LIST);
        ctx.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.checkDebugMode(this);
        cleanDatabaseIfTestingMode();

        Resources resources = getResources();
        redColor = resources.getColor(R.color.op_alert);
        greenColor = resources.getColor(R.color.positiveSum);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionbar.setDisplayShowTitleEnabled(false);

        setContentView(R.layout.operation_list);

        installRadisTimer();
    }

    private void doOnResume() {
        AccountManager accMan = mAccountManager;
        Long curDefaultAccId = accMan.mCurDefaultAccount;
        if (curDefaultAccId != null && curDefaultAccId != accMan.getDefaultAccountId(this)) {
            accMan.mCurDefaultAccount = null;
            accMan.setCurrentAccountId(null);
            mLastSelectionId = -1;
        }
        onFetchAllAccountCbk();
        if (mQuickAddController != null) {
            mQuickAddController.setAutoNegate(true);
            mQuickAddController.clearFocus();
            setQuickAddVisibility();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DBPrefsManager.getInstance(this).fillCache(this, new Runnable() {
            @Override
            public void run() {
                consolidateDbIfNeeded();
                if (mAccountAdapter == null) {
                    initAccountStuff();
                }
                mAccountManager.fetchAllAccounts(OperationListActivity.this, false, new Runnable() {
                    @Override
                    public void run() {
                        doOnResume();
                    }
                });
            }
        });
    }

    private void initQuickAdd() {
        mQuickAddController = new QuickAddController(this, this);
        mQuickAddController.setAccount(mAccountId);
        mQuickAddController.initViewBehavior();
        mQuickAddController.setAutoNegate(true);
        mQuickAddController.clearFocus();
        setQuickAddVisibility();
    }

    private void setQuickAddVisibility() {
        if (mQuickAddController != null) {
            boolean hideQuickAdd = DBPrefsManager.getInstance(this).getBoolean(RadisConfiguration.KEY_HIDE_OPS_QUICK_ADD);
            int visibility = View.VISIBLE;
            if (hideQuickAdd) {
                visibility = View.GONE;
            }
            mQuickAddController.setVisibility(visibility);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mQuickAddController != null) {
            mQuickAddController.onSaveInstanceState(outState);
        }
        if (mAccountId != null) {
            outState.putLong("mAccountId", mAccountId);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAccountId = savedInstanceState.getLong("mAccountId");
        final Bundle sis = savedInstanceState;
        DBPrefsManager.getInstance(this).fillCache(this, new Runnable() {
            @Override
            public void run() {
                initQuickAdd();
                mQuickAddController.onRestoreInstanceState(sis);
                if (mAccountManager.getSimpleCursorAdapter() == null) {
                    initAccountStuff();
                }
                if (mAccountAdapter == null || mAccountAdapter.isEmpty()) {
                    updateDisplay(null);
                }
            }
        });
    }

    private void initOperationList() {
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectOpAndAdjustOffset(i, false);
            }
        });
        mScrollLoader = new OnOperationScrollLoader(this);
        mListView.setOnScrollListener(mScrollLoader);
        mListView.setCacheColorHint(getResources().getColor(android.R.color.transparent));
        mListView.setSelector(android.R.color.transparent);
        mOnInsertionReceiver = new OnInsertionReceiver(this);
        mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);
        registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
        registerReceiver(mOnInsertionReceiver, new IntentFilter(
                OperationListActivity.INTENT_UPDATE_ACC_LIST));
        registerReceiver(mOnInsertionReceiver, new IntentFilter(
                INTENT_UPDATE_OP_LIST));
    }

    private void initAccountStuff() {
        String[] from = new String[]{AccountTable.KEY_ACCOUNT_NAME,
                AccountTable.KEY_ACCOUNT_CUR_SUM,
                AccountTable.KEY_ACCOUNT_CUR_SUM_DATE,
                AccountTable.KEY_ACCOUNT_CURRENCY};

        int[] to = new int[]{android.R.id.text1, R.id.account_sum, R.id.account_balance_at};

        mAccountAdapter = new SimpleCursorAdapter(this, R.layout.account_row, null, from, to,
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mAccountAdapter.setViewBinder(new SimpleAccountViewBinder());
        mAccountManager.setSimpleCursorAdapter(mAccountAdapter);
        getSupportActionBar().setListNavigationCallbacks(mAccountAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return onAccountChanged(itemId);
            }
        });
    }

    private boolean onAccountChanged(long itemId) {
        if (mQuickAddController != null && itemId != mAccountId) {
            mAccountManager.setCurrentAccountId(itemId);
            ((OperationRowViewBinder) mOpListCursorAdapter.getViewBinder()).setCurrentAccountId(itemId);
            mQuickAddController.setAccount(itemId);
            getOperationsList();
            return true;
        } else {
            if (mQuickAddController == null) {
                getOperationsList();
                initQuickAdd();
            }
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOnInsertionReceiver != null) {
            unregisterReceiver(mOnInsertionReceiver);
        }
    }

    protected void consolidateDbIfNeeded() {
        PrefsManager prefs = PrefsManager.getInstance(this);
        Boolean needConsolidate = prefs.getBoolean("consolidateDB", false);
        Log.d(TAG, "needConsolidate : " + needConsolidate);
        if (needConsolidate) {
            RadisService.acquireStaticLock(this);
            this.startService(new Intent(this, RadisService.class));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> res;
        switch (i) {
            case GET_OPS:
                res = OperationTable.getOpsBetweenDateLoader(this, startOpDate, mAccountId);
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
                            InfoTables.KEY_TAG_NAME, InfoTables.KEY_MODE_NAME};

                    int[] to = new int[]{R.id.op_date, R.id.op_third_party, R.id.op_sum, R.id.op_infos};
                    mOpListCursorAdapter =
                            new OperationsCursorAdapter(this, R.layout.operation_row, from, to, cursor,
                                    new OperationRowViewBinder(this, cursor,
                                            OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE));
                    mListView.setAdapter(mOpListCursorAdapter);
                    refresh = true;
                }
                Cursor old = mOpListCursorAdapter.swapCursor(cursor);
                if (old != null) {
                    ((OperationRowViewBinder) mOpListCursorAdapter.getViewBinder()).increaseCache(cursor);
                    old.close();
                }
                if (mQuickAddController == null) {
                    initQuickAdd();
                }
                mQuickAddController.clearFocus();
                if (refresh || needRefreshSelection) {
                    needRefreshSelection = false;
                    refreshSelection();
                }
                break;
        }
    }

    private void processAccountList() {
        AccountManager accMan = mAccountManager;
        Cursor allAccounts = accMan.getAllAccountsCursor();
        if (allAccounts == null || allAccounts.getCount() == 0) {
            // no account, open create account
            AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT);
        } else {
            mAccountId = accMan.getCurrentAccountId(this);
            if (mAccountId != null) {
                getSupportActionBar().setSelectedNavigationItem(accMan.getCurrentAccountPosition(this));
            }
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
        mAccountId = mAccountManager.getCurrentAccountId(this);
        if (mAccountId != null) {
            if (mOperationsLoader == null) {
                startOpDate = new GregorianCalendar();
                mScrollLoader.setStartDate(startOpDate);
                startOpDate.set(Calendar.DAY_OF_MONTH, startOpDate.getActualMinimum(Calendar.DAY_OF_MONTH));
                Tools.clearTimeOfCalendar(startOpDate);
                Log.d(TAG, "startOpDate : " + Tools.getDateStr(startOpDate));
                getSupportLoaderManager().initLoader(GET_OPS, null, this);
            } else {
                getSupportLoaderManager().restartLoader(GET_OPS, null, this);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case AccountEditor.ACCOUNT_EDITOR:
                if (resultCode == RESULT_OK) {
                    updateAccountList();
                }
                break;
            case OperationEditor.OPERATION_EDITOR:
                if (resultCode == RESULT_OK) {
                    this.needRefreshSelection = true;
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
                    }
                    mAccountManager.backupCurAccountId();
                    updateAccountList();
                    updateOperationList();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (!this.isFinishing()) {
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
        updateAccountList();
    }

    private void updateOperationList() {
        getOperationsList();
    }

    private void updateAccountList() {
        mAccountManager.fetchAllAccounts(this, true, new Runnable() {
            @Override
            public void run() {
                onFetchAllAccountCbk();
            }
        });
    }

    private void onFetchAllAccountCbk() {
        if (mListView == null) {
            initOperationList();
        }
        processAccountList();
        updateOperationList();
    }

    private void cleanDatabaseIfTestingMode() {
        // if run by robotium, delete database, can't do it from robotium test, it leads to crash
        // see http://stackoverflow.com/questions/12125656/robotium-testing-failed-because-of-deletedatabase
        if (ROBOTIUM_MODE) {
            DBPrefsManager.getInstance(this).resetAll();
            ContentProviderClient client = getContentResolver()
                    .acquireContentProviderClient("fr.geobert.radis.db");
            DbContentProvider provider = (DbContentProvider) client
                    .getLocalContentProvider();
            provider.deleteDatabase(this);
            client.release();
        }
    }

    private void installRadisTimer() {
        if (mFirstStart) {
            Intent i = new Intent(this, InstallRadisServiceReceiver.class);
            i.setAction(Tools.INTENT_RADIS_STARTED);
            sendBroadcast(i);
            RadisService.callMe(this);
            mFirstStart = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.operations_list_menu, menu);
        inflater.inflate(R.menu.common_menu, menu);
        if (Tools.DEBUG_MODE) {
            inflater.inflate(R.menu.debug_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.go_to_create_account:
                AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT);
                return true;
            case R.id.go_to_edit_account:
                long id = mAccountAdapter.getItemId(getSupportActionBar().getSelectedNavigationIndex());
                AccountEditor.callMeForResult(this, id);
                return true;
            case R.id.create_operation:
                OperationEditor.callMeForResult(this, OperationEditor.NO_OPERATION, mAccountId);
                return true;
            case R.id.go_to_sch_op:
                ScheduledOpListActivity.callMe(this, mAccountId);
                return true;
            case R.id.delete_account:
                DeleteAccountConfirmationDialog.newInstance(mAccountId).show(getSupportFragmentManager(), "delAccount");
                return true;
            default:
                return Tools.onDefaultOptionItemSelected(this, item);
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
                            + Tools.getDateStr(mProjectionDate) + "  opDate : " + Tools.getDateStr(opDate));
            if (!op.isBeforeFirst() && !op.isAfterLast()) {
                final int origPos = op.getPosition();
                boolean canContinue;
                if (opDate <= mProjectionDate || mProjectionDate == 0) {
                    canContinue = op.moveToPrevious();
                    if (canContinue) {
                        opDate = op.getLong(dateIdx);
                        while (canContinue && (opDate <= mProjectionDate || mProjectionDate == 0)) {
                            long s = op.getLong(opSumIdx);
                            if (op.getLong(transIdx) == mAccountId) {
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
                            if (op.getLong(transIdx) == mAccountId) {
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
                    operation.mScheduledId, operation.getDate(), operation.mTransferAccountId);
        } else {
            return DeleteOpConfirmationDialog.newInstance(operation.mAccountId, operation.mRowId);
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
        if (startDate != null) {
            startOpDate = startDate;
            getOperationsList();
        } else {
            // no op found with cur month and month - 1, try if there is one
            Cursor c = OperationTable.fetchLastOp(this, mAccountId);
            if (c != null) {
                if (c.moveToFirst()) {
                    long date = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_DATE));
                    Log.d(TAG, "last chance date : " + Tools.getDateStr(date));
                    startOpDate = new GregorianCalendar();
                    startOpDate.setTimeInMillis(date);
                    getOperationsList();
                }
                c.close();
            }
        }
    }

    @Override
    public long getCurrentAccountId() {
        return mAccountId;
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
        updateOperationList();
        updateAccountList();
    }

    protected static class DeleteOpConfirmationDialog extends DialogFragment {
        private long accountId;
        private long operationId;

        public static DeleteOpConfirmationDialog newInstance(final long accountId, final long opId) {
            DeleteOpConfirmationDialog frag = new DeleteOpConfirmationDialog();
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
                    final OperationListActivity activity = (OperationListActivity) getActivity();
                    if (OperationTable.deleteOp(activity, operationId, accountId)) {
                        activity.afterDelUpdateSelection();
                    }

                }
            });
        }
    }

    protected static class DeleteOccurenceConfirmationDialog extends DialogFragment {
        public static DeleteOccurenceConfirmationDialog newInstance(final long accountId, final long opId,
                                                                    final long schId, final long date,
                                                                    final long transfertId) {
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
                                    final OperationListActivity activity = (OperationListActivity) getActivity();
                                    if (OperationTable.deleteOp(activity, operationId, accountId)) {
                                        activity.afterDelUpdateSelection();
                                    }
                                }
                            })
                    .setNeutralButton(R.string.del_all_following,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final OperationListActivity activity = (OperationListActivity) getActivity();
                                    int nbDel =
                                            OperationTable.deleteAllFutureOccurrences(activity, accountId, schId,
                                                    date, transfertId);
                                    Log.d(TAG, "nbDel : " + nbDel);
                                    if (nbDel > 0) {
                                        activity.afterDelUpdateSelection();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.del_all_occurrences,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    final OperationListActivity activity = (OperationListActivity) getActivity();
                                    if (OperationTable.deleteAllOccurrences(activity, accountId, schId, transfertId) > 0) {
                                        activity.afterDelUpdateSelection();
                                    }
                                }
                            });
            return builder.create();
        }
    }

    protected static class DeleteAccountConfirmationDialog extends DialogFragment {
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
                        OperationListActivity.refreshAccountList(getActivity());
                    } else {
                        ((OperationListActivity) getActivity()).mAccountManager.setCurrentAccountId(null);
                    }
                }
            }, R.string.account_delete_confirmation);
        }
    }

    // for accounts list
    private class SimpleAccountViewBinder implements SimpleCursorAdapter.ViewBinder {
        private int ACCOUNT_NAME_COL = -1;
        private int ACCOUNT_CUR_SUM;
        private int ACCOUNT_CUR_SUM_DATE;
        private int ACCOUNT_CURRENCY;
        private String currencySymbol = null;

        private SimpleAccountViewBinder() {
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int i) {
            if (ACCOUNT_NAME_COL == -1) {
                ACCOUNT_NAME_COL = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME);
                ACCOUNT_CUR_SUM = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM);
                ACCOUNT_CUR_SUM_DATE = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE);
                ACCOUNT_CURRENCY = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CURRENCY);
                try {
                    currencySymbol = Currency.getInstance(cursor.getString(ACCOUNT_CURRENCY)).getSymbol();
                } catch (IllegalArgumentException ex) {
                    currencySymbol = "";
                }
            }

            boolean res;
            if (i == ACCOUNT_NAME_COL) {
                TextView textView = (TextView) view;
                textView.setText(cursor.getString(i));
                res = true;
            } else if (i == ACCOUNT_CUR_SUM) {
                TextView textView = (TextView) view;
                StringBuilder stringBuilder = new StringBuilder();
                long sum = cursor.getLong(i);
                if (sum < 0) {
                    textView.setTextColor(redColor);
                } else {
                    textView.setTextColor(greenColor);
                }
                stringBuilder.append(Formater.getSumFormater().format(sum / 100.0d));
                stringBuilder.append(' ').append(currencySymbol);
                textView.setText(stringBuilder);
                res = true;
            } else if (i == ACCOUNT_CUR_SUM_DATE) {
                TextView textView = (TextView) view;
                long dateLong = cursor.getLong(i);
                if (mAccountId == cursor.getLong(0)) {
                    mProjectionDate = dateLong;
                }
                StringBuilder stringBuilder = new StringBuilder();
                if (dateLong > 0) {
                    stringBuilder.append(String.format(
                            getString(R.string.balance_at),
                            Formater.getFullDateFormater().format(
                                    new Date(dateLong))));
                } else {
                    stringBuilder.append(getString(R.string.current_sum));
                }
                textView.setText(stringBuilder);
                res = true;
            } else {
                res = false;
            }
            return res;
        }

    }

    @Override
    public AccountManager getAccountManager() {
        return mAccountManager;
    }
}

