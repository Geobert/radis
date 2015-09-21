package fr.geobert.radis.ui.editor

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.Tools
import java.util.*

public class ScheduledOperationEditor : CommonOpEditor(), OpEditFragmentAccessor {
    private var mOriginalSchOp: ScheduledOperation? = null
    private var mOpIdSource: Long = 0

    private val mViewPager by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.pager) as ViewPager }

    private val mPagerAdapter = object : FragmentStatePagerAdapter(supportFragmentManager) {
        private val fragmentsList: Array<Fragment?> = arrayOfNulls(count)
        override fun getItem(position: Int): Fragment? {
            val f = fragmentsList.get(position)
            return if (null == f) {
                fragmentsList.set(position, when (position) {
                    0 -> OperationEditFragment()
                    else -> ScheduleEditorFragment()
                })
                fragmentsList.get(position)
            } else {
                f
            }
        }

        override fun getCount(): Int = 2

        override fun getPageTitle(position: Int): CharSequence =
                when (position) {
                    0 -> getString(R.string.basics)
                    else -> getString(R.string.scheduling)
                }
    }

    fun getOpFragment() = mPagerAdapter.getItem(0) as OperationEditFragment
    fun getSchFragment() = mPagerAdapter.getItem(1) as ScheduleEditorFragment

    override fun setView() {
        setContentView(R.layout.multipane_editor)
        mViewPager.adapter = mPagerAdapter
    }

    //    fun onAllAccountsFetched() {
    //        mAccountManager.setCurrentAccountId(mCurAccountId, this) // trigger config fetch
    //        //getOpFragment().onAllAccountFetched()
    //        //super<CommonOpEditor>.onAllAccountsFetched()
    //    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.confirm_cancel_menu, menu)
        return true
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.confirm -> {
                onOkClicked()
                return true
            }
            else -> return super<CommonOpEditor>.onOptionsItemSelected(item)
        }
    }

    private fun onOkClicked() {
        if (mRowId <= 0 || mOriginalSchOp != null) {
            val errMsg = StringBuilder()
            if (isFormValid(errMsg)) {
                fillOperationWithInputs(getSchFragment().mCurrentSchOp as Operation)
                saveOpAndExit()
            } else {
                Tools.popError(this, errMsg.toString(), null)
            }
        }
    }

    // to be called after setContentView
    //    fun init(savedInstanceState: Bundle) {
    //        val extras = getIntent().getExtras()
    //        super<CommonOpEditor>.init(extras)
    //        mOpIdSource = extras.getLong(PARAM_SRC_OP_TO_CONVERT)
    //    }

    private fun onOpNotFound(cbk: (Operation) -> Unit): Boolean {
        if (mOpIdSource > 0) {
            onOpFetchedCbks.add(cbk)
            supportLoaderManager.initLoader<Cursor>(GET_SCH_OP_SRC, intent.extras, this)
            return false
        } else {
            val op = ScheduledOperation(mCurAccountId)
            getSchFragment().mCurrentSchOp = op
            mCurrentOp = op
            cbk(op)
            return true
        }
    }

    override fun fetchOrCreateCurrentOp(cbk: (Operation) -> Unit) {
        setTitle(R.string.sch_edition)
        if (mRowId > 0) {
            onOpFetchedCbks.add(cbk)
            fetchOp(GET_SCH_OP)
        } else {
            onOpNotFound(cbk)
        }
    }

    //    override fun populateFields() {
    //        // TODO do not populate from Activity, let the fragments independently ask for an op then populate themselves
    //        getOpFragment().populateCommonFields(mCurrentOp!!)
    //        getOpFragment().setCheckedEditVisibility(View.GONE)
    //        getSchFragment().populateFields()
    //    }

    private fun startInsertionServiceAndExit() {
        Log.d("Radis", "startInsertionServiceAndExit")
        fr.geobert.radis.service.RadisService.acquireStaticLock(this)
        this.startService(Intent(this, fr.geobert.radis.service.RadisService::class.java))
        val res = Intent()
        getSchFragment().mCurrentSchOp!!.mRowId = mRowId
        res.putExtra("operation", getSchFragment().mCurrentSchOp!!)
        if (mOpIdSource > 0) {
            res.putExtra("opIdSource", mOpIdSource)
        }
        setResult(Activity.RESULT_OK, res)
        finish()
    }

    override fun saveOpAndExit() {
        val op = getSchFragment().mCurrentSchOp
        val origOp = mOriginalSchOp
        if (op != null) {
            if (mRowId <= 0) {
                if (mOpIdSource > 0 && origOp != null) {
                    // is converting a transaction into a schedule
                    if ((op.getDate() != origOp.getDate())) {
                        // change the date of the source transaction
                        OperationTable.updateOp(this, mOpIdSource, op, origOp)
                    }
                    // do not insert another occurrence with same date
                    ScheduledOperation.addPeriodicityToDate(op)
                }
                val id = ScheduledOperationTable.createScheduledOp(this, op)
                if (id > 0) {
                    mRowId = id
                }
                startInsertionServiceAndExit()
            } else {
                if (!op.equals(origOp)) {
                    UpdateOccurencesDialog.newInstance().show(supportFragmentManager, "askOnDiff")
                } else {
                    // nothing to update
                    val res = Intent()
                    op.mRowId = mRowId
                    res.putExtra("operation", op)
                    setResult(Activity.RESULT_OK, res)
                    finish()
                }
            }
        }
    }

    public class UpdateOccurencesDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = activity as ScheduledOperationEditor
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.ask_update_occurrences).setCancelable(false).setPositiveButton(R.string.update, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    context.onUpdateAllOccurrenceClicked()
                }
            }).setNeutralButton(R.string.disconnect, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    context.onDisconnectFromOccurrences()
                }
            }).setNegativeButton(R.string.cancel, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    dialog.cancel()
                }
            })
            return builder.create()
        }

        companion object {
            public fun newInstance(): UpdateOccurencesDialog {
                val frag = UpdateOccurencesDialog()
                //            Bundle args = new Bundle();
                //            args.putLong("accountId", accountId);
                //            frag.setArguments(args);
                return frag
            }
        }
    }

    protected fun onDisconnectFromOccurrences() {
        val schOp = getSchFragment().mCurrentSchOp
        if (schOp != null) {
            ScheduledOperationTable.updateScheduledOp(this, mRowId, schOp, false)
            OperationTable.disconnectAllOccurrences(this, schOp.mAccountId, mRowId)
            startInsertionServiceAndExit()
        }
    }

    private fun onUpdateAllOccurrenceClicked() {
        val schOp = getSchFragment().mCurrentSchOp
        if (schOp != null) {
            ScheduledOperationTable.updateScheduledOp(this, mRowId, schOp, false)
            val orig = mOriginalSchOp
            if (orig != null) {
                if (schOp.periodicityEquals(orig)) {
                    ScheduledOperationTable.updateAllOccurences(this, schOp, mPreviousSum, mRowId)
                    AccountTable.consolidateSums(this, mCurrentOp!!.mAccountId)
                } else {
                    ScheduledOperationTable.deleteAllOccurences(this, mRowId)
                }
                startInsertionServiceAndExit()
            }
        }
    }

    protected fun isFormValid(errMsg: StringBuilder): Boolean {
        var res = getOpFragment().isFormValid(errMsg)
        if (res) {
            res = getSchFragment().isFormValid(errMsg)
        }
        return res
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("originalOp", mOriginalSchOp)
        outState.putParcelable("currentSchOp", getSchFragment().mCurrentSchOp)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        getSchFragment().mCurrentSchOp = savedInstanceState.getParcelable<ScheduledOperation>("currentSchOp")
        mOriginalSchOp = savedInstanceState.getParcelable<ScheduledOperation>("originalOp")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun fillOperationWithInputs(operation: Operation) {
        getOpFragment().fillOperationWithInputs(operation)
        getSchFragment().fillOperationWithInputs(operation)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> =
            when (id) {
                GET_SCH_OP -> CursorLoader(this, Uri.parse("${DbContentProvider.SCHEDULED_JOINED_OP_URI}/$mRowId"),
                        ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null, null, null)
                else -> CursorLoader(this, Uri.parse("${ DbContentProvider.OPERATION_JOINED_URI}/$mOpIdSource"), // GET_SCH_OP_SRC
                        OperationTable.OP_COLS_QUERY, null, null, null)
            }


    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.id) {
            GET_SCH_OP -> if (data.count > 0 && data.moveToFirst()) {
                mOriginalSchOp = ScheduledOperation(data)
                mCurrentOp = ScheduledOperation(data)
                val op = ScheduledOperation(data)
                onOpFetchedCbks.forEach { it(op) }
            } else {
                val cbks = LinkedList<(Operation) -> Unit>(onOpFetchedCbks)
                onOpFetchedCbks.clear()
                cbks.forEach {
                    if (!onOpNotFound(it)) {
                        mOpIdSource = 0
                        //                        populateFields()
                    }
                }
            }
            GET_SCH_OP_SRC -> if (data.count > 0 && data.moveToFirst()) {
                val op = ScheduledOperation(data, mCurAccountId)
                mCurrentOp = op
                getSchFragment().mCurrentSchOp = mCurrentOp as ScheduledOperation
                mOriginalSchOp = ScheduledOperation(data, mCurAccountId)
                onOpFetchedCbks.forEach { it(op) }
            } else {
                val cbks = LinkedList<(Operation) -> Unit>(onOpFetchedCbks)
                onOpFetchedCbks.clear()
                cbks.forEach {
                    if (!onOpNotFound(it)) {
                        mOpIdSource = 0
                        //                        populateFields()
                    }
                }
            }
            else -> {
            }
        }
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }

    override fun isTransfertChecked(): Boolean {
        return getOpFragment().isTransfertChecked()
    }

    override fun getSrcAccountSpinnerIdx(): Int {
        return getOpFragment().getSrcAccountIdx()
    }

    companion object {
        // activities ids
        public val ACTIVITY_SCH_OP_CREATE: Int = 3000
        public val ACTIVITY_SCH_OP_EDIT: Int = 3001
        public val ACTIVITY_SCH_OP_CONVERT: Int = 3002
        public val PARAM_SRC_OP_TO_CONVERT: String = "sourceOpId"

        //        protected val ASK_UPDATE_OCCURENCES_DIALOG_ID: Int = 10
        private val GET_SCH_OP = 620
        private val GET_SCH_OP_SRC = 630

        public fun callMeForResult(context: Activity, opId: Long, mAccountId: Long, mode: Int) {
            val i = Intent(context, ScheduledOperationEditor::class.java)
            if (mode == ACTIVITY_SCH_OP_CONVERT) {
                i.putExtra(PARAM_SRC_OP_TO_CONVERT, opId)
            } else {
                i.putExtra(CommonOpEditor.PARAM_OP_ID, opId)
            }
            i.putExtra(AccountEditor.PARAM_ACCOUNT_ID, mAccountId)
            context.startActivityForResult(i, mode)
        }
    }
}
