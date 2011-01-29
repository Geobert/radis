package fr.geobert.Radis;

import java.text.ParseException;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

public class OperationEditor extends Activity {
	static final int DATE_DIALOG_ID = 0;
	static final int THIRD_PARTIES_DIALOG_ID = 1;
	static final int TAGS_DIALOG_ID = 2;
	static final int MODES_DIALOG_ID = 3;
	static final int EDIT_INFO_DIALOG_ID = 4;

	private OperationsDbAdapter mDbHelper;
	private AutoCompleteTextView mOpThirdPartyText;
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
	private InfoManager mInfoManager;

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

	private class InfoAdapter extends CursorAdapter {
		private String mColName = null;
		private String mTableName = null;
		private String mCurrentConstraint;

		private String boldFormat = "<u><b>$1</b></u>";

		public InfoAdapter(String tableName, String colName) {
			super(OperationEditor.this, null);
			mColName = colName;
			mTableName = tableName;
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(cursor.getColumnIndex(mColName));
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			mCurrentConstraint = constraint != null ? constraint.toString()
					: null;
			if (getFilterQueryProvider() != null) {
				return getFilterQueryProvider().runQuery(constraint);
			}
			Cursor c = mDbHelper.fetchMatchingInfo(mTableName, mColName,
					mCurrentConstraint);
			OperationEditor.this.startManagingCursor(c);
			return c;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			String text = convertToString(cursor).toString();
			if (mCurrentConstraint != null) {
				text = text.replaceAll("(?i)(" + mCurrentConstraint + ")",
						boldFormat);
			}
			((TextView) view).setText(Html.fromHtml(text));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
			final View view = inflater.inflate(
					android.R.layout.simple_dropdown_item_1line, parent, false);

			return view;
		}
	}

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

		mOpThirdPartyText = (AutoCompleteTextView) findViewById(R.id.edit_op_third_party);
		mOpThirdPartyText.setAdapter(new InfoAdapter(
				OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME));
		mOpModeText = (EditText) findViewById(R.id.edit_op_mode);
		mOpSumText = (EditText) findViewById(R.id.edit_op_sum);
		mOpTagText = (EditText) findViewById(R.id.edit_op_tag);
		mOpDateBut = (Button) findViewById(R.id.edit_op_date);
		Button confirmButton = (Button) findViewById(R.id.confirm_op);
		Button cancelButton = (Button) findViewById(R.id.cancel_op);
		Button thirdPartyEdit = (Button) findViewById(R.id.edit_op_third_parties_list);
		Button tagsEdit = (Button) findViewById(R.id.edit_op_tags_list);
		Button modesEdit = (Button) findViewById(R.id.edit_op_modes_list);
		Button opSignBut = (Button) findViewById(R.id.edit_op_sign);
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
		
		tagsEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(TAGS_DIALOG_ID);
			}
		});
		
		modesEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(MODES_DIALOG_ID);
			}
		});

		opSignBut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					invertSign();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	private void invertSign() throws ParseException {
		Double sum = Operation.SUM_FORMAT.parse(mOpSumText.getText().toString()).doubleValue();
		if (sum != null) {
			sum = -sum;
		}
		mOpSumText.setText(Operation.SUM_FORMAT.format(sum));
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

	private Dialog createInfoListDialog(String table, String colName,
			String title) {
		mInfoManager = new InfoManager(this, mDbHelper, title, table, colName);
		return mInfoManager.getListDialog();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Operation op = mCurrentOp;
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, op.getYear(),
					op.getMonth(), op.getDay());
		case THIRD_PARTIES_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
					OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
					getString(R.string.third_parties));
		case TAGS_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_TAGS_TABLE,
					OperationsDbAdapter.KEY_TAG_NAME, getString(R.string.tags));
		case MODES_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_MODES_TABLE,
					OperationsDbAdapter.KEY_MODE_NAME,
					getString(R.string.modes));
		case EDIT_INFO_DIALOG_ID:
			Dialog d = mInfoManager.getEditDialog();
			mInfoManager.initEditDialog(d);
			return d;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		Operation op = mCurrentOp;
		switch (id) {
		case EDIT_INFO_DIALOG_ID:
			mInfoManager.initEditDialog(dialog);
		}
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
