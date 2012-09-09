package fr.geobert.radis.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import fr.geobert.radis.Operation;
import fr.geobert.radis.OperationList;
import fr.geobert.radis.R;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;

public class OperationEditor extends CommonOpEditor {
	private static final String TAG = "OperationEditor";
	protected Operation mOriginalOp;
	protected static final int ASK_UPDATE_SCHEDULED_DIALOG_ID = 10;
	

	private static final int GET_OP = 610;

	@Override
	protected void setView() {
		setContentView(R.layout.operation_edit);
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId > 0) {
			fetchOp(GET_OP);
		} else {
			mCurrentOp = new Operation();
			mCurrentOp.mAccountId = mCurAccountId;
			populateFields();
		}
	}

	@Override
	protected void populateFields() {
		populateCommonFields(mCurrentOp);
	}

	private void setResAndExit(boolean sumUpdateIsNeeded) {
		Log.d(TAG, "setResAndExit, sumUpdateIsNeeded : " + (sumUpdateIsNeeded ? "YES" : "NO"));
		Intent res = new Intent();
		res.putExtra(OperationList.SUM, mCurrentOp.mSum);
		res.putExtra(OperationList.OLD_SUM, mPreviousSum);

		if (sumUpdateIsNeeded) {
			res.putExtra(OperationList.OP_DATE, mCurrentOp.getDate());
			res.putExtra(OperationList.NEW_ROWID, mCurrentOp.mRowId);
			res.putExtra(OperationList.TRANSFERT_ID,
					mCurrentOp.mTransferAccountId);
		}
		res.putExtra(OperationList.UPDATE_SUM_NEEDED, sumUpdateIsNeeded);
		// cas où on edit en supprimant transfert -> maj somme du dstAccount
		if (mOriginalOp != null
				&& (mOriginalOp.mTransferAccountId != mCurrentOp.mTransferAccountId)) {
			res.putExtra(OperationList.OLD_TRANSFERT_ID,
					mOriginalOp.mTransferAccountId);
		}
		setResult(RESULT_OK, res);
		finish();
	}

	@Override
	protected void saveOpAndExit() {
		Operation op = mCurrentOp;
		Log.d(TAG, "saveOpAndExit, mRowId : " + mRowId);
		if (mRowId <= 0) {
			setResAndExit(OperationTable.createOp(this, op, op.mAccountId));
		} else {
			if (op.equals(mOriginalOp)) {
				setResAndExit(false);
			} else {
				if (op.mScheduledId > 0 && !op.equalsButDate(mOriginalOp)) {
					showDialog(ASK_UPDATE_SCHEDULED_DIALOG_ID);
				} else {
					setResAndExit(OperationTable.updateOp(this, mRowId, op,
							op.mAccountId));
				}
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ASK_UPDATE_SCHEDULED_DIALOG_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.ask_update_scheduling)
					.setCancelable(false)
					.setPositiveButton(R.string.update,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									final ScheduledOperation op = new ScheduledOperation(
											mCurrentOp, mCurrentOp.mAccountId);
									ScheduledOperationTable.updateScheduledOp(
											OperationEditor.this,
											mCurrentOp.mScheduledId, op, true);
									ScheduledOperationTable.updateAllOccurences(
											OperationEditor.this, op,
											mPreviousSum,
											mCurrentOp.mScheduledId);
									OperationEditor.this.setResAndExit(false);
								}
							})
					.setNeutralButton(R.string.disconnect,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mCurrentOp.mScheduledId = 0;
									OperationEditor.this.setResAndExit(OperationTable
											.updateOp(OperationEditor.this,
													mRowId, mCurrentOp,
													mCurrentOp.mAccountId));
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			return builder.create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("originalOp", mOriginalOp);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		mOriginalOp = savedInstanceState.getParcelable("originalOp");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		CursorLoader l = null;
		if (l == null) {
			switch (id) {
			case GET_OP:
				l = new CursorLoader(this,
						Uri.parse(DbContentProvider.OPERATION_JOINED_URI + "/"
								+ mRowId), OperationTable.OP_COLS_QUERY, null,
						null, null);
				break;

			default:
				break;
			}
		}
		return l;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		hideProgress();
		switch (loader.getId()) {
		case GET_OP:
			data.moveToFirst();
			mCurrentOp = new Operation(data);
			mOriginalOp = new Operation(data);
			populateFields();
			break;
		default:
			break;
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}
}
