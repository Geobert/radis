package fr.geobert.radis.ui.editor

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v4.widget.CursorAdapter
import android.support.v4.widget.SimpleCursorAdapter
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.tools.Tools
import java.util.*

public class InfoManager(private val mDiagFragment: DialogFragment, title: String, table: Uri,
                         colName: String, private val mEditId: Int, private val mDeleteId: Int) : LoaderCallbacks<Cursor> {
    private var mContext: BaseActivity? = null
    private var mBuilder: AlertDialog.Builder
    private var mListDialog: AlertDialog? = null
    private val mAddBut: Button
    private val mDelBut: Button
    private val mEditBut: Button
    private var mSelectedInfo = -1
    private var mCursor: Cursor? = null
    private val mInfo: Bundle
    private var mEditorText: EditText? = null
    private var mOkBut: Button? = null
    private val mInfoText: AutoCompleteTextView
    private var mOldValue: String? = null
    private val mAdapter: SimpleCursorAdapter
    private val GET_MATCHING_INFO_ID: Int
    private var mEditDialog: AlertDialog? = null

    init {
        val ctx = mDiagFragment.activity as BaseActivity
        mContext = ctx
        GET_MATCHING_INFO_ID = EDITTEXT_OF_INFO[table.toString()] as Int
        mAdapter = object : SimpleCursorAdapter(ctx,
                android.R.layout.simple_list_item_single_choice, null,
                arrayOf(colName), intArrayOf(android.R.id.text1),
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position,
                        convertView, parent) as TextView
                if (Build.VERSION.SDK_INT < 11) {
                    textView.setTextColor(ContextCompat.getColor(mContext,
                            android.R.color.black))
                }
                return textView
            }

        }
        mInfo = Bundle()
        mInfo.putString("title", title)
        mInfo.putParcelable("table", table)
        mInfo.putString("colName", colName)
        val builder = AlertDialog.Builder(ctx)
        builder.setSingleChoiceItems(mAdapter, -1, { dialog, item ->
            mSelectedInfo = item
            refreshToolbarStatus()
        })
        builder.setTitle(title)
        val inflater = ctx.layoutInflater
        val layout = inflater.inflate(R.layout.info_list, null, false)
        builder.setView(layout)
        mBuilder = builder
        ctx.supportLoaderManager.initLoader(GET_MATCHING_INFO_ID, mInfo,
                this)

        builder.setPositiveButton(ctx.getString(R.string.ok), { dialog, id ->
            infoSelected()
            ctx.supportLoaderManager.destroyLoader(GET_MATCHING_INFO_ID)
            dialog.cancel()
            mDiagFragment.dismiss()
            mSelectedInfo = -1
        }).setNegativeButton(ctx.getString(R.string.cancel), { dialog, id ->
            ctx.supportLoaderManager.destroyLoader(GET_MATCHING_INFO_ID)
            dialog.cancel()
            mDiagFragment.dismiss()
            mSelectedInfo = -1
        })

        mAddBut = layout.findViewById(R.id.create_info) as Button
        mDelBut = layout.findViewById(R.id.del_info) as Button
        mEditBut = layout.findViewById(R.id.edit_info) as Button
        mInfoText = ctx.findViewById(EDITTEXT_OF_INFO[table.toString()] as Int) as AutoCompleteTextView

        mDelBut.setOnClickListener({ onDeleteClicked() })

        mAddBut.setOnClickListener({ onAddClicked() })

        mEditBut.setOnClickListener({ onEditClicked() })
    }

    protected fun infoSelected() {
        val lv = mListDialog!!.listView
        val c = mCursor
        if (c != null && lv != null && c.moveToPosition(lv.checkedItemPosition)) {
            Tools.setTextWithoutComplete(mInfoText, c.getString(c.getColumnIndex(mInfo.getString("colName"))))
        }
    }

    public fun getListDialog(): AlertDialog {
        if (mListDialog == null) {
            mListDialog = mBuilder.create()
        } else {
            mContext!!.supportLoaderManager.initLoader(GET_MATCHING_INFO_ID, mInfo, this)
        }
        return mListDialog as AlertDialog
    }

    public fun onPrepareDialog(dialog: AlertDialog) {
        mOkBut = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        refreshToolbarStatus()
    }

    public fun refreshToolbarStatus() {
        val oneSelected = (mSelectedInfo > -1)
        //                && (mSelectedInfo < dialog.getListView().getCount());
        mDelBut.isEnabled = oneSelected
        mEditBut.isEnabled = oneSelected
        mOkBut?.isEnabled = oneSelected
    }

    private fun onDeleteClicked() {
        // TODO
        //        mContext.mCurrentInfoTable = (Uri) mInfo.getParcelable("table");
        val ctx = mContext
        if (ctx != null)
            InfoManagerDialog.createInfoDeleteDialog(mDeleteId, ctx)?.show(ctx.supportFragmentManager, "dialog")
    }

    public fun deleteInfo() {
        val c = mCursor
        val ctx = mContext
        if (c != null && ctx != null) {
            c.moveToPosition(mSelectedInfo)
            InfoTables.deleteInfo(ctx, mInfo.getParcelable<Parcelable>("table") as Uri,
                    c.getLong(c.getColumnIndex("_id")))
            mSelectedInfo = -1
            refresh()
            refreshToolbarStatus()
        }
    }

    private fun onAddClicked() {
        val info = mInfo
        info.remove("value")
        info.remove("rowId")
        // TODO
        //        mContext.mCurrentInfoTable = (Uri) info.getParcelable("table");
        val ctx = mContext
        if (ctx != null)
            InfoManagerDialog.createInfoEditDialog(mEditId, ctx)?.show(ctx.supportFragmentManager, "dialog")
    }

    private fun onEditClicked() {
        val lv = mListDialog!!.listView
        val c = mCursor
        val ctx = mContext
        if (c != null && c.count > 0 && ctx != null) {
            c.moveToPosition(lv.checkedItemPosition)
            val info = mInfo

            info.putString("value", c.getString(c.getColumnIndex(info.getString("colName"))))
            info.putLong("rowId", c.getLong(c.getColumnIndex("_id")))
            // TODO
            //        mContext.mCurrentInfoTable = (Uri) info.getParcelable("table");
            InfoManagerDialog.createInfoEditDialog(mEditId, ctx)?.show(ctx.supportFragmentManager, "dialog")
        }
    }

    public fun initEditDialog(dialog: Dialog) {
        val t = mEditorText
        if (t != null) {
            val info = mInfo
            // dialog.setTitle(info.getString("title"));
            val tmp = info.getString("value")
            mOldValue = tmp
            t.setText(tmp)

            t.onFocusChangeListener = object : View.OnFocusChangeListener {
                override fun onFocusChange(v: View, hasFocus: Boolean) {
                    if (hasFocus) {
                        dialog.window.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                    }
                }
            }
        }
    }

    public fun getEditDialog(): Dialog {
        val context = mContext
        if (mEditDialog == null && context != null) {
            val builder = AlertDialog.Builder(context)
            val inflater = context.layoutInflater
            val layout = inflater.inflate(R.layout.info_edit, null, false)
            builder.setPositiveButton(context.getString(R.string.ok),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            saveText(context)
                        }
                    }).setNegativeButton(context.getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, id: Int) {
                            dialog.cancel()
                        }
                    })
            val info = mInfo
            mEditorText = layout.findViewById(R.id.info_edit_text) as EditText
            builder.setTitle(info.getString("title"))
            builder.setView(layout)
            mEditDialog = builder.create()
        }
        return mEditDialog as Dialog
    }

    private fun saveText(ctx: Context) {
        val value = mEditorText?.text.toString().trim()
        val rowId = mInfo.getLong("rowId")
        if (rowId != 0L) {
            // update
            InfoTables.updateInfo(ctx, mInfo.getParcelable<Parcelable>("table") as Uri,
                    rowId, value, null)
        } else {
            // create
            val id = InfoTables.getKeyIdIfExistsOrCreate(ctx, value,
                    mInfo.getParcelable<Parcelable>("table") as Uri)
            if (id > 0) {
                // already existing value, update
                Tools.popError(ctx, ctx.getString(R.string.item_exists), null)
            }
        }
        refresh()
    }

    private fun refresh() {
        mContext!!.supportLoaderManager.restartLoader(GET_MATCHING_INFO_ID, mInfo, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        return InfoTables.getMatchingInfoLoader(mContext!!,
                args.getParcelable<Parcelable>("table") as Uri, args.getString("colName"),
                null)
    }

    override fun onLoadFinished(arg0: Loader<Cursor>, data: Cursor) {
        mAdapter.changeCursor(data)
        mCursor = data
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
        if (mCursor != null) {
            mCursor!!.close()
            mCursor = null
        }
    }

    companion object {
        @SuppressWarnings("serial")
        private val EDITTEXT_OF_INFO = object : HashMap<String, Int>() {
            init {
                put(DbContentProvider.THIRD_PARTY_URI.toString(),
                        R.id.edit_op_third_party)
                put(DbContentProvider.TAGS_URI.toString(), R.id.edit_op_tag)
                put(DbContentProvider.MODES_URI.toString(), R.id.edit_op_mode)
            }
        }
    }
}
