package fr.geobert.radis.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;

import java.util.GregorianCalendar;

public class CheckingOpActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        IOperationList, UpdateDisplayInterface {
    public static final String CURRENT_ACCOUNT = "accountId";
    private static final int GET_UNCHECKED_OPS_OF_ACCOUNT = 1000;
    private TextView mStatusTxt;
    private EditText mTargetedSum;
    private ListView mListView;
    private SimpleCursorAdapter mAccountAdapter;
    private OperationsCursorAdapter mOpListCursorAdapter;
    private long mCurrentAccount;
    private CursorLoader mLoader;

    public static void callMe(Context ctx, final long currentAccountId) {
        Intent i = new Intent(ctx, CheckingOpActivity.class);
        i.putExtra(CURRENT_ACCOUNT, currentAccountId);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checking_list);
        mStatusTxt = (TextView) findViewById(R.id.checking_op_status);
        mTargetedSum = (EditText) findViewById(R.id.targeted_sum);
        mListView = (ListView) findViewById(android.R.id.list);

        mCurrentAccount = getIntent().getLongExtra(CURRENT_ACCOUNT, 0);
        String[] from = new String[]{OperationTable.KEY_OP_DATE,
                InfoTables.KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_SUM,
                InfoTables.KEY_TAG_NAME, InfoTables.KEY_MODE_NAME, OperationTable.KEY_OP_CHECKED};

        int[] to = new int[]{R.id.op_date, R.id.op_third_party, R.id.op_sum, R.id.op_infos};
        mOpListCursorAdapter =
                new OperationsCursorAdapter(this, R.layout.operation_row, from, to, null,
                        new CheckingOpRowViewBinder(this, null,
                                OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE));
        mListView.setAdapter(mOpListCursorAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));

        ActionBar actionbar = getSupportActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setIcon(R.drawable.op_checking_48);
        actionbar.setDisplayShowTitleEnabled(false);

        final CorrectCommaWatcher w = new CorrectCommaWatcher(
                Formater.getSumFormater().getDecimalFormatSymbols().getDecimalSeparator(), mTargetedSum);
        w.setAutoNegate(false);
        mTargetedSum.addTextChangedListener(w);
        mTargetedSum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    w.setAutoNegate(false);
                    ((EditText) v).selectAll();
                }
            }
        });
    }

    private boolean onAccountChanged(long itemId) {
        return false;
    }

    private void initAccountStuff() {
        getSupportActionBar().setListNavigationCallbacks(mAccountAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return onAccountChanged(itemId);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initAccountStuff();
        mAccountManager.fetchAllAccounts(this, false, new Runnable() {
            @Override
            public void run() {
                populateAccountSpinner();
                fetchOpForChecking();
            }
        });
    }

    private void populateAccountSpinner() {
        Cursor c = mAccountManager.getAllAccountsCursor();
        if (c != null && c.moveToFirst()) {
            mAccountAdapter = new SimpleCursorAdapter(this, R.layout.sch_account_row, c,
                    new String[]{AccountTable.KEY_ACCOUNT_NAME},
                    new int[]{android.R.id.text1}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            mAccountManager.setSimpleCursorAdapter(mAccountAdapter);
            if (mCurrentAccount != 0) {
                int pos = 0;
                while (pos < mAccountAdapter.getCount()) {
                    long id = mAccountAdapter.getItemId(pos);
                    if (id == mCurrentAccount) {
                        getSupportActionBar().setSelectedNavigationItem(pos);
                        mAccountManager.setCurrentAccountId(id);
                        Cursor acc = (Cursor) mAccountAdapter.getItem(pos);
                        final long curCheckedSum =
                                acc.getLong(acc.getColumnIndex(AccountTable.KEY_ACCOUNT_CHECKED_OP_SUM));
                        mTargetedSum.setText(Formater.getSumFormater().format(curCheckedSum / 100.d));
                        updateDisplay(null);
                        break;
                    } else {
                        pos++;
                    }
                }
            }

            getSupportActionBar().setListNavigationCallbacks(mAccountAdapter, new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    mCurrentAccount = itemId;
                    fetchOpForChecking();
                    return true;
                }
            });
        }
    }

    private void fetchOpForChecking() {
        if (mLoader == null) {
            getSupportLoaderManager().initLoader(GET_UNCHECKED_OPS_OF_ACCOUNT, null, this);
        } else {
            getSupportLoaderManager().restartLoader(GET_UNCHECKED_OPS_OF_ACCOUNT, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        mLoader = new CursorLoader(this,
                DbContentProvider.OPERATION_JOINED_URI,
                OperationTable.OP_COLS_QUERY,
                OperationTable.RESTRICT_TO_ACCOUNT + " AND ops." + OperationTable.KEY_OP_CHECKED + " = 0",
                new String[]{Long.toString(mCurrentAccount),
                        Long.toString(mCurrentAccount)},
                OperationTable.OP_ORDERING);
        return mLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> objectLoader, Cursor data) {
        mOpListCursorAdapter.changeCursor(data);

    }

    @Override
    public void onLoaderReset(Loader objectLoader) {

    }

    @Override
    public void getMoreOperations(GregorianCalendar startDate) {

    }

    @Override
    public long getCurrentAccountId() {
        return mCurrentAccount;
    }

    @Override
    public long computeSumFromCursor(Cursor cursor) {
        return 0;
    }

    @Override
    public ListView getListView() {
        return mListView;
    }

    @Override
    public DialogFragment getDeleteConfirmationDialog(Operation op) {
        return null;
    }

    @Override
    public void updateDisplay(Intent intent) {
        final long target = Tools.extractSumFromStr(mTargetedSum.getText().toString());
        final long checkedSum = mAccountManager.getCurrentAccountCheckedSum();
        final String currencySymbol = mAccountManager.getCurAccCurrencySymbol();
        mStatusTxt.setText(String.format("%s%s\n%s%s",
                Formater.getSumFormater().format(checkedSum / 100.0d), currencySymbol,
                Formater.getSumFormater().format((target - checkedSum) / 100.0d), currencySymbol));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return Tools.onDefaultOptionItemSelected(this, item);
        }
    }
}
