package fr.geobert.radis.ui;

import android.annotation.TargetApi;
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
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.ui.editor.ScheduledOperationEditor;

import java.util.GregorianCalendar;

public class ScheduledOpListActivity extends BaseActivity implements LoaderCallbacks<Cursor>, IOperationList {
    public static final String CURRENT_ACCOUNT = "accountId";
    private static final String TAG = "ScheduleOpList";

    // dialog ids
    private static final int GET_ALL_SCH_OPS = 900;
    private static final int GET_SCH_OPS_OF_ACCOUNT = 910;

    //    private Spinner mAccountSpinner;
    private long mCurrentAccount;
    private ListView mListView;
    private OperationsCursorAdapter mAdapter;
    private CursorLoader mLoader;
    private TextView mTotalLbl;
    private SimpleCursorAdapter mAccountAdapter;

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
        mTotalLbl = (TextView) findViewById(R.id.sch_op_sum_total);
        mCurrentAccount = getIntent().getLongExtra(CURRENT_ACCOUNT, 0);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        String[] from = new String[]{OperationTable.KEY_OP_DATE, InfoTables.KEY_THIRD_PARTY_NAME,
                OperationTable.KEY_OP_SUM,
                ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT,
                ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY,
                ScheduledOperationTable.KEY_SCHEDULED_END_DATE,};

        int[] to = new int[]{R.id.op_date, R.id.op_third_party, R.id.op_sum, R.id.op_infos};
        mAdapter = new OperationsCursorAdapter(this, R.layout.operation_row, from, to, null,
                new SchedOpRowViewBinder(this, null, OperationTable.KEY_OP_SUM, OperationTable.KEY_OP_DATE));
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectOpAndAdjustOffset(i);
            }
        });
        populateAccountSpinner();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void selectOpAndAdjustOffset(int position) {
        OperationsCursorAdapter adapter = mAdapter;
        adapter.setSelectedPosition(position);
        mListView.smoothScrollToPosition(position + 2); // scroll in order to see fully expanded op row
    }

    private void fetchSchOpsOfAccount() {
        if (mLoader == null) {
            getSupportLoaderManager().initLoader(GET_SCH_OPS_OF_ACCOUNT, null, this);
        } else {
            getSupportLoaderManager().restartLoader(GET_SCH_OPS_OF_ACCOUNT, null, this);
        }
    }

    private void populateAccountSpinner() {
        Cursor c = AccountManager.getInstance().getAllAccountsCursor();
        if (c != null && c.moveToFirst()) {
            mAccountAdapter = new SimpleCursorAdapter(this, R.layout.sch_account_row, c,
                    new String[]{AccountTable.KEY_ACCOUNT_NAME},
                    new int[]{android.R.id.text1}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
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
                    mCurrentAccount = itemId;
                    fetchSchOpsOfAccount();
                    return true;
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchSchOpsOfAccount();
    }

    private void deleteSchOp(final boolean delAllOccurrences, final long opId) {
        Cursor cursorOp = ScheduledOperationTable.fetchOneScheduledOp(this,
                opId);
        final long transId = cursorOp.getLong(cursorOp
                .getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
        boolean needRefresh = false;
        if (delAllOccurrences) {
            ScheduledOperationTable.deleteAllOccurences(this, opId);
            needRefresh = true;
            OperationListActivity.refreshAccountList(this);
        }
        if (ScheduledOperationTable.deleteScheduledOp(this, opId)) {
            needRefresh = true;
        }
        if (needRefresh) {
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
            default:
                return Tools.onDefaultOptionItemSelected(this, item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.scheduled_list_menu, menu);
        inflater.inflate(R.menu.common_menu, menu);
        return true;
    }

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
        computeTotal(data);
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

    @Override
    public void getMoreOperations(GregorianCalendar startDate) {
        // not needed
    }

    @Override
    public long getCurrentAccountId() {
        return mCurrentAccount;
    }

    @Override
    public long computeSumFromCursor(Cursor cursor) {
        // not needed
        return 0;
    }

    @Override
    public ListView getListView() {
        return mListView;
    }

    @Override
    public DialogFragment getDeleteConfirmationDialog(final Operation op) {
        return DeleteOpConfirmationDialog.newInstance(op.mRowId);
    }

    protected static class DeleteOpConfirmationDialog extends DialogFragment {
        private long operationId;

        public static DeleteOpConfirmationDialog newInstance(final long opId) {
            DeleteOpConfirmationDialog frag = new DeleteOpConfirmationDialog();
            Bundle args = new Bundle();
            args.putLong("opId", opId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            this.operationId = args.getLong("opId");
            final ScheduledOpListActivity activity = (ScheduledOpListActivity) getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.ask_delete_occurrences)
                    .setCancelable(false)
                    .setPositiveButton(R.string.del_all_occurrences,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    activity.deleteSchOp(true, operationId);
                                }
                            })
                    .setNeutralButton(R.string.cancel_delete_occurrences,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    activity.deleteSchOp(false, operationId);
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
    }
}
