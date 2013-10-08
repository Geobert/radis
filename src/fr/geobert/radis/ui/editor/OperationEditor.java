package fr.geobert.radis.ui.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.tools.Tools;

public class OperationEditor extends CommonOpEditor {
    public static final long NO_OPERATION = 0;
    public static final int OPERATION_EDITOR = 2000;
    private static final String TAG = "OperationEditor";
    private static final int GET_OP = 610;
    protected Operation mOriginalOp;
    private OperationEditFragment mEditFragment;

    public static void callMeForResult(final BaseActivity context, final long opId, final long accountId) {
        final Intent intent = new Intent(context, OperationEditor.class);
        intent.putExtra(PARAM_OP_ID, opId);
        intent.putExtra(AccountEditor.PARAM_ACCOUNT_ID, accountId);
        context.startActivityForResult(intent, OPERATION_EDITOR);
    }

    @Override
    protected void setView() {
        setContentView(R.layout.operation_edit);
        mEditFragment = (OperationEditFragment) getSupportFragmentManager().findFragmentById(R.id.main_edit_pane);
    }

    @Override
    protected void fetchOrCreateCurrentOp() {
        if (mRowId > 0) {
            fetchOp(GET_OP);
        } else {
            mCurrentOp = new Operation();
            mCurrentOp.mAccountId = mCurAccountId;
            populateFields();
        }
    }

    @Override
    protected void populateFields() {
        mEditFragment.populateCommonFields(mCurrentOp);
    }

    private void onCancelClicked() {
        setResult(CommonOpEditor.RESULT_CANCELED);
        finish();
    }

    private void onOkClicked() {
        if (mRowId <= 0 || mOriginalOp != null) {
            StringBuilder errMsg = new StringBuilder();
            if (mEditFragment.isFormValid(errMsg)) {
                fillOperationWithInputs(mCurrentOp);
                saveOpAndExit();
            } else {
                Tools.popError(this,
                        errMsg.toString(), null);
            }
        }
    }

    private void setResAndExit() {
        Intent res = new Intent();
        res.putExtra("opId", mRowId);
        res.putExtra("opDate", mCurrentOp.getDate());
        setResult(RESULT_OK, res);
        finish();
    }

    @Override
    protected void saveOpAndExit() {
        Operation op = mCurrentOp;
        Log.d(TAG, "saveOpAndExit, mRowId : " + mRowId);
        if (mRowId <= 0) {
            mRowId = OperationTable.createOp(this, op, op.mAccountId);
            setResAndExit();
        } else {
            if (op.equals(mOriginalOp)) {
                setResAndExit();
            } else {
                if (op.mScheduledId > 0 && !op.equalsButDate(mOriginalOp)) {
                    UpdateScheduledOp.newInstance(mCurrentOp, mPreviousSum, mRowId).show(getSupportFragmentManager(),
                            "dialog");
                } else {
                    OperationTable.updateOp(this, mRowId, op, mOriginalOp);
                    setResAndExit();
                }
            }
        }
    }

    @Override
    protected void fillOperationWithInputs(Operation operation) {
        mEditFragment.fillOperationWithInputs(operation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.confirm_cancel_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cancel:
                onCancelClicked();
                return true;
            case R.id.confirm:
                onOkClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("originalOp", mOriginalOp);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mOriginalOp = savedInstanceState.getParcelable("originalOp");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
        CursorLoader l = null;
        if (l == null) {
            switch (id) {
                case GET_OP:
                    l = new CursorLoader(this,
                            Uri.parse(DbContentProvider.OPERATION_JOINED_URI + "/"
                                    + mRowId), OperationTable.OP_COLS_QUERY, null,
                            null, null);
                    break;

                default:
                    break;
            }
        }
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        hideProgress();
        switch (loader.getId()) {
            case GET_OP:
                data.moveToFirst();
                mCurrentOp = new Operation(data);
                mOriginalOp = new Operation(data);
                populateFields();
                break;
            default:
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    protected static class UpdateScheduledOp extends DialogFragment {
        public static UpdateScheduledOp newInstance(Operation currentOp, long previousSum, long rowId) {
            UpdateScheduledOp frag = new UpdateScheduledOp();
            Bundle args = new Bundle();
            args.putLong("previousSum", previousSum);
            args.putLong("rowId", rowId);
            args.putParcelable("currentOp", currentOp);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final OperationEditor act = (OperationEditor) getActivity();
            Bundle args = getArguments();
            final Operation currentOp = args.getParcelable("currentOp");
            final long previousSum = args.getLong("previousSum");
            final long rowId = args.getLong("rowId");
            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            builder.setMessage(R.string.ask_update_scheduling)
                    .setCancelable(false)
                    .setPositiveButton(R.string.update,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    final ScheduledOperation op =
                                            new ScheduledOperation(currentOp, currentOp.mAccountId);
                                    if (ScheduledOperationTable.updateScheduledOp(act, currentOp.mScheduledId, op,
                                            true)) {
                                        AccountTable.updateProjection(act, act.mCurAccountId, previousSum - op.mSum,
                                                op.getDate());
                                    }
                                    ScheduledOperationTable.updateAllOccurences(getActivity(), op, previousSum,
                                            currentOp.mScheduledId);
                                    act.setResAndExit();
                                }
                            })
                    .setNeutralButton(R.string.disconnect,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    currentOp.mScheduledId = 0;
                                    OperationTable.updateOp(act, rowId, currentOp, act.mOriginalOp);
                                    act.setResAndExit();
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            });
            return builder.create();
        }
    }
}

