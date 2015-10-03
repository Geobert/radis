package fr.geobert.radis.ui.editor

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import fr.geobert.radis.R
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.tools.Tools
import java.util.*

public class InfoManagerDialog : DialogFragment() {
    private var mInfoManager: InfoManager? = null
    private var mMode = -1

    private fun createInfoManagerIfNeeded(table: Uri, colName: String, title: String, editId: Int, deleteId: Int):
            InfoManager {
        var i: InfoManager? = mInfoManagersMap.get(table.toString())
        if (null == i) {
            i = InfoManager(this, title, table, colName, editId, deleteId)
            mInfoManagersMap.put(table.toString(), i)
        }
        return i
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments
        val table = args.getParcelable<Uri>("table")
        val colName = args.getString("colName")
        val title = args.getString("title")
        val editId = args.getInt("editId")
        val deleteId = args.getInt("deleteId")
        val mode = args.getInt("mode")
        mMode = mode
        val infoManager = createInfoManagerIfNeeded(table, colName, title, editId, deleteId)
        mInfoManager = infoManager
        when (mode) {
            THIRD_PARTIES_DIALOG_ID, TAGS_DIALOG_ID, MODES_DIALOG_ID -> return infoManager.getListDialog()
            EDIT_MODE_DIALOG_ID, EDIT_TAG_DIALOG_ID, EDIT_THIRD_PARTY_DIALOG_ID -> return infoManager.getEditDialog()
            DELETE_MODE_DIALOG_ID, DELETE_TAG_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID ->
                return Tools.createDeleteConfirmationDialog(activity, { d, i ->
                    createInfoManagerIfNeeded(table, colName, title, editId, deleteId).deleteInfo()
                })
            else -> // should not happen
                throw RuntimeException("Unknown dialog:$mode")
        }
    }

    override fun onResume() {
        super.onResume()
        val d = dialog
        when (mMode) {
            THIRD_PARTIES_DIALOG_ID, TAGS_DIALOG_ID, MODES_DIALOG_ID -> mInfoManager!!.onPrepareDialog(d as AlertDialog)
            EDIT_MODE_DIALOG_ID, EDIT_TAG_DIALOG_ID, EDIT_THIRD_PARTY_DIALOG_ID -> mInfoManager!!.initEditDialog(d)
            DELETE_MODE_DIALOG_ID, DELETE_TAG_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID -> {
            }
        }// should not happen
    }

    companion object {
        public val THIRD_PARTIES_DIALOG_ID: Int = 1
        public val TAGS_DIALOG_ID: Int = 2
        public val MODES_DIALOG_ID: Int = 3
        public val EDIT_THIRD_PARTY_DIALOG_ID: Int = 4
        public val EDIT_TAG_DIALOG_ID: Int = 5
        public val EDIT_MODE_DIALOG_ID: Int = 6
        public val DELETE_THIRD_PARTY_DIALOG_ID: Int = 7
        public val DELETE_TAG_DIALOG_ID: Int = 8
        public val DELETE_MODE_DIALOG_ID: Int = 9
        private val mInfoManagersMap = HashMap<String, InfoManager>()
        private val mInfoManagerDialogMap = HashMap<String, InfoManagerDialog>()

        public fun resetInfoManager() {
            mInfoManagerDialogMap.clear()
            mInfoManagersMap.clear()
        }

        private fun newInstance(table: Uri, colName: String, title: String, editId: Int, deleteId: Int, mode: Int):
                InfoManagerDialog {
            val frag = InfoManagerDialog()
            val args = Bundle()
            args.putParcelable("table", table)
            args.putString("colName", colName)
            args.putString("title", title)
            args.putInt("editId", editId)
            args.putInt("deleteId", deleteId)
            args.putInt("mode", mode)
            frag.arguments = args
            return frag
        }

        public fun createThirdPartiesListDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.THIRD_PARTY_URI.toString() + "_list"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.THIRD_PARTY_URI, InfoTables.KEY_THIRD_PARTY_NAME,
                        ctx.getString(R.string.third_parties), EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID,
                        THIRD_PARTIES_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        public fun createTagsListDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.TAGS_URI.toString() + "_list"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.TAGS_URI, InfoTables.KEY_TAG_NAME,
                        ctx.getString(R.string.tags), EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID, TAGS_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        public fun createModesListDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.MODES_URI.toString() + "_list"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME,
                        ctx.getString(R.string.modes), EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID, MODES_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        protected fun createThirdPartiesDeleteDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.THIRD_PARTY_URI.toString() + "_del"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.THIRD_PARTY_URI, InfoTables.KEY_THIRD_PARTY_NAME,
                        ctx.getString(R.string.third_parties), EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID,
                        DELETE_THIRD_PARTY_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        protected fun createTagsDeleteDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.TAGS_URI.toString() + "_del"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.TAGS_URI, InfoTables.KEY_TAG_NAME,
                        ctx.getString(R.string.tags), EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        protected fun createModesDeleteDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.MODES_URI.toString() + "_del"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME,
                        ctx.getString(R.string.modes), EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID,
                        DELETE_MODE_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        public fun createInfoDeleteDialog(deleteId: Int, ctx: Context): InfoManagerDialog? {
            when (deleteId) {
                DELETE_THIRD_PARTY_DIALOG_ID -> return createThirdPartiesDeleteDialog(ctx)
                DELETE_TAG_DIALOG_ID -> return createTagsDeleteDialog(ctx)
                DELETE_MODE_DIALOG_ID -> return createModesDeleteDialog(ctx)
                else -> return null
            }
        }

        public fun createInfoEditDialog(editId: Int, ctx: Context): InfoManagerDialog? {
            when (editId) {
                EDIT_THIRD_PARTY_DIALOG_ID -> return createThirdPartiesEditDialog(ctx)
                EDIT_TAG_DIALOG_ID -> return createTagsEditDialog(ctx)
                EDIT_MODE_DIALOG_ID -> return createModesEditDialog(ctx)
                else -> return null
            }
        }

        protected fun createThirdPartiesEditDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.THIRD_PARTY_URI.toString() + "_edit"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.THIRD_PARTY_URI, InfoTables.KEY_THIRD_PARTY_NAME,
                        ctx.getString(R.string.third_parties), EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID,
                        EDIT_THIRD_PARTY_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        protected fun createTagsEditDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.TAGS_URI.toString() + "_edit"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.TAGS_URI, InfoTables.KEY_TAG_NAME,
                        ctx.getString(R.string.tags), EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID, EDIT_TAG_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }

        protected fun createModesEditDialog(ctx: Context): InfoManagerDialog {
            val k = DbContentProvider.MODES_URI.toString() + "_edit"
            var d: InfoManagerDialog? = mInfoManagerDialogMap.get(k)
            if (d == null) {
                d = InfoManagerDialog.newInstance(DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME,
                        ctx.getString(R.string.modes), EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID, EDIT_MODE_DIALOG_ID)
                mInfoManagerDialogMap.put(k, d)
            }
            return d
        }
    }
}
