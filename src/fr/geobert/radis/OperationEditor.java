package fr.geobert.radis;

import java.text.ParseException;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

public class OperationEditor extends CommonOpEditor {
	protected Long mAccountId;
	protected double mPreviousSum = 0.0;

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
		mDbHelper = new OperationsDbAdapter(this, mAccountId);
		mDbHelper.open();
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId != null) {
			Cursor opCursor = mDbHelper.fetchOneOp(mRowId);
			startManagingCursor(opCursor);
			mCurrentOp = new Operation(opCursor);
		} else {
			mCurrentOp = new Operation();
		}
	}
	
	@Override
	protected void populateFields() {
		Operation op = mCurrentOp;
		mPreviousSum = op.mSum;
		populateCommonFields(op);
	}

	@Override
	protected void initListeners() {
		super.initListeners();

	}

	@Override
	protected void saveOpAndSetActivityResult() throws ParseException {
		Operation op = mCurrentOp;
		fillOperationWithInputs(op);
		if (mRowId == null) {
			long id = mDbHelper.createOp(op);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateOp(mRowId, op);
		}
		Intent res = new Intent();
		res.putExtra("sum", mCurrentOp.mSum);
		res.putExtra("oldSum", mPreviousSum);
		setResult(RESULT_OK, res);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putDouble("previousSum", mPreviousSum);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mPreviousSum = state.getDouble("previousSum");
	}

}
