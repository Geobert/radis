package fr.geobert.radis.tools

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import kotlin.properties.Delegates

public class ProjectionDateController(private val mActivity: Activity) {
    private val mProjectionMode: Spinner by Delegates.lazy { mActivity.findViewById(R.id.projection_date_spinner) as Spinner }
    public val mProjectionDate: EditText by Delegates.lazy { mActivity.findViewById(R.id.projection_date_value) as EditText }
    private var mAccountId: Long = 0
    private var mOrigProjMode: Int = 0
    private var mOrigProjDate: String? = null
    private var mCurPos: Int = 0

    init {
        initViews()
    }

    public fun initViews() {
        val adapter = ArrayAdapter.createFromResource(mActivity, R.array.projection_modes, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        mProjectionMode.setAdapter(adapter)
        mProjectionMode.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, pos: Int, id: Long) {
                mProjectionDate.setVisibility(if (pos > 0) View.VISIBLE else View.GONE)
                if (pos != mCurPos) {
                    mProjectionDate.setText("")
                }
                this@ProjectionDateController.setHint(pos)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
            }
        })
    }

    protected fun setHint(pos: Int) {
        var hint: CharSequence = ""
        when (pos) {
            0 -> hint = ""
            1 -> hint = mActivity.getString(R.string.projection_day_of_month)
            2 -> hint = mActivity.getString(R.string.projection_full_date)
        }
        mProjectionDate.setHint(hint)
    }

    public fun populateFields(account: Account) {
        mAccountId = account.id
        mCurPos = account.projMode
        mOrigProjDate = account.projDate
        setHint(mCurPos)
        mOrigProjMode = mCurPos
        mProjectionMode.setSelection(mCurPos)
        mProjectionDate.setVisibility(if (mCurPos > 0) View.VISIBLE else View.GONE)
        mProjectionDate.setText(mOrigProjDate)
    }

    public fun getMode(): Int {
        return mProjectionMode.getSelectedItemPosition()
    }

    public fun getDate(): String {
        return mProjectionDate.getText().toString()
    }

    public fun hasChanged(): Boolean {
        return mOrigProjMode != mProjectionMode.getSelectedItemPosition() || (mOrigProjMode != 0 && mOrigProjDate != mProjectionDate.getText().toString())
    }

    public fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("projectionMode", mProjectionMode.getSelectedItemPosition())
        outState.putString("projectionDate", mProjectionDate.getText().toString())
        outState.putInt("origProjMode", mOrigProjMode)
        outState.putString("origProjDate", mOrigProjDate)
        outState.putInt("pos", mCurPos)
        outState.putLong("accountId", mAccountId)
    }

    public fun onRestoreInstanceState(state: Bundle) {
        mProjectionMode.setSelection(state.getInt("projectionMode"))
        mProjectionDate.setText(state.getString("projectionDate"))
        mOrigProjDate = state.getString("origProjDate")
        mOrigProjMode = state.getInt("origProjMode")
        mCurPos = state.getInt("pos")
        mAccountId = state.getLong("accountId")
        setHint(mCurPos)
        mProjectionMode.setSelection(mCurPos)
        mProjectionDate.setVisibility(if (mCurPos > 0) View.VISIBLE else View.GONE)
        mProjectionDate.setText(mOrigProjDate)
    }

    //    protected fun saveProjectionDate() {
    //        try {
    //            AccountTable.updateAccountProjectionDate(mActivity, mAccountId, mInstance)
    //            if (mActivity is UpdateDisplayInterface) {
    //                (mActivity as UpdateDisplayInterface).updateDisplay(null)
    //            }
    //        } catch (e: ParseException) {
    //            Tools.popError(mActivity, mActivity.getString(R.string.bad_format_for_date), null)
    //            e.printStackTrace()
    //        } catch (e: NumberFormatException) {
    //            Tools.popError(mActivity, mActivity.getString(R.string.bad_format_for_date), null)
    //            e.printStackTrace()
    //        }
    //
    //    }

    //    companion object {
    //
    //        protected var mInstance: ProjectionDateController by Delegates.notNull()
    //
    //        platformStatic public fun getDialog(activity: Activity): AlertDialog {
    //            val builder = AlertDialog.Builder(activity)
    //            val inflater = activity.getLayoutInflater()
    //            val layout = inflater.inflate(R.layout.projection_date_dialog, null)
    //            builder.setPositiveButton(activity.getString(R.string.ok), object : DialogInterface.OnClickListener {
    //                override fun onClick(dialog: DialogInterface, id: Int) {
    //                    mInstance.saveProjectionDate()
    //                }
    //            }).setNegativeButton(activity.getString(R.string.cancel), object : DialogInterface.OnClickListener {
    //                override fun onClick(dialog: DialogInterface, id: Int) {
    //                    dialog.cancel()
    //                }
    //            }).setTitle(R.string.projection_date)
    //            builder.setView(layout)
    //            val dialog = builder.create()
    //            mInstance = ProjectionDateController(activity)
    //            return dialog
    //        }
    //
    //        platformStatic public fun onPrepareDialog(account: Cursor) {
    //            mInstance.populateFields(account)
    //        }
    //    }
}
