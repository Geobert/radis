package fr.geobert.radis.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.TargetSumWatcher;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CheckingOpActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        IOperationList, UpdateDisplayInterface {
    public static final String CURRENT_ACCOUNT = "accountId";
    private static final int GET_UNCHECKED_OPS_OF_ACCOUNT = 1000;
    private TextView mStatusTxt;
    private EditText mTargetedSum;
    private ListView mListView;
    private SimpleCursorAdapter mAccountAdapter;
    private OperationsCursorAdapter mOpListAdapter;
    private long mCurrentAccount;
    private CursorLoader mLoader;
    private boolean initialized = false;
    private Cursor mUncheckedOps;
    private CheckingOpRowViewBinder mInnerOpViewBinder;

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
        mInnerOpViewBinder = new CheckingOpRowViewBinder(this, null,
                OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE);
        mOpListAdapter =
                new OperationsCursorAdapter(this, R.layout.operation_row, from, to, null,
                        mInnerOpViewBinder);
        mListView.setAdapter(mOpListAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));

        ActionBar actionbar = getSupportActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setIcon(R.drawable.op_checking_48);
        actionbar.setDisplayShowTitleEnabled(false);

        final TargetSumWatcher w = new TargetSumWatcher(
                Formater.getSumFormater().getDecimalFormatSymbols().getDecimalSeparator(), mTargetedSum, this);
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
//                        Cursor acc = (Cursor) mAccountAdapter.getItem(pos);
//                        final long curCheckedSum =
//                                acc.getLong(acc.getColumnIndex(AccountTable.KEY_ACCOUNT_CHECKED_OP_SUM));
                        mTargetedSum.setText(Formater.getSumFormater().format(mAccountManager.getCurrentAccountSum() / 100.d));
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
        mUncheckedOps = data;
        mOpListAdapter.changeCursor(data);
        mTargetedSum.selectAll();
        mTargetedSum.requestFocus();
        Tools.showKeyboard(this);
        this.initialized = true;
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
        final long total = mAccountManager.getCurrentAccountStartSum() + checkedSum;
        final long diff = target - total;
        final String currencySymbol = mAccountManager.getCurAccCurrencySymbol();
        mStatusTxt.setText(String.format("%s%s\n%s%s",
                Formater.getSumFormater().format(total / 100.0d), currencySymbol,
                Formater.getSumFormater().format(diff / 100.0d), currencySymbol));
        if (initialized && diff == 0) {
            Tools.popMessage(this, getString(R.string.targeted_sum_reached), R.string.op_checking,
                    getString(R.string.ok), null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.operations_checking_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.auto_checking:
                AutoCheckingDialog.newInstance(new AutoCheckingClickListener()).show(getSupportFragmentManager(),
                        "autochecking_dialog");
                return true;
            default:
                return Tools.onDefaultOptionItemSelected(this, item);
        }
    }

    private class AutoCheckingClickListener implements DialogInterface.OnClickListener {
        DatePicker datePicker;
//        EditText nbMaxOps;

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            doAutoChecking(new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(),
                    datePicker.getDayOfMonth()));
        }
    }

    private void doAutoChecking(final GregorianCalendar maxDate) {
        Cursor uncheckedOps = mUncheckedOps;
        if (uncheckedOps != null) {
            final int origPos = uncheckedOps.getPosition();
            final int sumIdx = uncheckedOps.getColumnIndex(OperationTable.KEY_OP_SUM);
            if (uncheckedOps.moveToLast()) {
                final long curCheckedSum = mAccountManager.getCurrentAccountCheckedSum();
                final long targetSum = Tools.extractSumFromStr(mTargetedSum.getText().toString());
                final int dateIdx = uncheckedOps.getColumnIndex(OperationTable.KEY_OP_DATE);
                final int checkedIdx = uncheckedOps.getColumnIndex(OperationTable.KEY_OP_CHECKED);
                Long sum = mAccountManager.getCurrentAccountStartSum();
                long total = 0;
                long opSum;
                ArrayList<Integer> checkedOpsPos = new ArrayList<Integer>();
                ArrayList<Integer> notCheckedOpsPos = new ArrayList<Integer>();
                // first pass
                do {
                    if (!uncheckedOps.isBeforeFirst() && !uncheckedOps.isAfterLast()) {
                        if (uncheckedOps.getLong(dateIdx) <= maxDate.getTimeInMillis() &&
                                uncheckedOps.getInt(checkedIdx) == 0) {
                            opSum = uncheckedOps.getLong(sumIdx);
                            total = curCheckedSum + sum + opSum;
                            if (total <= targetSum) {
                                sum += opSum;
                                checkedOpsPos.add(uncheckedOps.getPosition());
                            } else {
                                notCheckedOpsPos.add(uncheckedOps.getPosition());
                            }
                        }
                    }
                } while (uncheckedOps.moveToPrevious());

                final int opSumIdx = uncheckedOps.getColumnIndex(OperationTable.KEY_OP_SUM);
                if (notCheckedOpsPos.size() > 0 && total != targetSum) {
                    total = secondPass(uncheckedOps, targetSum, total, checkedOpsPos, notCheckedOpsPos, opSumIdx, false);
                }
                // update list display
                mInnerOpViewBinder.setCheckedPosition(checkedOpsPos);
                mOpListAdapter.notifyDataSetChanged();
                // update database AFTER display update because the request only return unchecked op
                // but we still want the autochecked transaction at this point

                final int accIdIdx = uncheckedOps.getColumnIndex(OperationTable.KEY_OP_ACCOUNT_ID);
                final int tranAccIdIdx = uncheckedOps.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID);
                for (Integer pos : checkedOpsPos) {
                    uncheckedOps.moveToPosition(pos);
                    final long opId = uncheckedOps.getLong(0);
                    opSum = uncheckedOps.getLong(opSumIdx);
                    final long accId = uncheckedOps.getLong(accIdIdx);
                    final long transAccId = uncheckedOps.getLong(tranAccIdIdx);
                    OperationTable.updateOpCheckedStatus(this, opId, opSum, accId, transAccId, true);
                }
                // update displayed sum AFTER update database
                updateDisplay(null);
                if (total != targetSum) {
                    Tools.popMessage(this,
                            String.format(getString(R.string.missing_ops), Formater.getSumFormater().format(total / 100),
                                    Formater.getSumFormater().format((targetSum - total) / 100)),
                            R.string.auto_checking,
                            getString(R.string.ok), null);
                }
            }
            uncheckedOps.moveToPosition(origPos);
        }

    }

    private boolean testOpSum(final boolean totalSuperiorToTarget, final long opSum) {
        if (totalSuperiorToTarget) {
            return opSum <= 0;
        } else {
            return opSum >= 0;
        }
    }

    private long secondPass(Cursor uncheckedOps, long targetSum, long total, ArrayList<Integer> checkedOpsPos,
                            ArrayList<Integer> notCheckedOpsPos, final int opSumIdx, final boolean isLastCall) {
        long opSum;
        ArrayList<Integer> tmp = new ArrayList<Integer>(notCheckedOpsPos);
        final boolean totalSuperiorToTarget = total > targetSum;
        for (Integer pos : tmp) {
            uncheckedOps.moveToPosition(pos);
            opSum = uncheckedOps.getLong(opSumIdx);
            if (testOpSum(totalSuperiorToTarget, opSum)) {
                total += opSum;
                checkedOpsPos.add(pos);
                notCheckedOpsPos.remove(pos);
            } else if (isLastCall) {
                notCheckedOpsPos.remove(pos);
            }
            if (total == targetSum) {
                return total;
            }
        }
        if (notCheckedOpsPos.size() > 0) {
            return secondPass(uncheckedOps, targetSum, total, checkedOpsPos, notCheckedOpsPos, opSumIdx, true);
        }
        return total;
    }

    private static class AutoCheckingDialog extends DialogFragment {
        private DatePicker mMaxDate;
        private AutoCheckingClickListener onOkListener;

        public static AutoCheckingDialog newInstance(AutoCheckingClickListener listener) {
            AutoCheckingDialog frag = new AutoCheckingDialog();
            frag.onOkListener = listener;
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View v = inflater.inflate(R.layout.autochecking_dialog, null);
            mMaxDate = (DatePicker) v.findViewById(R.id.max_date);
            GregorianCalendar d = new GregorianCalendar();
            mMaxDate.updateDate(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH));
            onOkListener.datePicker = mMaxDate;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(v).setTitle(R.string.auto_checking).setNegativeButton(R.string.cancel, null).
                    setPositiveButton(R.string.do_check, onOkListener);
            return builder.create();
        }
    }
}
