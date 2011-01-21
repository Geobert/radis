package fr.geobert.Radis;

import java.text.ParseException;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

public class OperationEditor extends Activity {
	static final int DATE_DIALOG_ID = 0;
	static final int THIRD_PARTIES_DIALOG_ID = 1;
	static final int TAGS_DIALOG_ID = 2;
	static final int MODES_DIALOG_ID = 3;

	private OperationsDbAdapter mDbHelper;
	private EditText mOpThirdPartyText;
	private EditText mOpSumText;
	private EditText mOpModeText;
	private EditText mOpTagText;
	private Button mOpDateBut;

	private Long mRowId;
	private Long mAccountId;

	// to let inner class access to the context
	private OperationEditor context = this;
	private Operation mCurrentOp;
	private double mPreviousSum = 0.0;

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			Operation op = mCurrentOp;
			op.setYear(year);
			op.setMonth(monthOfYear);
			op.setDay(dayOfMonth);
			updateDateButton();
		}

	};

	private void updateDateButton() {
		mOpDateBut.setText(mCurrentOp.getDateStr());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		mAccountId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID)
				: null;
		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState.getSerializable(Tools.EXTRAS_OP_ID);
		if (mRowId == null) {
			mRowId = extras != null ? extras.getLong(Tools.EXTRAS_OP_ID) : null;
			if (mRowId == -1) {
				mRowId = null;
			}
		}
		mDbHelper = new OperationsDbAdapter(this, mAccountId);
		mDbHelper.open();
		setContentView(R.layout.operation_edit);

		mOpThirdPartyText = (EditText) findViewById(R.id.edit_op_third_party);
		mOpModeText = (EditText) findViewById(R.id.edit_op_mode);
		mOpSumText = (EditText) findViewById(R.id.edit_op_sum);
		mOpTagText = (EditText) findViewById(R.id.edit_op_tag);
		mOpDateBut = (Button) findViewById(R.id.edit_op_date);
		Button confirmButton = (Button) findViewById(R.id.confirm_op);
		Button cancelButton = (Button) findViewById(R.id.cancel_op);
		Button thirdPartyEdit = (Button) findViewById(R.id.edit_op_third_parties_list);

		populateFields();

		// listeners
		confirmButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					StringBuilder errMsg = new StringBuilder();

					if (isFormValid(errMsg)) {
						saveState();
						Intent res = new Intent();
						res.putExtra("sum", mCurrentOp.getSum());
						res.putExtra("oldSum", mPreviousSum);
						setResult(RESULT_OK, res);
						finish();
					} else {
						Tools.getInstance(context).popError(errMsg.toString());
					}
				} catch (ParseException e) {
					Tools.getInstance(context).popError(e.getMessage());
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		mOpDateBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog(DATE_DIALOG_ID);
			}
		});

		thirdPartyEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(THIRD_PARTIES_DIALOG_ID);
			}
		});
	}

	private boolean isFormValid(StringBuilder errMsg) {
		boolean res = true;
		String sumStr = mOpSumText.getText().toString();
		if (sumStr.length() == 0) {
			if (errMsg.length() > 0) {
				errMsg.append("\n");
			}
			errMsg.append("Somme de départ vide");
			res = false;
		}
		return res;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Operation op = mCurrentOp;
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, op.getYear(),
					op.getMonth(), op.getDay());
		case THIRD_PARTIES_DIALOG_ID:
			Cursor c = mDbHelper.fetchAllThirdParties();
			Dialog d = new InfoEditor(this, mDbHelper,
					getString(R.string.third_parties_list), c,
					OperationsDbAdapter.KEY_THIRD_PARTY_NAME).getDialog();
			return d;
		}
		return null;
	}

	private void populateFields() {
		if (mRowId != null) {
			Cursor opCursor = mDbHelper.fetchOneOp(mRowId);
			startManagingCursor(opCursor);
			mCurrentOp = new Operation(opCursor);

			mOpSumText.setText(mCurrentOp.getSumStr());
		} else {
			mCurrentOp = new Operation();
			if (mCurrentOp.getSum() == 0.0) {
				mOpSumText.setText("");
			} else {
				mOpSumText.setText(mCurrentOp.getSumStr());
			}
		}
		Operation op = mCurrentOp;
		mPreviousSum = op.getSum();
		mOpThirdPartyText.setText(op.getThirdParty());
		mOpModeText.setText(op.getMode());
		mOpTagText.setText(op.getTag());
		updateDateButton();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// StringBuilder errMsg = new StringBuilder();
		// if (isFormValid(errMsg)) {
		// saveState();
		// outState.putSerializable(AccountsDbAdapter.KEY_ACCOUNT_ROWID,
		// mRowId);
		// }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
	}

	private void saveState() throws ParseException {
		Operation op = mCurrentOp;
		op.setThirdParty(mOpThirdPartyText.getText().toString());
		op.setMode(mOpModeText.getText().toString());
		op.setTag(mOpTagText.getText().toString());
		op.setSumStr(mOpSumText.getText().toString());
		op.setDateStr(mOpDateBut.getText().toString());

		if (mRowId == null) {
			long id = mDbHelper.createOp(op);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateOp(mRowId, op);
		}
	}
}
