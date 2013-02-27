package fr.geobert.radis.ui.editor;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.service.RadisService;

public class ScheduledOperationEditor extends CommonOpEditor implements OpEditFragmentAccessor {
    protected static final int ASK_UPDATE_OCCURENCES_DIALOG_ID = 10;
    private static final int GET_SCH_OP = 620;
    private static final int GET_SCH_OP_SRC = 630;
    private OperationEditFragment mMainEditFragment;
    private ScheduleEditorFragment mSchedEditFragment;
    private ScheduledOperation mOriginalSchOp;
    private long mOpIdSource;

    @Override
    protected void setView() {
        setContentView(R.layout.scheduled_operation_edit);
        mMainEditFragment = (OperationEditFragment) getSupportFragmentManager().findFragmentById(R.id.main_edit_pane);
//        mSchedEditFragment = (ScheduleEditorFragment) getSupportFragmentManager().findFragmentById(R.id.)
    }

    // to be called after setContentView
    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mOpIdSource = extras.getLong("operationId");
    }

    private void onOpNotFound() {
        if (mOpIdSource > 0) {
            getSupportLoaderManager().initLoader(GET_SCH_OP_SRC,
                    getIntent().getExtras(), this);
        } else {
            mSchedEditFragment.mCurrentSchOp = new ScheduledOperation();
            populateFields();
        }
    }

    @Override
    protected void fetchOrCreateCurrentOp() {
        if (mRowId > 0) {
            fetchOp(GET_SCH_OP);
        } else {
            onOpNotFound();
        }
    }

    @Override
    protected void populateFields() {
        mSchedEditFragment.populateFields();
    }

    private void startInsertionServiceAndExit() {
        Log.d("Radis", "startInsertionServiceAndExit");
        RadisService.acquireStaticLock(this);
        this.startService(new Intent(this, RadisService.class));
        Intent res = new Intent();
        if (mOpIdSource > 0) {
            res.putExtra("schOperationId", mRowId);
            res.putExtra("opIdSource", mOpIdSource);
        }
        setResult(RESULT_OK, res);
        finish();
    }

    @Override
    protected void saveOpAndExit() {
        ScheduledOperation op = mSchedEditFragment.mCurrentSchOp;
        if (mRowId <= 0) {
            if (mOpIdSource > 0) { // is converting a transaction into a
                // schedule
                if ((op.getDate() != mOriginalSchOp.getDate())) {
                    // change the date of the source transaction
                    OperationTable.updateOp(this, mOpIdSource, op,
                            op.mAccountId);
                }
                // do not insert another occurrence with same date
                ScheduledOperation.addPeriodicityToDate(op);
            }
            long id = ScheduledOperationTable.createScheduledOp(this, op);
            Log.d("SCHEDULED_OP_EDITOR", "created sch op id : " + id);
            if (id > 0) {
                mRowId = id;
            }
            startInsertionServiceAndExit();
        } else {
            if (!op.equals(mOriginalSchOp)) {
                showDialog(ASK_UPDATE_OCCURENCES_DIALOG_ID);
            } else { // nothing to update
                Intent res = new Intent();
                setResult(RESULT_OK, res);
                finish();
            }
        }
    }

//    @Override
//    protected Dialog onCreateDialog(int id) {
//        switch (id) {
//            case ASK_UPDATE_OCCURENCES_DIALOG_ID:
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setMessage(R.string.ask_update_occurences)
//                        .setCancelable(false)
//                        .setPositiveButton(R.string.update,
//                                new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog,
//                                                        int which) {
//                                        onUpdateAllOccurenceClicked();
//                                    }
//                                })
//                        .setNeutralButton(R.string.disconnect,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,
//                                                        int id) {
//                                        onDisconnectFromOccurences();
//                                    }
//                                })
//                        .setNegativeButton(R.string.cancel,
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,
//                                                        int id) {
//
//                                        dialog.cancel();
//                                    }
//                                });
//                return builder.create();
//            default:
//                return super.onCreateDialog(id);
//        }
//
//    }

    protected void onDisconnectFromOccurences() {
        ScheduledOperationTable.updateScheduledOp(this, mRowId, mSchedEditFragment.mCurrentSchOp, false);
        OperationTable.disconnectAllOccurrences(this, mSchedEditFragment.mCurrentSchOp.mAccountId, mRowId);
        startInsertionServiceAndExit();
    }

    private void onUpdateAllOccurenceClicked() {
        ScheduledOperationTable.updateScheduledOp(this, mRowId, mSchedEditFragment.mCurrentSchOp, false);
        if (mSchedEditFragment.mCurrentSchOp.periodicityEquals(mOriginalSchOp)) {
            ScheduledOperationTable.updateAllOccurences(this, mSchedEditFragment.mCurrentSchOp,
                    mPreviousSum, mRowId);
            AccountTable.consolidateSums(this, mCurrentOp.mAccountId);
        } else {
            ScheduledOperationTable.deleteAllOccurences(this, mRowId);
        }
        startInsertionServiceAndExit();
    }

    protected boolean isFormValid(StringBuilder errMsg) {
        boolean res = mMainEditFragment.isFormValid(errMsg);
        if (res) {
            res = mSchedEditFragment.isFormValid(errMsg);
        }
        return res;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("originalOp", mOriginalSchOp);
        outState.putParcelable("currentSchOp", mSchedEditFragment.mCurrentSchOp);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mSchedEditFragment.mCurrentSchOp = savedInstanceState.getParcelable("currentSchOp");
        mOriginalSchOp = savedInstanceState.getParcelable("originalOp");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
//        if (!mOnBasics) {
//            mViewFlipper.showNext();
//        }
        super.onResume();
    }

    @Override
    protected void fillOperationWithInputs(Operation operation) {
        mMainEditFragment.fillOperationWithInputs(operation);
        mSchedEditFragment.fillOperationWithInputs(operation);
    }

//    private void flip(ViewFlipper flipper, final boolean l2r) {
//        if (l2r) {
//            flipper.setInAnimation(ScheduledOperationEditor.this,
//                    R.anim.enter_from_left);
//            flipper.setOutAnimation(ScheduledOperationEditor.this,
//                    R.anim.exit_by_right);
//            flipper.showPrevious();
//        } else {
//            flipper.setInAnimation(ScheduledOperationEditor.this,
//                    R.anim.enter_from_right);
//            flipper.setOutAnimation(ScheduledOperationEditor.this,
//                    R.anim.exit_by_left);
//            flipper.showNext();
//        }
//        mFlipperScroll.fullScroll(ScrollView.FOCUS_UP);
//        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        mgr.hideSoftInputFromWindow(mOpSumText.getWindowToken(), 0);
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//		Loader<Cursor> l = super.onCreateLoader(id, args);
        CursorLoader l = null;
        if (l == null) {
            switch (id) {
                case GET_SCH_OP:
                    l = new CursorLoader(this,
                            Uri.parse(DbContentProvider.SCHEDULED_JOINED_OP_URI
                                    + "/" + mRowId),
                            ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null,
                            null, null);
                    break;
                case GET_SCH_OP_SRC:
                    l = new CursorLoader(this,
                            Uri.parse(DbContentProvider.OPERATION_JOINED_URI + "/"
                                    + mOpIdSource),
                            OperationTable.OP_COLS_QUERY, null,
                            null, null);

                default:
                    break;
            }
        }
        return l;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//		super.onLoadFinished(loader, data);
        hideProgress();
        switch (loader.getId()) {
            case GET_SCH_OP:
                if (data.getCount() > 0 && data.moveToFirst()) {
                    mSchedEditFragment.mCurrentSchOp = new ScheduledOperation(data);
                    mOriginalSchOp = new ScheduledOperation(data);
                } else {
                    onOpNotFound();
                }
                populateFields();
                break;
            case GET_SCH_OP_SRC:
                if (data.getCount() > 0 && data.moveToFirst()) {
                    mSchedEditFragment.mCurrentSchOp = new ScheduledOperation(data, mCurAccountId);
                    mOriginalSchOp = new ScheduledOperation(data, mCurAccountId);
                } else {
                    onOpNotFound();
                }
                populateFields();
                break;
//		case GET_ALL_ACCOUNTS:
//			populateAccountSpinner(data);
//			break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
//		super.onLoaderReset(arg0);
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isTransfertChecked() {
        return mMainEditFragment.isTransfertChecked();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getSrcAccountSpinnerIdx() {
        return mMainEditFragment.getSrcAccountIdx();  //To change body of implemented methods use File | Settings | File Templates.
    }
}
