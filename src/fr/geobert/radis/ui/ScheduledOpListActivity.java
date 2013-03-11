package fr.geobert.radis.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.*;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.OpViewBinder;
import fr.geobert.radis.ui.editor.ScheduledOperationEditor;

import java.util.Date;

public class ScheduledOpListActivity extends BaseActivity implements
        LoaderCallbacks<Cursor> {
    public static final String CURRENT_ACCOUNT = "accountId";
    private static final String TAG = "ScheduleOpList";
    // context menu ids
    private static final int DELETE_OP_ID = Menu.FIRST + 1;
    private static final int EDIT_OP_ID = Menu.FIRST + 2;
    // dialog ids
    private static final int DIALOG_DELETE = 0;
    private static final int GET_ALL_SCH_OPS = 900;
    private static final int GET_SCH_OPS_OF_ACCOUNT = 910;
    private AdapterContextMenuInfo mOpToDelete;
    //    private Spinner mAccountSpinner;
    private long mCurrentAccount;
    private ListView mListView;
    private SimpleCursorAdapter mAdapter;
    private CursorLoader mLoader;
    private TextView mTotalLbl;
    private SimpleCursorAdapter mAccountAdapter;
    private boolean isResuming = false;

    public static void callMe(Context ctx, final long currentAccountId) {
        Intent i = new Intent(ctx, ScheduledOpListActivity.class);
        i.putExtra(CURRENT_ACCOUNT, currentAccountId);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setIcon(R.drawable.sched_48);
        actionbar.setDisplayShowTitleEnabled(false);

        setContentView(R.layout.scheduled_list);

        mListView = (ListView) findViewById(android.R.id.list);
//        registerForContextMenu(mListView);
        mTotalLbl = (TextView) findViewById(R.id.sch_op_sum_total);
        mCurrentAccount = getIntent().getLongExtra(CURRENT_ACCOUNT, 0);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        String[] from = new String[]{OperationTable.KEY_OP_DATE,
                InfoTables.KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_SUM,
                AccountTable.KEY_ACCOUNT_NAME,
                ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT,
                ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY,
                ScheduledOperationTable.KEY_SCHEDULED_END_DATE,};

        int[] to = new int[]{R.id.scheduled_date, R.id.scheduled_third_party,
                R.id.scheduled_sum, R.id.scheduled_account,
                R.id.scheduled_infos};
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.scheduled_row,
                null,
                from,
                to,
                android.support.v4.widget.SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mAdapter.setViewBinder(new InnerViewBinder());
        mListView.setAdapter(mAdapter);

        populateAccountSpinner();
    }

//    private void fetchAllSchOps() {
//        showProgress();
//        if (mLoader == null) {
//            getSupportLoaderManager().initLoader(GET_ALL_SCH_OPS, null, this);
//        } else {
//            getSupportLoaderManager()
//                    .restartLoader(GET_ALL_SCH_OPS, null, this);
//        }
//    }

    private void fetchSchOpsOfAccount() {
        showProgress();
        if (mLoader == null) {
            getSupportLoaderManager().initLoader(GET_SCH_OPS_OF_ACCOUNT, null,
                    this);
        } else {
            getSupportLoaderManager().restartLoader(GET_SCH_OPS_OF_ACCOUNT,
                    null, this);
        }
    }

    private void populateAccountSpinner() {
        Cursor c = AccountManager.getInstance().getAllAccountsCursor();
        if (c != null && c.moveToFirst()) {
            mAccountAdapter = new SimpleCursorAdapter(this, R.layout.sch_account_row, c,
                    new String[]{AccountTable.KEY_ACCOUNT_NAME},
                    new int[]{android.R.id.text1}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
//            ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this,
//                    R.layout.sch_account_row, android.R.id.text1);
//            adapter.add(new Account(0, getString(R.string.all_accounts)));
//            do {
//                adapter.add(new Account(c));
//            } while (c.moveToNext());

            //mAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            if (mCurrentAccount != 0) {
                int pos = 0;
                while (pos < mAccountAdapter.getCount()) {
                    long id = mAccountAdapter.getItemId(pos);
                    if (id == mCurrentAccount) {
                        getSupportActionBar().setSelectedNavigationItem(pos);
                        break;
                    } else {
                        pos++;
                    }
                }
            }

            getSupportActionBar().setListNavigationCallbacks(mAccountAdapter, new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    if (!isResuming) {
                        mCurrentAccount = itemId;
                        ((InnerViewBinder) mAdapter.getViewBinder())
                                .setCurAccount(mCurrentAccount);
//                        if (mCurrentAccount != 0) {
                        fetchSchOpsOfAccount();
//                        } else {
//                            fetchAllSchOps();
//                        }
                    }
                    return true;
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK) {
//            fetchSchOpsOfAccount();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResuming = true;
//        populateAccountSpinner(AccountList.getAllAccounts(this));
//        if (mCurrentAccount == 0) {
//            fetchAllSchOps();
//        } else {
        fetchSchOpsOfAccount();
//        }
    }

//    @Override
//    protected Dialog onCreateDialog(int id) {
//        switch (id) {
//            case DIALOG_DELETE:
//                return askDeleteOccurrences();
//            default:
//                return Tools.onDefaultCreateDialog(this, id);
//        }
//    }

    private AlertDialog askDeleteOccurrences() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.ask_delete_occurrences)
                .setCancelable(false)
                .setPositiveButton(R.string.del_all_occurrences,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteSchOp(true);
                            }
                        })
                .setNeutralButton(R.string.cancel_delete_occurrences,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteSchOp(false);
                            }
                        })
                .setNegativeButton(R.string.cancel_sch_deletion,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    private void deleteSchOp(final boolean delAllOccurrences) {
        AdapterContextMenuInfo op = mOpToDelete;
        Cursor cursorOp = ScheduledOperationTable.fetchOneScheduledOp(this,
                op.id);
        final long transId = cursorOp.getLong(cursorOp
                .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
        if (delAllOccurrences) {
            ScheduledOperationTable.deleteAllOccurences(this, op.id);
        }
        if (ScheduledOperationTable.deleteScheduledOp(this, op.id)) {
            int req;
            if (mCurrentAccount == 0) {
                req = GET_ALL_SCH_OPS;
            } else {
                req = GET_SCH_OPS_OF_ACCOUNT;
            }
            if (mLoader != null) {
                getSupportLoaderManager().restartLoader(req, null, this);
            } else {
                getSupportLoaderManager().initLoader(req, null, this);
            }
            if (transId > 0) {
                AccountTable.consolidateSums(this, transId);
            }
        }
        Log.d(TAG, "REFRESH AFTER DEL SCH OP");
//        AccountList.refreshDisplay(this);
        mOpToDelete = null;
    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v,
//                                    ContextMenuInfo menuInfo) {
//        if (((AdapterContextMenuInfo) menuInfo).id != -1) {
//            super.onCreateContextMenu(menu, v, menuInfo);
//            menu.add(0, EDIT_OP_ID, 0, R.string.edit);
//            menu.add(0, DELETE_OP_ID, 0, R.string.delete);
//        }
//    }

//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
//                .getMenuInfo();
//        switch (item.getItemId()) {
//            case DELETE_OP_ID:
//                showDialog(DIALOG_DELETE);
//                mOpToDelete = info;
//                return true;
//            case EDIT_OP_ID:
//                startEditScheduledOperation(info.id);
//                return true;
//        }
//        return super.onContextItemSelected(item);
//    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.create_operation:
                ScheduledOperationEditor.callMeForResult(this, 0, mCurrentAccount,
                        ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.scheduled_list_menu, menu);
        inflater.inflate(R.menu.common_menu, menu);
        return true;
    }

//    @Override
//    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
//        if (Tools.onKeyLongPress(keyCode, event, this)) {
//            return true;
//        }
//        return super.onKeyLongPress(keyCode, event);
//    }

    //    @Override
//    public boolean onMenuItemSelected(int featureId, MenuItem item) {
//        if (Tools.onDefaultMenuSelected(this, featureId, item)) {
//            return true;
//        }
//        return super.onMenuItemSelected(featureId, item);
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case GET_ALL_SCH_OPS:
                mLoader = new CursorLoader(this,
                        DbContentProvider.SCHEDULED_JOINED_OP_URI,
                        ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null,
                        null, ScheduledOperationTable.SCHEDULED_OP_ORDERING);
                break;
            case GET_SCH_OPS_OF_ACCOUNT:
                mLoader = new CursorLoader(this,
                        DbContentProvider.SCHEDULED_JOINED_OP_URI,
                        ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, "sch."
                        + ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID
                        + " = ? OR sch."
                        + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = ?",
                        new String[]{Long.toString(mCurrentAccount),
                                Long.toString(mCurrentAccount)},
                        ScheduledOperationTable.SCHEDULED_OP_ORDERING);
                break;
            default:
                break;
        }
        return mLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        hideProgress();
        mAdapter.changeCursor(data);
        if (mCurrentAccount != 0) {
            computeTotal(data);
        } else {
            mTotalLbl.setText(R.string.sched_op_total_choose_account);
        }
    }

    private void computeTotal(Cursor data) {
        long credit = 0;
        long debit = 0;
        if (data != null && data.getCount() > 0 && data.moveToFirst()) {
            do {
                long s = data.getLong(data
                        .getColumnIndex(OperationTable.KEY_OP_SUM));
                final long transId = data.getLong(data
                        .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
                if (transId == mCurrentAccount) {
                    s = -s;
                }
                if (s >= 0) {
                    credit += s;
                } else {
                    debit += s;
                }
            } while (data.moveToNext());
        }
        mTotalLbl.setText(String.format(getString(R.string.sched_op_total_sum),
                Formater.getSumFormater().format(credit / 100.0d), Formater
                .getSumFormater().format(debit / 100.0d), Formater
                .getSumFormater().format((credit + debit) / 100.0d)));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private class InnerViewBinder extends OpViewBinder {

        public InnerViewBinder() {
            super(ScheduledOpListActivity.this, OperationTable.KEY_OP_SUM,
                    OperationTable.KEY_OP_DATE, R.id.scheduled_icon,
                    mCurrentAccount);
        }

        public void setCurAccount(long accountId) {
            mCurAccountId = accountId;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            String colName = cursor.getColumnName(columnIndex);
            if (colName
                    .equals(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT)) {
                StringBuilder b = new StringBuilder();
                int periodicityUnit = cursor.getInt(columnIndex);
                int periodicity = cursor.getInt(columnIndex - 1);
                long endDate = cursor.getLong(columnIndex - 2);
                b.append(ScheduledOperation.getUnitStr(ScheduledOpListActivity.this,
                        periodicityUnit, periodicity));
                b.append(" - ");
                if (endDate > 0) {
                    b.append(Formater.getFullDateFormater().format(
                            new Date(endDate)));
                } else {
                    b.append(ScheduledOpListActivity.this
                            .getString(R.string.no_end_date));
                }
                ((TextView) view).setText(b.toString());
                return true;
            } else if (colName.equals(mDateColName)) {
                Date date = new Date(cursor.getLong(columnIndex));
                ((TextView) view).setText(Formater.getFullDateFormater()
                        .format(date));
                return true;
            } else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
                if (mCurrentAccount != 0
                        && mCurrentAccount == cursor
                        .getLong(cursor
                                .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))) {
                    ((TextView) view)
                            .setText(cursor.getString(cursor
                                    .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)));
                    return true;
                } else {
                    return super.setViewValue(view, cursor, columnIndex);
                }
            } else if (colName.equals(AccountTable.KEY_ACCOUNT_NAME)) {
                if (mCurrentAccount != 0
                        && mCurrentAccount == cursor
                        .getLong(cursor
                                .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))) {
                    ((TextView) view).setText(cursor.getString(cursor
                            .getColumnIndex(InfoTables.KEY_THIRD_PARTY_NAME)));
                    return true;
                } else {
                    return super.setViewValue(view, cursor, columnIndex);
                }
            } else {
                return super.setViewValue(view, cursor, columnIndex);
            }
        }
    }

}
