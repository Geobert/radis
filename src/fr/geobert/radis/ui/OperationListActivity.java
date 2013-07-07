package fr.geobert.radis.ui;

import android.annotation.TargetApi;
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
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
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
    private static final int GET_ACCOUNTS = 200;
    private static final int GET_OPS = 300;
    private boolean mFirstStart = true;
    private OnInsertionReceiver mOnInsertionReceiver;
    private IntentFilter mOnInsertionIntentFilter;
    private CursorLoader mAccountLoader;
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

    public static void refreshAccountList(final Context ctx) {
        Intent intent = new Intent(INTENT_UPDATE_ACC_LIST);
        ctx.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cleanDatabaseIfTestingMode();

        ActionBar actionbar = getSupportActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionbar.setDisplayShowTitleEnabled(false);

        initAccountStuff();

        setContentView(R.layout.operation_list);
        initOperationList();

        DBPrefsManager.getInstance(this).fillCache(this, new Runnable() {
            @Override
            public void run() {
                onPrefsInit();
            }
        });

        installRadisTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mQuickAddController != null) {
            mQuickAddController.setAutoNegate(true);
            mQuickAddController.clearFocus();
            updateOperationList();
        }
    }

    private void initQuickAdd() {
        mQuickAddController = new QuickAddController(this, this);
        mQuickAddController.setAccount(mAccountId);
        mQuickAddController.initViewBehavior();
        mQuickAddController.setAutoNegate(true);
        mQuickAddController.clearFocus();
        boolean hideQuickAdd = DBPrefsManager.getInstance(this).getBoolean(
                RadisConfiguration.KEY_HIDE_OPS_QUICK_ADD);
        int visibility = View.VISIBLE;
        if (hideQuickAdd) {
            visibility = View.GONE;
        }
        mQuickAddController.setVisibility(visibility);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mQuickAddController != null) {
            mQuickAddController.onSaveInstanceState(outState);
        }
        outState.putLong("mAccountId", mAccountId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAccountId = savedInstanceState.getLong("mAccountId");
        initQuickAdd();
        mQuickAddController.onRestoreInstanceState(savedInstanceState);
    }

    private void initOperationList() {
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectOpAndAdjustOffset(i);
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
        Resources resources = getResources();
        redColor = resources.getColor(R.color.op_alert);
        greenColor = resources.getColor(R.color.positiveSum);

        // Create an array to specify the fields we want to display in the list
        String[] from = new String[]{AccountTable.KEY_ACCOUNT_NAME,
                AccountTable.KEY_ACCOUNT_CUR_SUM,
                AccountTable.KEY_ACCOUNT_CUR_SUM_DATE,
                AccountTable.KEY_ACCOUNT_CURRENCY};

        // and an array of the fields we want to bind those fields to (in this
        // case just text1)
        int[] to = new int[]{android.R.id.text1, R.id.account_sum, R.id.account_balance_at};

        mAccountAdapter = new SimpleCursorAdapter(this, R.layout.account_row, null, from, to,
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mAccountAdapter.setViewBinder(new SimpleAccountViewBinder());
        AccountManager.getInstance().setSimpleCursorAdapter(mAccountAdapter);
        getSupportActionBar().setListNavigationCallbacks(mAccountAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return onAccountChanged(itemId);
            }
        });
    }

    private boolean onAccountChanged(long itemId) {
        if (mQuickAddController != null && itemId != mAccountId) {
            AccountManager.getInstance().setCurrentAccountId(itemId);
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
        unregisterReceiver(mOnInsertionReceiver);
    }

    protected void onPrefsInit() {
        getAccountList();
    }

    private void getAccountList() {
//        showProgress();
        if (mAccountLoader == null) {
            getSupportLoaderManager().initLoader(GET_ACCOUNTS, null, this);
        } else {
            getSupportLoaderManager().restartLoader(GET_ACCOUNTS, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Loader<Cursor> res;
        switch (i) {
            case GET_ACCOUNTS:
                res = AccountTable.getAllAccountsLoader(this);
                mAccountLoader = (CursorLoader) res;
                break;
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
            case GET_ACCOUNTS:
                AccountManager.getInstance().setAllAccountsCursor(cursor);
                processAccountList();
                break;
            case GET_OPS:
                if (mOpListCursorAdapter == null) {
                    String[] from = new String[]{OperationTable.KEY_OP_DATE,
                            InfoTables.KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_SUM,
                            InfoTables.KEY_TAG_NAME, InfoTables.KEY_MODE_NAME};

                    int[] to = new int[]{R.id.op_date, R.id.op_third_party, R.id.op_sum,
                            R.id.op_infos};
                    mOpListCursorAdapter =
                            new OperationsCursorAdapter(this, R.layout.operation_row, from, to, cursor,
                                    new OperationRowViewBinder(this, cursor,
                                            OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE));
                    mListView.setAdapter(mOpListCursorAdapter);
                }
                Cursor old = mOpListCursorAdapter.swapCursor(cursor);
                if (old != null) {
                    ((OperationRowViewBinder) mOpListCursorAdapter.getViewBinder()).increaseCache(cursor);
                    old.close();
                }
                mQuickAddController.clearFocus();
                break;
        }
    }

    private void processAccountList() {
        AccountManager accMan = AccountManager.getInstance();
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
//        DbContentProvider.reinit(ctx);
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);
    }

//    public static void restart(OperationListActivity ctx) {
//        ctx.getSupportLoaderManager().destroyLoader(GET_ACCOUNTS);
//        ctx.getSupportLoaderManager().destroyLoader(GET_OPS);
//        ctx.updateDisplay(null);
//    }

    /**
     * get the operations of current account
     * should be called after getAccountList
     */
    private void getOperationsList() {
        mAccountId = AccountManager.getInstance().getCurrentAccountId(this);
        if (mAccountId != null) {
            //showProgress();

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
        } else {
            // no account, open create account, should never happened
            AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT);
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
                    updateAccountList();
                    updateOperationList();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        switch (cursorLoader.getId()) {
            case GET_ACCOUNTS:
                AccountManager.getInstance().setAllAccountsCursor(null);
                break;
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

    @Override
    public void updateDisplay(Intent intent) {
        updateAccountList();
        updateOperationList();
    }

    private void updateOperationList() {
        getOperationsList();
    }

    private void updateAccountList() {
//        showProgress();
        if (mAccountLoader == null) {
            getSupportLoaderManager().initLoader(GET_ACCOUNTS, null, this);
        } else {
            getSupportLoaderManager().restartLoader(GET_ACCOUNTS, null, this);
        }
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
                            + Formater.getFullDateFormater().format(
                            new Date(mProjectionDate)) + "  opDate : " + Tools.getDateStr(opDate));
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
    public DialogFragment getDeleteConfirmationDialog(long accountId, long opId) {
        return DeleteOpConfirmationDialog.newInstance(accountId, opId);
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void selectOpAndAdjustOffset(int position) {
        OperationsCursorAdapter adapter = mOpListCursorAdapter;
        adapter.setSelectedPosition(position);
        mListView.smoothScrollToPosition(position + 2); // scroll in order to see fully expanded op row
    }

    @Override
    public void getMoreOperations(final GregorianCalendar startDate) {
        startOpDate = startDate;
        getOperationsList();
    }

    @Override
    public long getCurrentAccountId() {
        return mAccountId;
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
                        activity.updateOperationList();
                        activity.updateAccountList();
                        activity.mOpListCursorAdapter.setSelectedPosition(-1);
                    }

                }
            });
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
                        AccountManager.getInstance().setCurrentAccountId(null);
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

        private SimpleAccountViewBinder() {
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int i) {
            if (ACCOUNT_NAME_COL == -1) {
                ACCOUNT_NAME_COL = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME);
                ACCOUNT_CUR_SUM = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM);
                ACCOUNT_CUR_SUM_DATE = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE);
                ACCOUNT_CURRENCY = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CURRENCY);
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
                stringBuilder.append(' ').append(Currency
                        .getInstance(cursor.getString(ACCOUNT_CURRENCY))
                        .getSymbol());
                textView.setText(stringBuilder);
                res = true;
            } else if (i == ACCOUNT_CUR_SUM_DATE) {
                TextView textView = (TextView) view;
                long dateLong = cursor.getLong(i);
                mProjectionDate = dateLong;
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
}

