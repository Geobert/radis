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
    protected static final int THIRD_PARTIES_DIALOG_ID = 1;
    protected static final int TAGS_DIALOG_ID = 2;
    protected static final int MODES_DIALOG_ID = 3;
    protected static final int EDIT_THIRD_PARTY_DIALOG_ID = 4;
    protected static final int EDIT_TAG_DIALOG_ID = 5;
    protected static final int EDIT_MODE_DIALOG_ID = 6;
    protected static final int DELETE_THIRD_PARTY_DIALOG_ID = 7;
    protected static final int DELETE_TAG_DIALOG_ID = 8;
    protected static final int DELETE_MODE_DIALOG_ID = 9;
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

//    @Override
//    protected Dialog onCreateDialog(int id) {
//        switch (id) {
//            case THIRD_PARTIES_DIALOG_ID:
//                return createInfoManagerIfNeeded(DbContentProvider.THIRD_PARTY_URI,
//                        InfoTables.KEY_THIRD_PARTY_NAME,
//                        getString(R.string.third_parties),
//                        EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID)
//                        .getListDialog();
//            case TAGS_DIALOG_ID:
//                return createInfoManagerIfNeeded(DbContentProvider.TAGS_URI,
//                        InfoTables.KEY_TAG_NAME, getString(R.string.tags),
//                        EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID).getListDialog();
//            case MODES_DIALOG_ID:
//                return createInfoManagerIfNeeded(DbContentProvider.MODES_URI,
//                        InfoTables.KEY_MODE_NAME, getString(R.string.modes),
//                        EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID).getListDialog();
//            case EDIT_THIRD_PARTY_DIALOG_ID: {
//                InfoManager i = createInfoManagerIfNeeded(
//                        DbContentProvider.THIRD_PARTY_URI,
//                        InfoTables.KEY_THIRD_PARTY_NAME,
//                        getString(R.string.third_parties),
//                        EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID);
//                Dialog d = i.getEditDialog();
//                i.initEditDialog(d);
//                return d;
//            }
//            case EDIT_TAG_DIALOG_ID: {
//                InfoManager i = createInfoManagerIfNeeded(
//                        DbContentProvider.TAGS_URI, InfoTables.KEY_TAG_NAME,
//                        getString(R.string.tags), EDIT_TAG_DIALOG_ID,
//                        DELETE_TAG_DIALOG_ID);
//                Dialog d = i.getEditDialog();
//                i.initEditDialog(d);
//                return d;
//            }
//            case EDIT_MODE_DIALOG_ID: {
//                InfoManager i = createInfoManagerIfNeeded(
//                        DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME,
//                        getString(R.string.modes), EDIT_MODE_DIALOG_ID,
//                        DELETE_MODE_DIALOG_ID);
//                Dialog d = i.getEditDialog();
//                i.initEditDialog(d);
//                return d;
//            }
//            case DELETE_THIRD_PARTY_DIALOG_ID:
//                return Tools.createDeleteConfirmationDialog(this,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                InfoManager i = createInfoManagerIfNeeded(
//                                        DbContentProvider.THIRD_PARTY_URI,
//                                        InfoTables.KEY_THIRD_PARTY_NAME,
//                                        getString(R.string.third_parties),
//                                        EDIT_THIRD_PARTY_DIALOG_ID,
//                                        DELETE_THIRD_PARTY_DIALOG_ID);
//                                i.deleteInfo();
//                            }
//                        });
//            case DELETE_TAG_DIALOG_ID:
//                return Tools.createDeleteConfirmationDialog(this,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                InfoManager i = createInfoManagerIfNeeded(
//                                        DbContentProvider.TAGS_URI,
//                                        InfoTables.KEY_TAG_NAME,
//                                        getString(R.string.tags),
//                                        EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID);
//                                i.deleteInfo();
//                            }
//                        });
//            case DELETE_MODE_DIALOG_ID:
//                return Tools.createDeleteConfirmationDialog(this,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                InfoManager i = createInfoManagerIfNeeded(
//                                        DbContentProvider.MODES_URI,
//                                        InfoTables.KEY_MODE_NAME,
//                                        getString(R.string.modes),
//                                        EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID);
//                                i.deleteInfo();
//                            }
//                        });
//            default:
//                return Tools.onDefaultCreateDialog(this, id);
//        }
//    }

//    @Override
//    protected void onPrepareDialog(int id, Dialog dialog) {
//        switch (id) {
//            case EDIT_THIRD_PARTY_DIALOG_ID:
//            case EDIT_TAG_DIALOG_ID:
//            case EDIT_MODE_DIALOG_ID:
//                mInfoManagersMap.get(mCurrentInfoTable.toString()).initEditDialog(dialog);
//                break;
//            case THIRD_PARTIES_DIALOG_ID:
//                mInfoManagersMap.get(DbContentProvider.THIRD_PARTY_URI.toString())
//                        .onPrepareDialog((AlertDialog) dialog);
//                break;
//            case TAGS_DIALOG_ID:
//                mInfoManagersMap.get(DbContentProvider.TAGS_URI.toString())
//                        .onPrepareDialog((AlertDialog) dialog);
//                break;
//            case MODES_DIALOG_ID:
//                mInfoManagersMap.get(DbContentProvider.MODES_URI.toString())
//                        .onPrepareDialog((AlertDialog) dialog);
//                break;
//        }
//    }

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
