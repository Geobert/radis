package fr.geobert.radis.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import fr.geobert.radis.Operation;
import fr.geobert.radis.OperationList;
import fr.geobert.radis.R;
import fr.geobert.radis.ScheduledOperation;

public class OperationEditor extends CommonOpEditor {
	protected Operation mOriginalOp;
	protected static final int ASK_UPDATE_SCHEDULED_DIALOG_ID = 10;

	@Override
	protected void setView() {
		setContentView(R.layout.operation_edit);
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId > 0) {
			Cursor opCursor = mDbHelper.fetchOneOp(mRowId, mCurAccountId);
			startManagingCursor(opCursor);
			mCurrentOp = new Operation(opCursor);
			mOriginalOp = new Operation(opCursor);
		} else {
			mCurrentOp = new Operation();
			mCurrentOp.mAccountId = mCurAccountId;
		}
	}

	@Override
	protected void populateFields() {
		Operation op = mCurrentOp;
		populateCommonFields(op);
	}

	private void setResAndExit(boolean sumUpdateIsNeeded) {
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
		if (mRowId <= 0) {
			setResAndExit(mDbHelper.createOp(op, op.mAccountId));
		} else {
			if (op.equals(mOriginalOp)) {
				setResAndExit(false);
			} else {
				if (op.mScheduledId > 0 && !op.equalsButDate(mOriginalOp)) {
					showDialog(ASK_UPDATE_SCHEDULED_DIALOG_ID);
				} else {
					setResAndExit(mDbHelper.updateOp(mRowId, op, op.mAccountId));
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
									mDbHelper.updateScheduledOp(
											mCurrentOp.mScheduledId, op, true);
									ScheduledOperation.updateAllOccurences(
											mDbHelper, op, mPreviousSum,
											mCurrentOp.mScheduledId);
									OperationEditor.this.setResAndExit(false);
								}
							})
					.setNeutralButton(R.string.disconnect,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mCurrentOp.mScheduledId = 0;
									OperationEditor.this
											.setResAndExit(mDbHelper.updateOp(
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
}
