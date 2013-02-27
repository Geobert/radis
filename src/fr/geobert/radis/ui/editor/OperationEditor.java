package fr.geobert.radis.ui.editor;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.Tools;

public class OperationEditor extends CommonOpEditor {
    public static final long NO_OPERATION = 0;
    public static final int OPERATION_EDITOR = 2000;
    protected static final int ASK_UPDATE_SCHEDULED_DIALOG_ID = 10;
    private static final String TAG = "OperationEditor";
    private static final int GET_OP = 610;
    protected Operation mOriginalOp;
    private OperationEditFragment mEditFragment;

    public static void callMeForResult(BaseActivity context, long opId, long accountId) {
        Intent intent = new Intent(context, OperationEditor.class);
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
        StringBuilder errMsg = new StringBuilder();

        if (mEditFragment.isFormValid(errMsg)) {
            fillOperationWithInputs(mCurrentOp);
            saveOpAndExit();
        } else {
            Tools.popError(this,
                    errMsg.toString(), null);
        }
    }

    private void setResAndExit() {
        Intent res = new Intent();
        setResult(RESULT_OK, res);
        finish();
    }

    @Override
    protected void saveOpAndExit() {
        Operation op = mCurrentOp;
        Log.d(TAG, "saveOpAndExit, mRowId : " + mRowId);
        if (mRowId <= 0) {
            OperationTable.createOp(this, op, op.mAccountId);
            setResAndExit();
        } else {
            if (op.equals(mOriginalOp)) {
                setResAndExit();
            } else {
                if (op.mScheduledId > 0 && !op.equalsButDate(mOriginalOp)) {
                    showDialog(ASK_UPDATE_SCHEDULED_DIALOG_ID);
                } else {
                    OperationTable.updateOp(this, mRowId, op, op.mAccountId);
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

    //    @Override
//    protected Dialog onCreateDialog(int id) {
//        switch (id) {
//            case ASK_UPDATE_SCHEDULED_DIALOG_ID:
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setMessage(R.string.ask_update_scheduling)
//                        .setCancelable(false)
//                        .setPositiveButton(R.string.update,
//                                new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog,
//                                                        int which) {
//                                        final ScheduledOperation op = new ScheduledOperation(
//                                                mCurrentOp, mCurrentOp.mAccountId);
//                                        ScheduledOperationTable.updateScheduledOp(
//                                                OperationEditor.this,
//                                                mCurrentOp.mScheduledId, op, true);
//                                        ScheduledOperationTable.updateAllOccurences(
//                                                OperationEditor.this, op,
//                                                mPreviousSum,
//                                                mCurrentOp.mScheduledId);
//                                        OperationEditor.this.setResAndExit(true);
//                                    }
//                                })
//                        .setNeutralButton(R.string.disconnect,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,
//                                                        int id) {
//                                        mCurrentOp.mScheduledId = 0;
//                                        OperationEditor.this.setResAndExit(OperationTable
//                                                .updateOp(OperationEditor.this,
//                                                        mRowId, mCurrentOp,
//                                                        mCurrentOp.mAccountId));
//                                    }
//                                })
//                        .setNegativeButton(R.string.cancel,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,
//                                                        int id) {
//                                        dialog.cancel();
//                                    }
//                                });
//                return builder.create();
//            default:
//                return super.onCreateDialog(id);
//        }
//    }

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
        // TODO Auto-generated method stub

    }
}
