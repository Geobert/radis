package fr.geobert.radis.ui;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.tools.*;
import fr.geobert.radis.ui.editor.AccountEditor;
import fr.geobert.radis.ui.editor.OperationEditor;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;

public class OperationListActivity extends BaseActivity implements
        UpdateDisplayInterface, LoaderCallbacks<Cursor> {
    public static final String INTENT_UPDATE_OP_LIST = "fr.geobert.radis.UPDATE_OP_LIST";
    private static final String TAG = "OperationListActivity";
    private static final int GET_ACCOUNTS = 200;
    private static final int GET_OPS = 300;
    public static boolean ROBOTIUM_MODE = false;
    private boolean mFirstStart = true;
    private OnInsertionReceiver mOnInsertionReceiver;
    private IntentFilter mOnInsertionIntentFilter;
    private CursorLoader mAccountLoader;
    private CursorLoader mOperationsLoader;
    private SimpleCursorAdapter mAccountAdapter;
    private int redColor;
    private int greenColor;
    private Integer mLastSelectedPosition = null;
    private boolean mOnRestore = false;
    private QuickAddController mQuickAddController;
    private long mProjectionDate;
    private boolean mReceiverIsRegistered;
    private OperationsCursorAdapter mOpListCursorAdapter;
    private ListView mListView;
    private AdapterView.AdapterContextMenuInfo mCurrentSelectedOp;
    private GregorianCalendar startOpDate; // start date of ops to get


    private GregorianCalendar endOpDate; // end date of ops to get


    private static GregorianCalendar date1 = new GregorianCalendar();
    private Long mAccountId = null;

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
        mQuickAddController.clearFocus();
    }

    private void initOperationList() {
        mQuickAddController = new QuickAddController(this, this);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(findViewById(android.R.id.empty));

        mLastSelectedPosition = null;
        mReceiverIsRegistered = false;

        mOnInsertionReceiver = new OnInsertionReceiver(this);
        mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);
        registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
//        registerReceiver(mOnInsertionReceiver, new IntentFilter(
//                OperationListActivity.INTENT_UPDATE_ACC_LIST));
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
                return false; // TODO
            }
        });
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
        showProgress();
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
        hideProgress();
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
                    mOpListCursorAdapter = new OperationsCursorAdapter(this,
                            R.layout.operation_row, from, to, cursor);
                    mListView.setAdapter(mOpListCursorAdapter);
                }
                Cursor old = mOpListCursorAdapter.swapCursor(cursor);
                if (old != null) {
                    ((InnerViewBinder) mOpListCursorAdapter.getViewBinder()).increaseCache(cursor);
                    old.close();
                }
//                updateSumsAndSelection();
                mQuickAddController.clearFocus();
                break;
        }
    }

    private void processAccountList() {
        Cursor allAccounts = AccountManager.getInstance().getAllAccountsCursor();
        if (allAccounts != null && allAccounts.getCount() > 0) {
            // get the ops
            getOperationsList();
        } else {
            // no account, open create account
            AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT);
        }
    }

    /**
     * get the operations of current account
     * should be called after getAccountList
     */
    private void getOperationsList() {
        mAccountId = AccountManager.getInstance().getCurrentAccountId(this);
        if (mAccountId != null) {
            showProgress();

            if (mOperationsLoader == null) {
                startOpDate = new GregorianCalendar();
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
        showProgress();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static GregorianCalendar date2 = new GregorianCalendar();

    private static class OpRowHolder {
        public TextView separator;
        public ImageView scheduledImg;
        public StringBuilder tagBuilder = new StringBuilder();
    }    // used in InnerViewBinder

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

    private class InnerViewBinder extends OpViewBinder {
        /**
         * State of ListView item that has never been determined.
         */
        private static final int STATE_UNKNOWN = 0;
        /**
         * State of a ListView item that is sectioned. A sectioned item must
         * display the separator.
         */
        private static final int STATE_SECTIONED_CELL = 1;
        /**
         * State of a ListView item that is not sectioned and therefore does not
         * display the separator.
         */
        private static final int STATE_REGULAR_CELL = 2;
        private int[] mCellStates = null;

        public InnerViewBinder(Activity activity, Cursor c) {
            super(activity, OperationTable.KEY_OP_SUM,
                    OperationTable.KEY_OP_DATE, R.id.op_icon, mAccountId);
            initCache(c);
        }

        public void initCache(Cursor cursor) {
            mCellStates = cursor == null ? null : new int[cursor.getCount()];
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            String colName = cursor.getColumnName(columnIndex);
            if (colName.equals(InfoTables.KEY_TAG_NAME)) {
                final OpRowHolder h = (OpRowHolder) ((View) view.getParent()
                        .getParent()).getTag();
                TextView textView = ((TextView) view);
                StringBuilder b = h.tagBuilder;
                b.setLength(0);
                String s = cursor.getString(columnIndex);
                if (null != s) {
                    b.append(s);
                } else {
                    b.append('−');
                }
                b.append(" / ");
                s = cursor.getString(columnIndex + 1);
                if (null != s) {
                    b.append(s);
                } else {
                    b.append('−');
                }
                textView.setText(b.toString());

                ImageView i = h.scheduledImg;
                if (cursor.getLong(cursor
                        .getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)) > 0) {
                    i.setVisibility(View.VISIBLE);
                } else {
                    i.setVisibility(View.GONE);
                }

                boolean needSeparator = false;
                final int position = cursor.getPosition();
                assert (mCellStates != null);
                date1.setTimeInMillis(cursor.getLong(cursor
                        .getColumnIndex(OperationTable.KEY_OP_DATE)));
                switch (mCellStates[position]) {
                    case STATE_SECTIONED_CELL:
                        needSeparator = true;
                        break;

                    case STATE_REGULAR_CELL:
                        needSeparator = false;
                        break;

                    case STATE_UNKNOWN:
                    default:
                        // A separator is needed if it's the first itemview of the
                        // ListView or if the group of the current cell is different
                        // from the previous itemview.
                        if (position == 0) {
                            needSeparator = true;
                        } else {
                            cursor.moveToPosition(position - 1);
                            date2.setTimeInMillis(cursor.getLong(cursor
                                    .getColumnIndex(OperationTable.KEY_OP_DATE)));
                            if (date1.get(GregorianCalendar.MONTH) != date2
                                    .get(GregorianCalendar.MONTH)) {
                                needSeparator = true;
                            }
                            cursor.moveToPosition(position);
                        }
                }
                TextView separator = h.separator;
                if (needSeparator) {
                    separator.setText(DateFormat.format("MMMM", date1));
                    separator.setVisibility(View.VISIBLE);
                } else {
                    separator.setVisibility(View.GONE);
                }
                return true;
            } else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
                TextView textView = ((TextView) view);
                final long transfertId = cursor
                        .getLong(cursor
                                .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
                if (transfertId > 0 && transfertId == mAccountId) {
                    textView.setText(cursor.getString(cursor
                            .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)));
                    return true;
                } else {
                    String name = cursor.getString(cursor
                            .getColumnIndex(colName));
                    if (name != null) {
                        textView.setText(name);
                        return true;
                    } else {
                        name = cursor
                                .getString(cursor
                                        .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME));
                        if (name != null) {
                            textView.setText(name);
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            } else {
                return super.setViewValue(view, cursor, columnIndex);
            }
        }

        public void increaseCache(Cursor c) {
            int[] tmp = mCellStates;
            initCache(c);
            for (int i = 0; i < tmp.length; ++i) {
                mCellStates[i] = tmp[i];
            }
        }
    }

    private class OperationsCursorAdapter extends SimpleCursorAdapter {
        OperationsCursorAdapter(Activity context, int layout, String[] from,
                                int[] to, Cursor cursor) {
            super(context, layout, null, from, to,
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            InnerViewBinder viewBinder = new InnerViewBinder(context, cursor);
            setViewBinder(viewBinder);
        }

        @Override
        public Cursor swapCursor(Cursor c) {
            Cursor old = super.swapCursor(c);
            ((InnerViewBinder) getViewBinder()).initCache(c);
            return old;
        }

        @Override
        public void changeCursor(Cursor c) {
            super.changeCursor(c);
            ((InnerViewBinder) getViewBinder()).initCache(c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            OpRowHolder h = new OpRowHolder();
            h.separator = (TextView) v.findViewById(R.id.separator);
            h.scheduledImg = (ImageView) v.findViewById(R.id.op_sch_icon);
            v.setTag(h);
            return v;
        }
    }
}

