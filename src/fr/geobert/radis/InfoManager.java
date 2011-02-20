package fr.geobert.radis;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class InfoManager {
	private OperationEditor mContext = null;
	private AlertDialog.Builder mBuilder = null;
	private AlertDialog mListDialog = null;
	private Button mAddBut;
	private Button mDelBut;
	private Button mEditBut;
	private int mSelectedInfo = -1;
	private Cursor mCursor;
	private OperationsDbAdapter mDbHelper;
	private Bundle mInfo;
	private EditText mEditorText;
	private Button mOkBut;
	private AutoCompleteTextView mInfoText;
	private int mEditId;
	private int mDeleteId;

	@SuppressWarnings("serial")
	private static final HashMap<String, Integer> EDITTEXT_OF_INFO = new HashMap<String, Integer>() {
		{
			put(OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
					R.id.edit_op_third_party);
			put(OperationsDbAdapter.DATABASE_TAGS_TABLE, R.id.edit_op_tag);
			put(OperationsDbAdapter.DATABASE_MODES_TABLE, R.id.edit_op_mode);
		}
	};

	InfoManager(OperationEditor context, OperationsDbAdapter dbHelper,
			String title, String table, String colName, int editId, int deleteId) {
		mDbHelper = dbHelper;
		mContext = context;
		mInfo = new Bundle();
		mInfo.putString("title", title);
		mInfo.putString("table", table);
		mInfo.putString("colName", colName);
		mEditId = editId;
		mDeleteId = deleteId;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		LayoutInflater inflater = (LayoutInflater) context.getLayoutInflater();
		View layout = inflater.inflate(R.layout.info_list, null);
		builder.setPositiveButton(context.getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						infoSelected();
					}
				}).setNegativeButton(context.getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		mAddBut = (Button) layout.findViewById(R.id.create_info);
		mDelBut = (Button) layout.findViewById(R.id.del_info);
		mEditBut = (Button) layout.findViewById(R.id.edit_info);
		mInfoText = (AutoCompleteTextView) context
				.findViewById(EDITTEXT_OF_INFO.get(table));

		builder.setView(layout);
		mBuilder = builder;
		Cursor c = dbHelper.fetchMatchingInfo(table, colName, null);
		fillData(c, colName);

		mCursor = c;
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
		mCursor.moveToPosition(lv.getCheckedItemPosition());
		Tools.setTextWithoutComplete(mInfoText, mCursor.getString(mCursor
				.getColumnIndex(mInfo.getString("colName"))));
	}

	public void fillData(Cursor c, String colName) {
		mBuilder.setSingleChoiceItems(c, -1, colName,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						mSelectedInfo = item;
						refreshToolbarStatus();
					}
				});
	}

	public AlertDialog getListDialog() {
		mListDialog = mBuilder.create();
		return mListDialog;
	}

	public void onPrepareDialog(AlertDialog dialog) {
		mOkBut = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		refreshToolbarStatus();
	}

	public void refreshToolbarStatus() {
		boolean oneSelected = mSelectedInfo != -1;
		mDelBut.setEnabled(oneSelected);
		mEditBut.setEnabled(oneSelected);
		mOkBut.setEnabled(oneSelected);
	}

	private void onDeleteClicked() {
		mContext.mCurrentInfoTable = mInfo.getString("table");
		mContext.showDialog(mDeleteId);
	}

	public void deleteInfo() {
		mCursor.moveToPosition(mSelectedInfo);
		mDbHelper.deleteInfo(mInfo.getString("table"),
				mCursor.getLong(mCursor.getColumnIndex("_id")));
	}

	private void onAddClicked() {
		Bundle info = mInfo;
		info.remove("value");
		info.remove("rowId");
		mContext.mCurrentInfoTable = info.getString("table");
		mContext.showDialog(mEditId);
	}

	private void onEditClicked() {
		ListView lv = mListDialog.getListView();
		mCursor.moveToPosition(lv.getCheckedItemPosition());
		Bundle info = mInfo;
		info.putString("value", mCursor.getString(mCursor.getColumnIndex(mInfo
				.getString("colName"))));
		info.putLong("rowId", mCursor.getLong(mCursor.getColumnIndex("_id")));
		mContext.mCurrentInfoTable = info.getString("table");
		mContext.showDialog(mEditId);
	}

	private AlertDialog d;

	public void initEditDialog(Dialog dialog) {
		EditText t = mEditorText;
		Bundle info = mInfo;
		if (null != info) {
			//dialog.setTitle(info.getString("title"));
			String tmp = info.getString("value");
			t.setText(tmp);
		}
	}

	public Dialog getEditDialog() {
		Activity context = mContext;

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = (LayoutInflater) context.getLayoutInflater();
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
		return builder.create();
	}

	private void saveText() {
		EditText t = mEditorText;
		String value = t.getText().toString();
		long rowId = mInfo.getLong("rowId");
		long id = mDbHelper.getKeyIdOrCreate(value, mInfo.getString("table"));
		if (rowId != 0 || id != 0) { // update
			if (id != 0) {
				mDbHelper.updateInfo(mInfo.getString("table"), id, value);
			} else {
				mDbHelper.updateInfo(mInfo.getString("table"), rowId, value);
			}
		} else { // create
			mDbHelper.createInfo(mInfo.getString("table"), value);
		}
		mCursor.requery();
	}
}
