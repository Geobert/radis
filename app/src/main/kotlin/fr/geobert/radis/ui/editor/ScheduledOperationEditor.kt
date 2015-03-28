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
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.service.RadisService
import fr.geobert.radis.tools.Tools
import kotlin.properties.Delegates

public class ScheduledOperationEditor : CommonOpEditor(), OpEditFragmentAccessor {
    private var mOriginalSchOp: ScheduledOperation? = null
    private var mOpIdSource: Long = 0

    private val mViewPager by Delegates.lazy { findViewById(R.id.pager) as ViewPager }

    private val mPagerAdapter = object : FragmentPagerAdapter(getSupportFragmentManager()) {
        private val fragmentsList: Array<Fragment?> = arrayOfNulls(getCount())
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


        fun getOpFragment() = fragmentsList.get(0) as OperationEditFragment
        fun getSchFragment() = fragmentsList.get(1) as ScheduleEditorFragment

    }

    override fun setView() {
        setContentView(R.layout.multipaged_editor)
        mViewPager.setAdapter(mPagerAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.confirm_cancel_menu, menu)
        return true
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.getItemId()) {
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
                fillOperationWithInputs(mPagerAdapter.getSchFragment().mCurrentSchOp as Operation)
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

    private fun onOpNotFound(): Boolean {
        if (mOpIdSource > 0) {
            getSupportLoaderManager().initLoader<Cursor>(GET_SCH_OP_SRC, getIntent().getExtras(), this)
            return false
        } else {
            mPagerAdapter.getSchFragment().mCurrentSchOp = ScheduledOperation(mCurAccountId!!)
            mCurrentOp = mPagerAdapter.getSchFragment().mCurrentSchOp
            populateFields()
            return true
        }
    }

    override fun fetchOrCreateCurrentOp() {
        setTitle(R.string.sch_edition)
        if (mRowId > 0) {
            fetchOp(GET_SCH_OP)
        } else {
            onOpNotFound()
        }
    }

    override fun populateFields() {
        mPagerAdapter.getOpFragment().populateCommonFields(mCurrentOp)
        mPagerAdapter.getOpFragment().setCheckedEditVisibility(View.GONE)
        mPagerAdapter.getSchFragment().populateFields()
    }

    private fun startInsertionServiceAndExit() {
        Log.d("Radis", "startInsertionServiceAndExit")
        RadisService.acquireStaticLock(this)
        this.startService(Intent(this, javaClass<RadisService>()))
        val res = Intent()
        if (mOpIdSource > 0) {
            res.putExtra("schOperationId", mRowId)
            res.putExtra("opIdSource", mOpIdSource)
        }
        setResult(Activity.RESULT_OK, res)
        finish()
    }

    override fun saveOpAndExit() {
        val op = mPagerAdapter.getSchFragment().mCurrentSchOp
        if (op != null) {
            if (mRowId <= 0) {
                if (mOpIdSource > 0) {
                    // is converting a transaction into a
                    // schedule
                    if ((op.getDate() != mOriginalSchOp!!.getDate())) {
                        // change the date of the source transaction
                        OperationTable.updateOp(this, mOpIdSource, op, mOriginalSchOp)
                    }
                    // do not insert another occurrence with same date
                    ScheduledOperation.addPeriodicityToDate(op)
                }
                val id = ScheduledOperationTable.createScheduledOp(this, op)
                Log.d("SCHEDULED_OP_EDITOR", "created sch op id : " + id)
                if (id > 0) {
                    mRowId = id
                }
                startInsertionServiceAndExit()
            } else {
                if (!op.equals(mOriginalSchOp)) {
                    UpdateOccurencesDialog.newInstance().show(getSupportFragmentManager(), "askOnDiff")
                } else {
                    // nothing to update
                    val res = Intent()
                    setResult(Activity.RESULT_OK, res)
                    finish()
                }
            }
        }
    }

    public class UpdateOccurencesDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = getActivity() as ScheduledOperationEditor
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.ask_update_occurrences).setCancelable(false).setPositiveButton(R.string.update, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    context.onUpdateAllOccurenceClicked()
                }
            }).setNeutralButton(R.string.disconnect, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    context.onDisconnectFromOccurences()
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

    protected fun onDisconnectFromOccurences() {
        val schOp = mPagerAdapter.getSchFragment().mCurrentSchOp
        if (schOp != null) {
            ScheduledOperationTable.updateScheduledOp(this, mRowId, schOp, false)
            OperationTable.disconnectAllOccurrences(this, schOp.mAccountId, mRowId)
            startInsertionServiceAndExit()
        }
    }

    private fun onUpdateAllOccurenceClicked() {
        ScheduledOperationTable.updateScheduledOp(this, mRowId, mPagerAdapter.getSchFragment().mCurrentSchOp, false)
        val orig = mOriginalSchOp
        val schOp = mPagerAdapter.getSchFragment().mCurrentSchOp
        if (orig != null && schOp != null) {
            if (schOp.periodicityEquals(orig)) {
                ScheduledOperationTable.updateAllOccurences(this, mPagerAdapter.getSchFragment().mCurrentSchOp, mPreviousSum, mRowId)
                AccountTable.consolidateSums(this, mCurrentOp?.mAccountId as Long)
            } else {
                ScheduledOperationTable.deleteAllOccurences(this, mRowId)
            }
            startInsertionServiceAndExit()
        }
    }

    protected fun isFormValid(errMsg: StringBuilder): Boolean {
        var res = mPagerAdapter.getOpFragment().isFormValid(errMsg)
        if (res) {
            res = mPagerAdapter.getSchFragment().isFormValid(errMsg)
        }
        return res
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("originalOp", mOriginalSchOp)
        outState.putParcelable("currentSchOp", mPagerAdapter.getSchFragment().mCurrentSchOp)
        super<CommonOpEditor>.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mPagerAdapter.getSchFragment().mCurrentSchOp = savedInstanceState.getParcelable<ScheduledOperation>("currentSchOp")
        mOriginalSchOp = savedInstanceState.getParcelable<ScheduledOperation>("originalOp")
        super<CommonOpEditor>.onRestoreInstanceState(savedInstanceState)
    }

    override fun fillOperationWithInputs(operation: Operation) {
        mPagerAdapter.getOpFragment().fillOperationWithInputs(operation)
        mPagerAdapter.getSchFragment().fillOperationWithInputs(operation)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> =
            when (id) {
                GET_SCH_OP -> CursorLoader(this, Uri.parse("${DbContentProvider.SCHEDULED_JOINED_OP_URI}/$mRowId"),
                        ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null, null, null)
                else -> CursorLoader(this, Uri.parse("${ DbContentProvider.OPERATION_JOINED_URI}/$mOpIdSource"), // GET_SCH_OP_SRC
                        OperationTable.OP_COLS_QUERY, null, null, null)
            }


    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        hideProgress()
        when (loader.getId()) {
            GET_SCH_OP -> if (data.getCount() > 0 && data.moveToFirst()) {
                mPagerAdapter.getSchFragment().mCurrentSchOp = ScheduledOperation(data)
                mOriginalSchOp = ScheduledOperation(data)
                mCurrentOp = ScheduledOperation(data)
                populateFields()
            } else {
                if (!onOpNotFound()) {
                    mOpIdSource = 0
                    populateFields()
                }
            }
            GET_SCH_OP_SRC -> if (data.getCount() > 0 && data.moveToFirst()) {
                mCurrentOp = ScheduledOperation(data, mCurAccountId!!)
                mPagerAdapter.getSchFragment().mCurrentSchOp = mCurrentOp as ScheduledOperation
                mOriginalSchOp = ScheduledOperation(data, mCurAccountId!!)
                populateFields()
            } else {
                if (!onOpNotFound()) {
                    mOpIdSource = 0
                    populateFields()
                }
            }
            else -> {
            }
        }
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }

    override fun isTransfertChecked(): Boolean {
        return mPagerAdapter.getOpFragment().isTransfertChecked()
    }

    override fun getSrcAccountSpinnerIdx(): Int {
        return mPagerAdapter.getOpFragment().getSrcAccountIdx()
    }

    companion object {
        // activities ids
        public val ACTIVITY_SCH_OP_CREATE: Int = 3000
        public val ACTIVITY_SCH_OP_EDIT: Int = 3001
        public val ACTIVITY_SCH_OP_CONVERT: Int = 3002
        public val PARAM_SRC_OP_TO_CONVERT: String = "sourceOpId"

        protected val ASK_UPDATE_OCCURENCES_DIALOG_ID: Int = 10
        private val GET_SCH_OP = 620
        private val GET_SCH_OP_SRC = 630

        public fun callMeForResult(context: Activity, opId: Long, mAccountId: Long, mode: Int) {
            val i = Intent(context, javaClass<ScheduledOperationEditor>())
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
