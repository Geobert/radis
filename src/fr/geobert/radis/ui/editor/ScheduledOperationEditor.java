package fr.geobert.radis.ui.editor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.service.RadisService;

public class ScheduledOperationEditor extends CommonOpEditor implements OpEditFragmentAccessor {
    // activities ids
    public static final int ACTIVITY_SCH_OP_CREATE = 0;
    public static final int ACTIVITY_SCH_OP_EDIT = 1;

    protected static final int ASK_UPDATE_OCCURENCES_DIALOG_ID = 10;
    private static final int GET_SCH_OP = 620;
    private static final int GET_SCH_OP_SRC = 630;
    private TabListener<OperationEditFragment> mMainEditTab;
    private TabListener<ScheduleEditorFragment> mSchedEditTab;
    private ScheduledOperation mOriginalSchOp;
    private long mOpIdSource;

    public static void callMeForResult(Activity context, final long opId, final long mAccountId, final int mode) {
        Intent i = new Intent(context, ScheduledOperationEditor.class);
        i.putExtra(CommonOpEditor.PARAM_OP_ID, opId);
        i.putExtra(AccountEditor.PARAM_ACCOUNT_ID, mAccountId);
        context.startActivityForResult(i, mode);
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private boolean isInit = false;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            this.mActivity = activity;
            this.mTag = tag;
            this.mClass = clz;
            mFragment = Fragment.instantiate(mActivity, mClass.getName());
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            if (!isInit) {
                // If not, instantiate and add it to the activity
                isInit = true;
                ft.add(R.id.main_content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (isInit) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // do nothing
        }

        public T getFragment() {
            return (T) mFragment;
        }
    }

    @Override
    protected void setView() {
        setContentView(R.layout.scheduled_editor);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mMainEditTab = new TabListener<OperationEditFragment>(this,
                "transaction", OperationEditFragment.class);
        ActionBar.Tab tab = actionBar.newTab().
                setText(R.string.basics)
                .setTabListener(mMainEditTab);
        actionBar.addTab(tab);
        mSchedEditTab = new TabListener<ScheduleEditorFragment>(this, "scheduling", ScheduleEditorFragment.class);
        tab = actionBar.newTab().setText(R.string.scheduling).setTabListener(mSchedEditTab);
        actionBar.addTab(tab);
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

    private void onOkClicked() {
        saveOpAndExit();
    }

    private void onCancelClicked() {
        setResult(CommonOpEditor.RESULT_CANCELED);
        finish();
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
            mSchedEditTab.getFragment().mCurrentSchOp = new ScheduledOperation();
            //populateFields();
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
        mSchedEditTab.getFragment().populateFields();
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
        ScheduledOperation op = mSchedEditTab.getFragment().mCurrentSchOp;
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
        ScheduledOperationTable.updateScheduledOp(this, mRowId, mSchedEditTab.getFragment().mCurrentSchOp, false);
        OperationTable.disconnectAllOccurrences(this, mSchedEditTab.getFragment().mCurrentSchOp.mAccountId, mRowId);
        startInsertionServiceAndExit();
    }

    private void onUpdateAllOccurenceClicked() {
        ScheduledOperationTable.updateScheduledOp(this, mRowId, mSchedEditTab.getFragment().mCurrentSchOp, false);
        if (mSchedEditTab.getFragment().mCurrentSchOp.periodicityEquals(mOriginalSchOp)) {
            ScheduledOperationTable.updateAllOccurences(this, mSchedEditTab.getFragment().mCurrentSchOp,
                    mPreviousSum, mRowId);
            AccountTable.consolidateSums(this, mCurrentOp.mAccountId);
        } else {
            ScheduledOperationTable.deleteAllOccurences(this, mRowId);
        }
        startInsertionServiceAndExit();
    }

    protected boolean isFormValid(StringBuilder errMsg) {
        boolean res = mMainEditTab.getFragment().isFormValid(errMsg);
        if (res) {
            res = mSchedEditTab.getFragment().isFormValid(errMsg);
        }
        return res;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("originalOp", mOriginalSchOp);
        outState.putParcelable("currentSchOp", mSchedEditTab.getFragment().mCurrentSchOp);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mSchedEditTab.getFragment().mCurrentSchOp = savedInstanceState.getParcelable("currentSchOp");
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
        mMainEditTab.getFragment().fillOperationWithInputs(operation);
        mSchedEditTab.getFragment().fillOperationWithInputs(operation);
    }

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
                    mSchedEditTab.getFragment().mCurrentSchOp = new ScheduledOperation(data);
                    mOriginalSchOp = new ScheduledOperation(data);
                } else {
                    onOpNotFound();
                }
                populateFields();
                break;
            case GET_SCH_OP_SRC:
                if (data.getCount() > 0 && data.moveToFirst()) {
                    mSchedEditTab.getFragment().mCurrentSchOp = new ScheduledOperation(data, mCurAccountId);
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
        return mMainEditTab.getFragment().isTransfertChecked();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getSrcAccountSpinnerIdx() {
        return mMainEditTab.getFragment().getSrcAccountIdx();  //To change body of implemented methods use File | Settings | File Templates.
    }
}