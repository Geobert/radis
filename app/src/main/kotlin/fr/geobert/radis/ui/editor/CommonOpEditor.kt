package fr.geobert.radis.ui.editor

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.data.Operation
import kotlin.properties.Delegates

public abstract class CommonOpEditor : BaseActivity(), LoaderCallbacks<Cursor>, EditorToolbarTrait {
    var mCurrentOp: Operation? = null
    protected var mRowId: Long = 0
    protected var mOnRestore: Boolean = false
    var mPreviousSum: Long = 0
    var mCurAccountId: Long by Delegates.notNull()
    var mCurrentInfoTable: Uri? = null

    // abstract methods
    protected abstract fun setView()

    protected abstract fun populateFields()

    protected abstract fun fetchOrCreateCurrentOp()

    open protected fun onAllAccountsFetched() {
        if (!mOnRestore) {
            fetchOrCreateCurrentOp()
        } else {
            populateFields()
            mOnRestore = false
        }
    }

    override fun onResumeFragments() {
        super<BaseActivity>.onResumeFragments()
        mAccountManager.fetchAllAccounts(false, {
            onAllAccountsFetched()
        })
    }

    protected fun fetchOp(loaderId: Int) {
        showProgress()
        getSupportLoaderManager().initLoader<Cursor>(loaderId, Bundle(), this)
    }

    // default and common behaviors
    protected open fun saveOpAndExit() {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        val extras = getIntent().getExtras()
        mCurAccountId = extras?.getLong(AccountEditor.PARAM_ACCOUNT_ID) ?: 0
        init(extras)
        setView()
        initToolbar(this)
    }

    protected open fun init(extras: Bundle?) {
        mRowId = if (extras != null) extras.getLong(PARAM_OP_ID) else 0
    }

    override fun onDestroy() {
        super<BaseActivity>.onDestroy()
        InfoManagerDialog.resetInfoManager()
    }

    protected abstract fun fillOperationWithInputs(operation: Operation)

    override fun onSaveInstanceState(outState: Bundle) {
        if (mRowId > 0) {
            outState.putLong(PARAM_OP_ID, mRowId)
        }

        val op = mCurrentOp
        if (op != null) {
            fillOperationWithInputs(op)
            outState.putParcelable("currentOp", op)
        }
        outState.putLong("previousSum", mPreviousSum)
        outState.putParcelable("mCurrentInfoTable", mCurrentInfoTable)
        mOnRestore = true
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mOnRestore = true
        val rowId = savedInstanceState.getLong(PARAM_OP_ID)
        mRowId = if (rowId > 0) java.lang.Long.valueOf(rowId) else 0
        val op = savedInstanceState.getParcelable<Operation>("currentOp")
        mCurrentOp = op
        mCurrentInfoTable = savedInstanceState.getParcelable<Uri>("mCurrentInfoTable")
        //        populateFields();
        mPreviousSum = savedInstanceState.getLong("previousSum")
    }

    companion object {
        public val PARAM_OP_ID: String = "op_id"
    }
}
