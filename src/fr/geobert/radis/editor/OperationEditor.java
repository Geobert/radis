package fr.geobert.radis.editor;

import java.text.ParseException;

import org.acra.ErrorReporter;

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
	protected void initDbHelper() {
		mDbHelper = CommonDbAdapter.getInstance(this, mAccountId);
		mDbHelper.open();
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId > 0) {
			Cursor opCursor = mDbHelper.fetchOneOp(mRowId);
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

	private void setResAndExit() {
		try {
			Intent res = new Intent();
			res.putExtra("sum", mCurrentOp.mSum);
			res.putExtra("oldSum", mPreviousSum);
			setResult(RESULT_OK, res);
			super.saveOpAndExit();
		} catch (ParseException e) {
			ErrorReporter.getInstance().handleException(e);
			e.printStackTrace();
		}
	}

	@Override
	protected void saveOpAndExit() throws ParseException {
		Operation op = mCurrentOp;
		if (mRowId <= 0) {
			long id = mDbHelper.createOp(op);
			if (id > 0) {
				mRowId = id;
			}
			setResAndExit();
		} else {
			if (op.equals(mOriginalOp)) {
				setResAndExit();
			} else {
				if (op.mScheduledId > 0 && !op.equalsButDate(mOriginalOp)) {
					showDialog(ASK_UPDATE_SCHEDULED_DIALOG_ID);
				} else {
					mDbHelper.updateOp(mRowId, op);
					setResAndExit();
				}
			}
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
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
									ScheduledOperation
											.updateAllOccurences(mDbHelper, op,
													mPreviousSum,
													mCurrentOp.mScheduledId);
									OperationEditor.this.setResAndExit();
								}
							})
					.setNeutralButton(R.string.disconnect,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mCurrentOp.mScheduledId = 0;
									mDbHelper.updateOp(mRowId, mCurrentOp);
									OperationEditor.this.setResAndExit();
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
