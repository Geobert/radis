package fr.geobert.radis.ui.editor;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.view.KeyEvent;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.tools.Tools;

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
        Bundle extras = getIntent().getExtras();

        mCurAccountId = extras != null ? extras
                .getLong(AccountEditor.PARAM_ACCOUNT_ID) : null;
        init(savedInstanceState);
        setView();
    }

    protected void init(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
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
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (Tools.onKeyLongPress(keyCode, event, this)) {
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
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