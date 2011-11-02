package fr.geobert.radis.editor;

import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import fr.geobert.radis.InfoAdapter;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.MyAutoCompleteTextView;
import fr.geobert.radis.tools.Tools;

public abstract class CommonOpEditor extends Activity {
	protected static final int THIRD_PARTIES_DIALOG_ID = 1;
	protected static final int TAGS_DIALOG_ID = 2;
	protected static final int MODES_DIALOG_ID = 3;
	protected static final int EDIT_THIRD_PARTY_DIALOG_ID = 4;
	protected static final int EDIT_TAG_DIALOG_ID = 5;
	protected static final int EDIT_MODE_DIALOG_ID = 6;
	protected static final int DELETE_THIRD_PARTY_DIALOG_ID = 7;
	protected static final int DELETE_TAG_DIALOG_ID = 8;
	protected static final int DELETE_MODE_DIALOG_ID = 9;

	protected Operation mCurrentOp;
	protected CommonDbAdapter mDbHelper;
	protected AutoCompleteTextView mOpThirdPartyText;
	protected EditText mOpSumText;
	protected AutoCompleteTextView mOpModeText;
	protected AutoCompleteTextView mOpTagText;
	protected DatePicker mDatePicker;
	protected EditText mNotesText;
	protected long mRowId;

	protected HashMap<String, InfoManager> mInfoManagersMap;
	protected CorrectCommaWatcher mSumTextWatcher;
	protected boolean mOnRestore = false;
	public String mCurrentInfoTable;
	protected long mPreviousSum = 0L;

	// abstract methods
	protected abstract void setView();

	protected abstract void initDbHelper();

	protected abstract void populateFields();

	protected abstract void fetchOrCreateCurrentOp();

	// default and common behaviors
	protected void saveOpAndExit() {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}

		setView();
		init(savedInstanceState);
	}

	protected void init(Bundle savedInstanceState) {
		Bundle extras = getIntent().getExtras();
		if (savedInstanceState == null) {
			mRowId = 0;
		} else {
			Serializable s = savedInstanceState.getSerializable(Tools.EXTRAS_OP_ID);
			if (s == null) {
				mRowId = 0;
			} else {
				mRowId = ((Long) s).longValue();
			}
		}
		if (mRowId <= 0) {
			mRowId = extras != null ? extras.getLong(Tools.EXTRAS_OP_ID) : 0;
		}

		mOpThirdPartyText = (MyAutoCompleteTextView) findViewById(R.id.edit_op_third_party);
		mOpModeText = (MyAutoCompleteTextView) findViewById(R.id.edit_op_mode);
		mOpSumText = (EditText) findViewById(R.id.edit_op_sum);
		mOpTagText = (MyAutoCompleteTextView) findViewById(R.id.edit_op_tag);
		mSumTextWatcher = new CorrectCommaWatcher(Formater.SUM_FORMAT.getDecimalFormatSymbols()
				.getDecimalSeparator(), mOpSumText);
		mDatePicker = (DatePicker) findViewById(R.id.edit_op_date);
		mNotesText = (EditText) findViewById(R.id.edit_op_notes);
		mInfoManagersMap = new HashMap<String, InfoManager>();

		mOpThirdPartyText.setNextFocusDownId(R.id.edit_op_sum);
		mOpSumText.setNextFocusDownId(R.id.edit_op_tag);
		mOpTagText.setNextFocusDownId(R.id.edit_op_mode);
		mOpModeText.setNextFocusDownId(R.id.edit_op_notes);
	}

	protected void initViewAdapters() {
		mOpThirdPartyText
				.setAdapter(new InfoAdapter(this, mDbHelper,
						CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
						CommonDbAdapter.KEY_THIRD_PARTY_NAME));
		mOpModeText.setAdapter(new InfoAdapter(this, mDbHelper,
				CommonDbAdapter.DATABASE_MODES_TABLE, CommonDbAdapter.KEY_MODE_NAME));
		mOpTagText.setAdapter(new InfoAdapter(this, mDbHelper, CommonDbAdapter.DATABASE_TAGS_TABLE,
				CommonDbAdapter.KEY_TAG_NAME));
	}

	private void invertSign() throws ParseException {
		mSumTextWatcher.setAutoNegate(false);
		Double sum = Formater.SUM_FORMAT.parse(mOpSumText.getText().toString()).doubleValue();
		if (sum != null) {
			sum = -sum;
		}
		mOpSumText.setText(Formater.SUM_FORMAT.format(sum));
	}

	protected boolean isFormValid(StringBuilder errMsg) {
		boolean res = true;
		String str = mOpThirdPartyText.getText().toString().trim();
		if (str.length() == 0) {
			if (errMsg.length() > 0) {
				errMsg.append("\n");
			}
			errMsg.append(getString(R.string.empty_third_party));
			res = false;
		}
		str = mOpSumText.getText().toString().replace('+', ' ').trim();
		if (str.length() == 0) {
			if (errMsg.length() > 0) {
				errMsg.append("\n");
			}
			errMsg.append(getString(R.string.empty_amount));
			res = false;
		} else {
			try {
				Formater.SUM_FORMAT.parse(str).doubleValue();
			} catch (ParseException e) {
				if (errMsg.length() > 0) {
					errMsg.append("\n");
				}
				errMsg.append(getString(R.string.invalid_amount));
				res = false;
			}
		}
		return res;
	}

	private InfoManager createInfoManagerIfNeeded(String table, String colName, String title,
			int editId, int deletiId) {
		InfoManager i = mInfoManagersMap.get(table);
		if (null == i) {
			i = new InfoManager(this, title, table, colName, editId, deletiId);
			mInfoManagersMap.put(table, i);
		}
		return i;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case THIRD_PARTIES_DIALOG_ID:
			return createInfoManagerIfNeeded(CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
					CommonDbAdapter.KEY_THIRD_PARTY_NAME, getString(R.string.third_parties),
					EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID).getListDialog();
		case TAGS_DIALOG_ID:
			return createInfoManagerIfNeeded(CommonDbAdapter.DATABASE_TAGS_TABLE,
					CommonDbAdapter.KEY_TAG_NAME, getString(R.string.tags), EDIT_TAG_DIALOG_ID,
					DELETE_TAG_DIALOG_ID).getListDialog();
		case MODES_DIALOG_ID:
			return createInfoManagerIfNeeded(CommonDbAdapter.DATABASE_MODES_TABLE,
					CommonDbAdapter.KEY_MODE_NAME, getString(R.string.modes), EDIT_MODE_DIALOG_ID,
					DELETE_MODE_DIALOG_ID).getListDialog();
		case EDIT_THIRD_PARTY_DIALOG_ID: {
			InfoManager i = createInfoManagerIfNeeded(CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
					CommonDbAdapter.KEY_THIRD_PARTY_NAME, getString(R.string.third_parties),
					EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		}
		case EDIT_TAG_DIALOG_ID: {
			InfoManager i = createInfoManagerIfNeeded(CommonDbAdapter.DATABASE_TAGS_TABLE,
					CommonDbAdapter.KEY_TAG_NAME, getString(R.string.tags), EDIT_TAG_DIALOG_ID,
					DELETE_TAG_DIALOG_ID);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		}
		case EDIT_MODE_DIALOG_ID: {
			InfoManager i = createInfoManagerIfNeeded(CommonDbAdapter.DATABASE_MODES_TABLE,
					CommonDbAdapter.KEY_MODE_NAME, getString(R.string.modes), EDIT_MODE_DIALOG_ID,
					DELETE_MODE_DIALOG_ID);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		}
		case DELETE_THIRD_PARTY_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							InfoManager i = createInfoManagerIfNeeded(
									CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
									CommonDbAdapter.KEY_THIRD_PARTY_NAME,
									getString(R.string.third_parties), EDIT_THIRD_PARTY_DIALOG_ID,
									DELETE_THIRD_PARTY_DIALOG_ID);
							i.deleteInfo();
						}
					});
		case DELETE_TAG_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							InfoManager i = createInfoManagerIfNeeded(
									CommonDbAdapter.DATABASE_TAGS_TABLE,
									CommonDbAdapter.KEY_TAG_NAME, getString(R.string.tags),
									EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID);
							i.deleteInfo();
						}
					});
		case DELETE_MODE_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							InfoManager i = createInfoManagerIfNeeded(
									CommonDbAdapter.DATABASE_MODES_TABLE,
									CommonDbAdapter.KEY_MODE_NAME, getString(R.string.modes),
									EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID);
							i.deleteInfo();
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
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE).onPrepareDialog(
					(AlertDialog) dialog);
			break;
		case TAGS_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_TAGS_TABLE).onPrepareDialog(
					(AlertDialog) dialog);
			break;
		case MODES_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_MODES_TABLE).onPrepareDialog(
					(AlertDialog) dialog);
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		initDbHelper();
		if (!mOnRestore) {
			fetchOrCreateCurrentOp();
			populateFields();
		} else {
			mOnRestore = false;
		}
		initViewAdapters();
		initListeners();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// mDbHelper.close();
	}

	protected void populateCommonFields(Operation op) {
		Tools.setTextWithoutComplete(mOpThirdPartyText, op.mThirdParty);
		Tools.setTextWithoutComplete(mOpModeText, op.mMode);
		Tools.setTextWithoutComplete(mOpTagText, op.mTag);
		mDatePicker.updateDate(op.getYear(), op.getMonth(), op.getDay());
		mPreviousSum = op.mSum;
		mNotesText.setText(op.mNotes);
		Tools.setSumTextGravity(mOpSumText);
		if (mCurrentOp.mSum == 0.0) {
			mOpSumText.setText("");
			mSumTextWatcher.setAutoNegate(true);
		} else {
			mOpSumText.setText(mCurrentOp.getSumStr());
		}
	}

	protected void initListeners() {
		mOpSumText.addTextChangedListener(mSumTextWatcher);
		findViewById(R.id.edit_op_third_parties_list).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDialog(THIRD_PARTIES_DIALOG_ID);
					}
				});

		findViewById(R.id.edit_op_tags_list).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(TAGS_DIALOG_ID);
			}
		});

		findViewById(R.id.edit_op_modes_list).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(MODES_DIALOG_ID);
			}
		});

		findViewById(R.id.edit_op_sign).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					invertSign();
				} catch (ParseException e) {
					// nothing to do
				}
			}
		});

		findViewById(R.id.cancel_op).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		findViewById(R.id.confirm_op).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				StringBuilder errMsg = new StringBuilder();

				if (isFormValid(errMsg)) {
					fillOperationWithInputs(mCurrentOp);
					saveOpAndExit();
				} else {
					Tools.popError(CommonOpEditor.this, errMsg.toString(), null);
				}
			}
		});
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (Tools.onKeyLongPress(keyCode, event, this)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	protected void fillOperationWithInputs(Operation op) {
		op.mThirdParty = mOpThirdPartyText.getText().toString().trim();
		op.mMode = mOpModeText.getText().toString().trim();
		op.mTag = mOpTagText.getText().toString().trim();
		op.setSumStr(mOpSumText.getText().toString());
		op.mNotes = mNotesText.getText().toString().trim();

		DatePicker dp = mDatePicker;
		dp.clearChildFocus(getCurrentFocus());
		op.setDay(dp.getDayOfMonth());
		op.setMonth(dp.getMonth());
		op.setYear(dp.getYear());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mRowId > 0) {
			outState.putLong("rowId", mRowId);
		}

		Operation op = mCurrentOp;
		fillOperationWithInputs(op);
		outState.putParcelable("currentOp", op);
		outState.putLong("previousSum", mPreviousSum);
		mOnRestore = true;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		long rowId = savedInstanceState.getLong("rowId");
		mRowId = rowId > 0 ? Long.valueOf(rowId) : 0;
		Operation op = savedInstanceState.getParcelable("currentOp");
		mCurrentOp = op;
		populateFields();
		mOnRestore = true;
		mPreviousSum = savedInstanceState.getLong("previousSum");
	}
}
