package fr.geobert.radis.ui.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import fr.geobert.radis.R;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.Tools;

import java.util.HashMap;

public class InfoManagerDialog extends DialogFragment {
    public static final int THIRD_PARTIES_DIALOG_ID = 1;
    public static final int TAGS_DIALOG_ID = 2;
    public static final int MODES_DIALOG_ID = 3;
    public static final int EDIT_THIRD_PARTY_DIALOG_ID = 4;
    public static final int EDIT_TAG_DIALOG_ID = 5;
    public static final int EDIT_MODE_DIALOG_ID = 6;
    public static final int DELETE_THIRD_PARTY_DIALOG_ID = 7;
    public static final int DELETE_TAG_DIALOG_ID = 8;
    public static final int DELETE_MODE_DIALOG_ID = 9;
    private static HashMap<String, InfoManager> mInfoManagersMap = new HashMap<String, InfoManager>();
    private static HashMap<String, InfoManagerDialog> mInfoManagerDialogMap = new HashMap<String, InfoManagerDialog>();
    private InfoManager mInfoManager = null;
    private int mMode = -1;

    public static void resetInfoManager() {
        mInfoManagersMap.clear();
    }

    public static InfoManagerDialog newInstance(Uri table, String colName,
                                                String title, int editId, int deleteId, int mode) {
        InfoManagerDialog frag = new InfoManagerDialog();
        Bundle args = new Bundle();
        args.putParcelable("table", table);
        args.putString("colName", colName);
        args.putString("title", title);
        args.putInt("editId", editId);
        args.putInt("deleteId", deleteId);
        args.putInt("mode", mode);
        frag.setArguments(args);
        return frag;
    }

    public static InfoManagerDialog createThirdPartiesListDialog(Context ctx) {
        final String k = DbContentProvider.THIRD_PARTY_URI.toString() + "_list";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.THIRD_PARTY_URI,
                    InfoTables.KEY_THIRD_PARTY_NAME,
                    ctx.getString(R.string.third_parties),
                    EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID, THIRD_PARTIES_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    public static InfoManagerDialog createTagsListDialog(Context ctx) {
        final String k = DbContentProvider.TAGS_URI.toString() + "_list";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.TAGS_URI,
                    InfoTables.KEY_TAG_NAME, ctx.getString(R.string.tags),
                    EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID, TAGS_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    public static InfoManagerDialog createModesListDialog(Context ctx) {
        final String k = DbContentProvider.MODES_URI.toString() + "_list";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.MODES_URI,
                    InfoTables.KEY_MODE_NAME, ctx.getString(R.string.modes),
                    EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID, MODES_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    protected static InfoManagerDialog createThirdPartiesDeleteDialog(Context ctx) {
        final String k = DbContentProvider.THIRD_PARTY_URI.toString() + "_del";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.THIRD_PARTY_URI,
                    InfoTables.KEY_THIRD_PARTY_NAME,
                    ctx.getString(R.string.third_parties),
                    EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    protected static InfoManagerDialog createTagsDeleteDialog(Context ctx) {
        final String k = DbContentProvider.TAGS_URI.toString() + "_del";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.TAGS_URI,
                    InfoTables.KEY_TAG_NAME, ctx.getString(R.string.tags),
                    EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    protected static InfoManagerDialog createModesDeleteDialog(Context ctx) {
        final String k = DbContentProvider.MODES_URI.toString() + "_del";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.MODES_URI,
                    InfoTables.KEY_MODE_NAME, ctx.getString(R.string.modes),
                    EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    public static InfoManagerDialog createInfoDeleteDialog(final int deleteId, final Context ctx) {
        switch (deleteId) {
            case DELETE_THIRD_PARTY_DIALOG_ID:
                return createThirdPartiesDeleteDialog(ctx);
            case DELETE_TAG_DIALOG_ID:
                return createTagsDeleteDialog(ctx);
            case DELETE_MODE_DIALOG_ID:
                return createModesDeleteDialog(ctx);
            default:
                return null;
        }
    }

    public static InfoManagerDialog createInfoEditDialog(final int editId, final Context ctx) {
        switch (editId) {
            case EDIT_THIRD_PARTY_DIALOG_ID:
                return createThirdPartiesEditDialog(ctx);
            case EDIT_TAG_DIALOG_ID:
                return createTagsEditDialog(ctx);
            case EDIT_MODE_DIALOG_ID:
                return createModesEditDialog(ctx);
            default:
                return null;
        }
    }

    protected static InfoManagerDialog createThirdPartiesEditDialog(Context ctx) {
        final String k = DbContentProvider.THIRD_PARTY_URI.toString() + "_edit";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.THIRD_PARTY_URI,
                    InfoTables.KEY_THIRD_PARTY_NAME,
                    ctx.getString(R.string.third_parties),
                    EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID, EDIT_THIRD_PARTY_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    protected static InfoManagerDialog createTagsEditDialog(Context ctx) {
        final String k = DbContentProvider.TAGS_URI.toString() + "_edit";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.TAGS_URI,
                    InfoTables.KEY_TAG_NAME, ctx.getString(R.string.tags),
                    EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID, EDIT_TAG_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    protected static InfoManagerDialog createModesEditDialog(Context ctx) {
        final String k = DbContentProvider.MODES_URI.toString() + "_edit";
        InfoManagerDialog d = mInfoManagerDialogMap.get(k);
        if (d == null) {
            d = InfoManagerDialog.newInstance(DbContentProvider.MODES_URI,
                    InfoTables.KEY_MODE_NAME, ctx.getString(R.string.modes),
                    EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID, EDIT_MODE_DIALOG_ID);
            mInfoManagerDialogMap.put(k, d);
        }
        return d;
    }

    private InfoManager createInfoManagerIfNeeded(Uri table, String colName,
                                                  String title, int editId, int deleteId) {
        InfoManager i = mInfoManagersMap.get(table.toString());
        if (null == i) {
            i = new InfoManager(this, title, table, colName, editId, deleteId);
            mInfoManagersMap.put(table.toString(), i);
        }
        return i;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        final Uri table = args.getParcelable("table");
        final String colName = args.getString("colName");
        final String title = args.getString("title");
        final int editId = args.getInt("editId");
        final int deleteId = args.getInt("deleteId");
        final int mode = args.getInt("mode");
        mMode = mode;
        mInfoManager = createInfoManagerIfNeeded(table, colName, title, editId, deleteId);
        switch (mode) {
            case THIRD_PARTIES_DIALOG_ID:
            case TAGS_DIALOG_ID:
            case MODES_DIALOG_ID:
                return mInfoManager.getListDialog();
            case EDIT_MODE_DIALOG_ID:
            case EDIT_TAG_DIALOG_ID:
            case EDIT_THIRD_PARTY_DIALOG_ID:
                return mInfoManager.getEditDialog();
            case DELETE_MODE_DIALOG_ID:
            case DELETE_TAG_DIALOG_ID:
            case DELETE_THIRD_PARTY_DIALOG_ID:
                return Tools.createDeleteConfirmationDialog(getActivity(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        createInfoManagerIfNeeded(table, colName, title, editId, deleteId).deleteInfo();
                    }
                });
            default:
                // should not happen
                return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog d = getDialog();
        switch (mMode) {
            case THIRD_PARTIES_DIALOG_ID:
            case TAGS_DIALOG_ID:
            case MODES_DIALOG_ID:
                mInfoManager.onPrepareDialog((AlertDialog) d);
                break;
            case EDIT_MODE_DIALOG_ID:
            case EDIT_TAG_DIALOG_ID:
            case EDIT_THIRD_PARTY_DIALOG_ID:
                mInfoManager.initEditDialog(d);
                break;
            case DELETE_MODE_DIALOG_ID:
            case DELETE_TAG_DIALOG_ID:
            case DELETE_THIRD_PARTY_DIALOG_ID:
                break;
            default:
                // should not happen
        }
    }
}