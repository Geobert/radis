package fr.geobert.radis.ui.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import fr.geobert.radis.R;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.Tools;

import java.util.HashMap;

public class InfoManager implements LoaderCallbacks<Cursor> {
    @SuppressWarnings("serial")
    private static final HashMap<String, Integer> EDITTEXT_OF_INFO = new HashMap<String, Integer>() {
        {
            put(DbContentProvider.THIRD_PARTY_URI.toString(),
                    R.id.edit_op_third_party);
            put(DbContentProvider.TAGS_URI.toString(), R.id.edit_op_tag);
            put(DbContentProvider.MODES_URI.toString(), R.id.edit_op_mode);
        }
    };
    private final DialogFragment mDiagFragment;
    private CommonOpEditor mContext = null;
    private AlertDialog.Builder mBuilder = null;
    private AlertDialog mListDialog = null;
    private Button mAddBut;
    private Button mDelBut;
    private Button mEditBut;
    private int mSelectedInfo = -1;
    private Cursor mCursor;
    private Bundle mInfo;
    private EditText mEditorText;
    private Button mOkBut;
    private AutoCompleteTextView mInfoText;
    private int mEditId;
    private int mDeleteId;
    private String mOldValue;
    private SimpleCursorAdapter mAdapter;
    private int GET_MATCHING_INFO_ID;
    private AlertDialog mEditDialog = null;

    InfoManager(DialogFragment fragment, String title, Uri table,
                String colName, int editId, int deleteId) {
        mContext = (CommonOpEditor) fragment.getActivity();
        mDiagFragment = fragment;
        GET_MATCHING_INFO_ID = EDITTEXT_OF_INFO.get(table.toString());
        mAdapter = new SimpleCursorAdapter(mContext,
                android.R.layout.simple_list_item_single_choice, null,
                new String[]{colName}, new int[]{android.R.id.text1},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position,
                        convertView, parent);
                if (Build.VERSION.SDK_INT < 11) {
                    textView.setTextColor(mContext.getResources().getColor(
                            android.R.color.black));
                }
                return textView;
            }

        };
        mInfo = new Bundle();
        mInfo.putString("title", title);
        mInfo.putParcelable("table", table);
        mInfo.putString("colName", colName);
        mEditId = editId;
        mDeleteId = deleteId;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setSingleChoiceItems(mAdapter, -1,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        mSelectedInfo = item;
                        refreshToolbarStatus();
                    }
                });
        builder.setTitle(title);
        LayoutInflater inflater = mContext.getLayoutInflater();
        View layout = inflater.inflate(R.layout.info_list, null);
        builder.setView(layout);
        mBuilder = builder;
        mContext.getSupportLoaderManager().initLoader(GET_MATCHING_INFO_ID, mInfo,
                this);

        builder.setPositiveButton(mContext.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        infoSelected();
                        mContext.getSupportLoaderManager().destroyLoader(GET_MATCHING_INFO_ID);
                        dialog.cancel();
                        mDiagFragment.dismiss();
                        mSelectedInfo = -1;
                    }
                }).setNegativeButton(mContext.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mContext.getSupportLoaderManager().destroyLoader(GET_MATCHING_INFO_ID);
                        dialog.cancel();
                        mDiagFragment.dismiss();
                        mSelectedInfo = -1;
                    }
                });

        mAddBut = (Button) layout.findViewById(R.id.create_info);
        mDelBut = (Button) layout.findViewById(R.id.del_info);
        mEditBut = (Button) layout.findViewById(R.id.edit_info);
        mInfoText = (AutoCompleteTextView) mContext
                .findViewById(EDITTEXT_OF_INFO.get(table.toString()));

        mDelBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDeleteClicked();
            }
        });

        mAddBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAddClicked();
            }
        });

        mEditBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEditClicked();
            }
        });
    }

    protected void infoSelected() {
        ListView lv = mListDialog.getListView();
        if (mCursor.moveToPosition(lv.getCheckedItemPosition())) {
            Tools.setTextWithoutComplete(mInfoText, mCursor.getString(mCursor
                    .getColumnIndex(mInfo.getString("colName"))));
        }
    }

    // public void fillData(Cursor c, String colName) {
    // mBuilder.setSingleChoiceItems(c, -1, colName,
    // new DialogInterface.OnClickListener() {
    // public void onClick(DialogInterface dialog, int item) {
    // mSelectedInfo = item;
    // refreshToolbarStatus((AlertDialog) dialog);
    // }
    // });
    // }

    public AlertDialog getListDialog() {
        if (mListDialog == null) {
            mListDialog = mBuilder.create();
        } else {
            mContext.getSupportLoaderManager().initLoader(GET_MATCHING_INFO_ID, mInfo,
                    this);
        }
        return mListDialog;
    }

    public void onPrepareDialog(AlertDialog dialog) {
        mOkBut = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        refreshToolbarStatus();
    }

    public void refreshToolbarStatus() {
        boolean oneSelected = (mSelectedInfo > -1);
//                && (mSelectedInfo < dialog.getListView().getCount());
        mDelBut.setEnabled(oneSelected);
        mEditBut.setEnabled(oneSelected);
        mOkBut.setEnabled(oneSelected);
    }

    private void onDeleteClicked() {
        mContext.mCurrentInfoTable = (Uri) mInfo.getParcelable("table");
        InfoManagerDialog.createInfoDeleteDialog(mDeleteId, mContext).show(mContext.getSupportFragmentManager(), "dialog");
    }

    public void deleteInfo() {
        mCursor.moveToPosition(mSelectedInfo);
        InfoTables.deleteInfo(mContext, (Uri) mInfo.getParcelable("table"),
                mCursor.getLong(mCursor.getColumnIndex("_id")));
        mSelectedInfo = -1;
        refresh();
        refreshToolbarStatus();
    }

    private void onAddClicked() {
        Bundle info = mInfo;
        info.remove("value");
        info.remove("rowId");
        mContext.mCurrentInfoTable = (Uri) info.getParcelable("table");
        InfoManagerDialog.createInfoEditDialog(mEditId, mContext).show(mContext.getSupportFragmentManager(), "dialog");
    }

    private void onEditClicked() {
        ListView lv = mListDialog.getListView();
        mCursor.moveToPosition(lv.getCheckedItemPosition());
        Bundle info = mInfo;
        info.putString("value", mCursor.getString(mCursor.getColumnIndex(mInfo
                .getString("colName"))));
        info.putLong("rowId", mCursor.getLong(mCursor.getColumnIndex("_id")));
        mContext.mCurrentInfoTable = (Uri) info.getParcelable("table");
        InfoManagerDialog.createInfoEditDialog(mEditId, mContext).show(mContext.getSupportFragmentManager(), "dialog");
    }

    public void initEditDialog(final Dialog dialog) {
        EditText t = mEditorText;
        Bundle info = mInfo;
        if (null != info) {
            // dialog.setTitle(info.getString("title"));
            String tmp = info.getString("value");
            mOldValue = tmp;
            t.setText(tmp);
        }
        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow()
                            .setSoftInputMode(
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public Dialog getEditDialog() {
        if (mEditDialog == null) {
            Activity context = mContext;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = context.getLayoutInflater();
            View layout = inflater.inflate(R.layout.info_edit, null);
            builder.setPositiveButton(context.getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            saveText();
                        }
                    }).setNegativeButton(context.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            Bundle info = mInfo;
            mEditorText = (EditText) layout.findViewById(R.id.info_edit_text);
            if (null != info) {
                builder.setTitle(info.getString("title"));
            }
            builder.setView(layout);
            mEditDialog = builder.create();
        }
        return mEditDialog;
    }

    private void saveText() {
        EditText t = mEditorText;
        String value = t.getText().toString().trim();
        long rowId = mInfo.getLong("rowId");
        if (rowId != 0) { // update
            InfoTables.updateInfo(mContext, (Uri) mInfo.getParcelable("table"),
                    rowId, value, mOldValue);
        } else { // create
            long id = InfoTables.getKeyIdIfExists(value,
                    (Uri) mInfo.getParcelable("table"));
            if (id > 0) { // already existing value, update
                Tools.popError(mContext,
                        mContext.getString(R.string.item_exists), null);
            } else {
                InfoTables.createInfo(mContext,
                        (Uri) mInfo.getParcelable("table"), value);
            }
        }
        refresh();
    }

    private void refresh() {
        mContext.getSupportLoaderManager().restartLoader(GET_MATCHING_INFO_ID,
                mInfo, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return InfoTables.getMatchingInfoLoader(mContext,
                (Uri) args.getParcelable("table"), args.getString("colName"),
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        mAdapter.changeCursor(data);
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }
}
