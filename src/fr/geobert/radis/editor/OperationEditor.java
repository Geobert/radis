package fr.geobert.radis.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.Tools;

public class OperationEditor extends CommonOpEditor {
	protected Long mAccountId;
	protected Operation mOriginalOp;
	protected static final int ASK_UPDATE_SCHEDULED_DIALOG_ID = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Bundle extras = getIntent().getExtras();
		mAccountId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID)
				: null;
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void setView() {
		setContentView(R.layout.operation_edit);
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId > 0) {
			Cursor opCursor = mDbHelper.fetchOneOp(mRowId, mAccountId);
			startManagingCursor(opCursor);
			mCurrentOp = new Operation(opCursor);
			mOriginalOp = new Operation(opCursor);
		} else {
			mCurrentOp = new Operation();
		}
	}

	@Override
	protected void populateFields() {
		Operation op = mCurrentOp;
		populateCommonFields(op);
	}

	private void setResAndExit(boolean sumUpdateIsNeeded) {
		Intent res = new Intent();
		if (sumUpdateIsNeeded) {
			res.putExtra("sum", mCurrentOp.mSum);
			res.putExtra("oldSum", mPreviousSum);
			res.putExtra("opDate", mCurrentOp.getDate());
			res.putExtra("newRowId", mCurrentOp.mRowId);
		}
		res.putExtra("sumUpdateNeeded", sumUpdateIsNeeded);
		setResult(RESULT_OK, res);
		finish();
	}

	@Override
	protected void saveOpAndExit() {
		Operation op = mCurrentOp;
		if (mRowId <= 0) {
			setResAndExit(mDbHelper.createOp(op, mAccountId));
		} else {
			if (op.equals(mOriginalOp)) {
				setResAndExit(false);
			} else {
				if (op.mScheduledId > 0 && !op.equalsButDate(mOriginalOp)) {
					showDialog(ASK_UPDATE_SCHEDULED_DIALOG_ID);
				} else {
					setResAndExit(mDbHelper.updateOp(mRowId, op, mAccountId));
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
											mCurrentOp, mAccountId);
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
													mRowId, mCurrentOp, mAccountId));
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
