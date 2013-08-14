package fr.geobert.radis.ui.editor;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.Operation;

public abstract class CommonOpEditor extends BaseActivity implements
        LoaderCallbacks<Cursor> {
    public static final String PARAM_OP_ID = "op_id";
    protected Operation mCurrentOp;
    protected long mRowId;
    protected boolean mOnRestore = false;
    protected long mPreviousSum = 0L;
    protected Long mCurAccountId;
    Uri mCurrentInfoTable;

    // abstract methods
    protected abstract void setView();

    protected abstract void populateFields();

    protected abstract void fetchOrCreateCurrentOp();

    protected void fetchOp(int loaderId) {
        showProgress();
        getSupportLoaderManager().initLoader(loaderId, null, this);
    }

    // default and common behaviors
    protected void saveOpAndExit() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        mCurAccountId = extras != null ? extras
                .getLong(AccountEditor.PARAM_ACCOUNT_ID) : null;
        init(extras);
        setView();
    }

    protected void init(final Bundle extras) {
        mRowId = extras != null ? extras.getLong(PARAM_OP_ID) : 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mOnRestore) {
            fetchOrCreateCurrentOp();
        } else {
            mOnRestore = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InfoManagerDialog.resetInfoManager();
    }

    protected abstract void fillOperationWithInputs(Operation operation);

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mRowId > 0) {
            outState.putLong(PARAM_OP_ID, mRowId);
        }

        Operation op = mCurrentOp;
        fillOperationWithInputs(op);
        outState.putParcelable("currentOp", op);
        outState.putLong("previousSum", mPreviousSum);
        outState.putParcelable("mCurrentInfoTable", mCurrentInfoTable);
        mOnRestore = true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Cursor allAccount = AccountManager.getInstance().getAllAccountsCursor();
        if (allAccount == null || allAccount.getCount() == 0) {
            AccountManager.getInstance().fetchAllAccounts(this, new Runnable() {
                @Override
                public void run() {
                    populateFields();
                }
            });
        }
        mOnRestore = true;
        long rowId = savedInstanceState.getLong(PARAM_OP_ID);
        mRowId = rowId > 0 ? Long.valueOf(rowId) : 0;
        Operation op = savedInstanceState.getParcelable("currentOp");
        mCurrentOp = op;
        mCurrentInfoTable = (Uri) savedInstanceState.getParcelable("mCurrentInfoTable");
        populateFields();
        mPreviousSum = savedInstanceState.getLong("previousSum");
    }
}
