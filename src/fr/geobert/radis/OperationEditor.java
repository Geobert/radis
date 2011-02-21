package fr.geobert.radis;

import java.text.ParseException;
import java.util.HashMap;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

public class OperationEditor extends Activity {
	private static final int THIRD_PARTIES_DIALOG_ID = 1;
	private static final int TAGS_DIALOG_ID = 2;
	private static final int MODES_DIALOG_ID = 3;
	private static final int EDIT_THIRD_PARTY_DIALOG_ID = 4;
	private static final int EDIT_TAG_DIALOG_ID = 5;
	private static final int EDIT_MODE_DIALOG_ID = 6;
	private static final int DELETE_THIRD_PARTY_DIALOG_ID = 7;
	private static final int DELETE_TAG_DIALOG_ID = 8;
	private static final int DELETE_MODE_DIALOG_ID = 9;

	private OperationsDbAdapter mDbHelper;
	private AutoCompleteTextView mOpThirdPartyText;
	private EditText mOpSumText;
	private AutoCompleteTextView mOpModeText;
	private AutoCompleteTextView mOpTagText;
	// private Button mOpDateBut;
	private DatePicker mDatePicker;
	private EditText mNotesText;
	private Long mRowId;
	private Long mAccountId;

	private Operation mCurrentOp;
	private double mPreviousSum = 0.0;
	private HashMap<String, InfoManager> mInfoManagersMap;
	private CorrectCommaWatcher mSumTextWatcher;
	private boolean mOnRestore = false;
	public String mCurrentInfoTable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (null == Operation.SUM_FORMAT) {
			Tools.initSumFormater();
		}
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
		mOpThirdPartyText.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME));
		mOpModeText = (AutoCompleteTextView) findViewById(R.id.edit_op_mode);
		mOpModeText.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_MODES_TABLE,
				OperationsDbAdapter.KEY_MODE_NAME));
		mOpSumText = (EditText) findViewById(R.id.edit_op_sum);
		mSumTextWatcher = new CorrectCommaWatcher(Operation.SUM_FORMAT
				.getDecimalFormatSymbols().getDecimalSeparator(), mOpSumText);
		mOpTagText = (AutoCompleteTextView) findViewById(R.id.edit_op_tag);
		mOpTagText.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_TAGS_TABLE,
				OperationsDbAdapter.KEY_TAG_NAME));
		mDatePicker = (DatePicker) findViewById(R.id.edit_op_date);
		mNotesText = (EditText) findViewById(R.id.edit_op_notes);
		mInfoManagersMap = new HashMap<String, InfoManager>();
	}

	private void invertSign() throws ParseException {
		mSumTextWatcher.setAutoNegate(false);
		Double sum = Operation.SUM_FORMAT
				.parse(mOpSumText.getText().toString()).doubleValue();
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
			errMsg.append(getString(R.string.empty_amount));
			res = false;
		}
		return res;
	}

	private Dialog createInfoListDialog(String table, String colName,
			String title, int editId, int deletiId) {
		InfoManager i = new InfoManager(this, mDbHelper, title, table, colName,
				editId, deletiId);
		mInfoManagersMap.put(table, i);
		return i.getListDialog();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case THIRD_PARTIES_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
					OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
					getString(R.string.third_parties),
					EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID);
		case TAGS_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_TAGS_TABLE,
					OperationsDbAdapter.KEY_TAG_NAME, getString(R.string.tags),
					EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID);
		case MODES_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_MODES_TABLE,
					OperationsDbAdapter.KEY_MODE_NAME,
					getString(R.string.modes), EDIT_MODE_DIALOG_ID,
					DELETE_MODE_DIALOG_ID);
		case EDIT_THIRD_PARTY_DIALOG_ID:
		case EDIT_TAG_DIALOG_ID:
		case EDIT_MODE_DIALOG_ID:
			InfoManager i = mInfoManagersMap.get(mCurrentInfoTable);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		case DELETE_THIRD_PARTY_DIALOG_ID:
		case DELETE_TAG_DIALOG_ID:
		case DELETE_MODE_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mInfoManagersMap.get(mCurrentInfoTable)
									.deleteInfo();
						}
					});
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case EDIT_THIRD_PARTY_DIALOG_ID:
		case EDIT_TAG_DIALOG_ID:
		case EDIT_MODE_DIALOG_ID:
			mInfoManagersMap.get(mCurrentInfoTable).initEditDialog(dialog);
			break;
		case THIRD_PARTIES_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE)
					.onPrepareDialog((AlertDialog) dialog);
			break;
		case TAGS_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_TAGS_TABLE)
					.onPrepareDialog((AlertDialog) dialog);
			break;
		case MODES_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_MODES_TABLE)
					.onPrepareDialog((AlertDialog) dialog);
			break;
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
			if (mCurrentOp.mSum == 0.0) {
				mOpSumText.setText("");
			} else {
				mOpSumText.setText(mCurrentOp.getSumStr());
			}
			mSumTextWatcher.setAutoNegate(true);
		}
		Tools.setSumTextGravity(mOpSumText);
		Operation op = mCurrentOp;
		mPreviousSum = op.mSum;
		Tools.setTextWithoutComplete(mOpThirdPartyText, op.mThirdParty);
		Tools.setTextWithoutComplete(mOpModeText, op.mMode);
		Tools.setTextWithoutComplete(mOpTagText, op.mTag);
		mDatePicker.updateDate(op.getYear(), op.getMonth(), op.getDay());
		mNotesText.setText(op.mNotes);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mOnRestore) {
			populateFields();
		} else {
			mOnRestore = false;
		}
		mOpSumText.addTextChangedListener(mSumTextWatcher);
		initListeners();
	}

	private void initListeners() {
		Button confirmButton = (Button) findViewById(R.id.confirm_op);
		Button cancelButton = (Button) findViewById(R.id.cancel_op);
		Button thirdPartyEdit = (Button) findViewById(R.id.edit_op_third_parties_list);
		Button tagsEdit = (Button) findViewById(R.id.edit_op_tags_list);
		Button modesEdit = (Button) findViewById(R.id.edit_op_modes_list);
		Button opSignBut = (Button) findViewById(R.id.edit_op_sign);
		// listeners
		confirmButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					StringBuilder errMsg = new StringBuilder();

					if (isFormValid(errMsg)) {
						saveState();
						Intent res = new Intent();
						res.putExtra("sum", mCurrentOp.mSum);
						res.putExtra("oldSum", mPreviousSum);
						setResult(RESULT_OK, res);
						finish();
					} else {
						Tools.popError(OperationEditor.this, errMsg.toString(),
								null);
					}
				} catch (ParseException e) {
					Tools.popError(OperationEditor.this, e.getMessage(), null);
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

	private void saveState() throws ParseException {
		Operation op = mCurrentOp;
		op.mThirdParty = mOpThirdPartyText.getText().toString();
		op.mMode = mOpModeText.getText().toString();
		op.mTag = mOpTagText.getText().toString();
		op.setSumStr(mOpSumText.getText().toString());
		op.mNotes = mNotesText.getText().toString();

		DatePicker dp = mDatePicker;
		dp.clearChildFocus(getCurrentFocus());
		op.setDay(dp.getDayOfMonth());
		op.setMonth(dp.getMonth());
		op.setYear(dp.getYear());

		if (mRowId == null) {
			long id = mDbHelper.createOp(op);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateOp(mRowId, op);
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (Tools.onKeyLongPress(keyCode, event, this)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("third_party", mOpThirdPartyText.getText()
				.toString());
		outState.putString("tag", mOpTagText.getText().toString());
		outState.putString("mode", mOpModeText.getText().toString());
		outState.putString("sum", mOpSumText.getText().toString());
		if (mRowId != null) {
			outState.putLong("rowId", mRowId.longValue());
		}
		outState.putString("notes", mNotesText.getText().toString());
		outState.putDouble("previousSum", mPreviousSum);

		mOnRestore = true;
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		Tools.setTextWithoutComplete(mOpThirdPartyText,
				state.getString("third_party"));
		Tools.setTextWithoutComplete(mOpTagText, state.getString("tag"));
		Tools.setTextWithoutComplete(mOpModeText, state.getString("mode"));
		mOpSumText.setText(state.getString("sum"));
		Tools.setSumTextGravity(mOpSumText);
		mOnRestore = true;
		long rowId = state.getLong("rowId");
		mRowId = rowId != 0 ? Long.valueOf(rowId) : null;
		mPreviousSum = state.getDouble("previousSum");
		Operation op = (Operation) getLastNonConfigurationInstance();
		mCurrentOp = op;
		if (op != null) {
			mDatePicker.updateDate(op.getYear(), op.getMonth(), op.getDay());
		} else {
			ErrorReporter.getInstance().handleException(
					new NullPointerException("op was not correctly restored"));
		}
		mNotesText.setText(state.getString("notes"));
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		DatePicker dp = mDatePicker;
		dp.clearChildFocus(getCurrentFocus());
		Operation op = mCurrentOp;
		op.setDay(dp.getDayOfMonth());
		op.setMonth(dp.getMonth());
		op.setYear(dp.getYear());
		return op;
	}
}
