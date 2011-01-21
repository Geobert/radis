package fr.geobert.Radis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

public class InfoEditor {
	private OperationEditor mContext = null;
	private AlertDialog.Builder mBuilder = null;
	private AlertDialog mDialog = null;
	private Button mAddBut;
	private Button mDelBut;
	private Button mEditBut;
	private int mSelectedInfo = -1;
	private Cursor mCursor;
	private OperationsDbAdapter mDbHelper;

	InfoEditor(OperationEditor context, OperationsDbAdapter dbHelper,
			String title, Cursor c, String colName) {
		mContext = context;
		mDbHelper = dbHelper;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		LayoutInflater inflater = (LayoutInflater) context.getLayoutInflater();
		View layout = inflater.inflate(R.layout.info_list, null);
		builder.setPositiveButton(context.getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
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
		builder.setView(layout);
		mBuilder = builder;
		fillData(c, colName);
		mCursor = c;
		mDelBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onDeleteClicked();
			}
		});
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

	public AlertDialog getDialog() {
		mDialog = mBuilder.create();
		refreshToolbarStatus();
		return mDialog;
	}

	private void refreshToolbarStatus() {
		boolean oneSelected = mSelectedInfo != -1;
		mDelBut.setEnabled(oneSelected);
		mEditBut.setEnabled(oneSelected);
	}

	private void onDeleteClicked() {
		mCursor.moveToPosition(mSelectedInfo);
		mDbHelper.deleteThirdParty(mCursor.getLong(mCursor
				.getColumnIndex(OperationsDbAdapter.KEY_THIRD_PARTY_ROWID)));
	}
}
