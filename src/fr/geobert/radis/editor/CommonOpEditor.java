package fr.geobert.radis.editor;

import java.text.ParseException;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import fr.geobert.radis.Account;
import fr.geobert.radis.AccountList;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.InfoAdapter;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.MyAutoCompleteTextView;
import fr.geobert.radis.tools.Tools;

public abstract class CommonOpEditor extends BaseActivity implements
		LoaderCallbacks<Cursor> {
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
	protected AutoCompleteTextView mOpThirdPartyText;
	protected EditText mOpSumText;
	protected AutoCompleteTextView mOpModeText;
	protected AutoCompleteTextView mOpTagText;
	protected DatePicker mDatePicker;
	protected Spinner mSrcAccount;
	protected Spinner mDstAccount;
	protected EditText mNotesText;
	protected long mRowId;

	protected HashMap<String, InfoManager> mInfoManagersMap;
	protected CorrectCommaWatcher mSumTextWatcher;
	protected boolean mOnRestore = false;
	public Uri mCurrentInfoTable;
	protected long mPreviousSum = 0L;

	protected Long mCurAccountId;
	private LinearLayout mThirdPartyCont;
	private LinearLayout mTransfertCont;
	protected CheckBox mIsTransfertCheck;

	// abstract methods
	protected abstract void setView();

	protected abstract void populateFields();

	protected abstract void fetchOrCreateCurrentOp();

	protected void fetchOp(int loaderId) {
		showProgress();
		getSupportLoaderManager().initLoader(loaderId, null, this);
	}

	// default and common behaviors
	protected void saveOpAndExit() {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		mCurAccountId = extras != null ? extras
				.getLong(Tools.EXTRAS_ACCOUNT_ID) : null;
		setView();
		init(savedInstanceState);
	}

	protected void init(Bundle savedInstanceState) {
		Bundle extras = getIntent().getExtras();
		mRowId = extras != null ? extras.getLong(Tools.EXTRAS_OP_ID) : 0;

		mOpThirdPartyText = (MyAutoCompleteTextView) findViewById(R.id.edit_op_third_party);
		mOpModeText = (MyAutoCompleteTextView) findViewById(R.id.edit_op_mode);
		mOpSumText = (EditText) findViewById(R.id.edit_op_sum);
		mOpTagText = (MyAutoCompleteTextView) findViewById(R.id.edit_op_tag);
		mSumTextWatcher = new CorrectCommaWatcher(Formater.getSumFormater()
				.getDecimalFormatSymbols().getDecimalSeparator(), mOpSumText);
		mDatePicker = (DatePicker) findViewById(R.id.edit_op_date);
		mSrcAccount = (Spinner) findViewById(R.id.trans_src_account);
		mDstAccount = (Spinner) findViewById(R.id.trans_dst_account);
		mTransfertCont = (LinearLayout) findViewById(R.id.transfert_cont);
		mThirdPartyCont = (LinearLayout) findViewById(R.id.third_party_cont);
		mNotesText = (EditText) findViewById(R.id.edit_op_notes);
		mInfoManagersMap = new HashMap<String, InfoManager>();

		mOpThirdPartyText.setNextFocusDownId(R.id.edit_op_sum);
		mOpSumText.setNextFocusDownId(R.id.edit_op_tag);
		mOpTagText.setNextFocusDownId(R.id.edit_op_mode);
		mOpModeText.setNextFocusDownId(R.id.edit_op_notes);

		mIsTransfertCheck = (CheckBox) findViewById(R.id.is_transfert);
		mIsTransfertCheck
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						onTransfertCheckedChanged(arg1);
					}
				});
		mTransfertCont.setVisibility(View.GONE);
	}

	protected void onTransfertCheckedChanged(boolean arg1) {
		Animation in = AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in);
		Animation out = AnimationUtils.makeOutAnimation(this, true);
		if (arg1 == true) {
			mTransfertCont.startAnimation(in);
			mThirdPartyCont.startAnimation(out);
			mTransfertCont.setVisibility(View.VISIBLE);
			mThirdPartyCont.setVisibility(View.GONE);
		} else {
			mTransfertCont.startAnimation(out);
			mThirdPartyCont.startAnimation(in);
			mTransfertCont.setVisibility(View.GONE);
			mThirdPartyCont.setVisibility(View.VISIBLE);
		}
	}

	protected void initViewAdapters() {
		mOpThirdPartyText.setAdapter(new InfoAdapter(this,
				DbContentProvider.THIRD_PARTY_URI,
				InfoTables.KEY_THIRD_PARTY_NAME));
		mOpModeText.setAdapter(new InfoAdapter(this,
				DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME));
		mOpTagText.setAdapter(new InfoAdapter(this, DbContentProvider.TAGS_URI,
				InfoTables.KEY_TAG_NAME));
	}

	final protected void populateTransfertSpinner(Cursor c) {
		if (c != null && c.moveToFirst()) {
			ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this,
					android.R.layout.simple_spinner_item);
			ArrayAdapter<Account> adapter2 = new ArrayAdapter<Account>(this,
					android.R.layout.simple_spinner_item);
			adapter.add(new Account(0, getString(R.string.no_transfert)));
			adapter2.add(new Account(0, getString(R.string.no_transfert)));
			do {
				adapter.add(new Account(c));
				adapter2.add(new Account(c));
			} while (c.moveToNext());

			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSrcAccount.setAdapter(adapter);
			mDstAccount.setAdapter(adapter2);
		}

		final boolean isTransfert = mCurrentOp.mTransferAccountId > 0;
		mIsTransfertCheck.setChecked(isTransfert);
		if (isTransfert) {
			initAccountSpinner(mSrcAccount, mCurrentOp.mAccountId);
			initAccountSpinner(mDstAccount, mCurrentOp.mTransferAccountId);
		} else {
			if (mCurAccountId != 0) {
				initAccountSpinner(mSrcAccount, mCurAccountId);
			}
		}

	}

	private void invertSign() throws ParseException {
		mSumTextWatcher.setAutoNegate(false);
		Double sum = Formater.getSumFormater()
				.parse(mOpSumText.getText().toString()).doubleValue();
		if (sum != null) {
			sum = -sum;
		}
		mOpSumText.setText(Formater.getSumFormater().format(sum));
	}

	protected boolean isFormValid(StringBuilder errMsg) {
		boolean res = true;
		String str;
		if (mIsTransfertCheck.isChecked()) {
			final Account srcAccount = (Account) mSrcAccount.getSelectedItem();
			final Account dstAccount = (Account) mDstAccount.getSelectedItem();
			if (srcAccount.mAccountId == 0) {
				errMsg.append(getString(R.string.err_transfert_no_src));
				res = false;
			} else if (dstAccount.mAccountId == 0) {
				errMsg.append(getString(R.string.err_transfert_no_dst));
				res = false;
			} else  if (srcAccount.mAccountId > 0 && dstAccount.mAccountId > 0 && srcAccount.mAccountId == dstAccount.mAccountId) {
				errMsg.append(getString(R.string.err_transfert_same_acc));
				res = false;
			}
		} else {
			str = mOpThirdPartyText.getText().toString().trim();
			if (str.length() == 0) {
				if (errMsg.length() > 0) {
					errMsg.append("\n");
				}
				errMsg.append(getString(R.string.empty_third_party));
				res = false;
			}
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
				Formater.getSumFormater().parse(str).doubleValue();
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

	private InfoManager createInfoManagerIfNeeded(Uri table, String colName,
			String title, int editId, int deletiId) {
		InfoManager i = mInfoManagersMap.get(table.toString());
		if (null == i) {
			i = new InfoManager(this, title, table, colName, editId, deletiId);
			mInfoManagersMap.put(table.toString(), i);
		}
		return i;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case THIRD_PARTIES_DIALOG_ID:
			return createInfoManagerIfNeeded(DbContentProvider.THIRD_PARTY_URI,
					InfoTables.KEY_THIRD_PARTY_NAME,
					getString(R.string.third_parties),
					EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID)
					.getListDialog();
		case TAGS_DIALOG_ID:
			return createInfoManagerIfNeeded(DbContentProvider.TAGS_URI,
					InfoTables.KEY_TAG_NAME, getString(R.string.tags),
					EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID).getListDialog();
		case MODES_DIALOG_ID:
			return createInfoManagerIfNeeded(DbContentProvider.MODES_URI,
					InfoTables.KEY_MODE_NAME, getString(R.string.modes),
					EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID).getListDialog();
		case EDIT_THIRD_PARTY_DIALOG_ID: {
			InfoManager i = createInfoManagerIfNeeded(
					DbContentProvider.THIRD_PARTY_URI,
					InfoTables.KEY_THIRD_PARTY_NAME,
					getString(R.string.third_parties),
					EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		}
		case EDIT_TAG_DIALOG_ID: {
			InfoManager i = createInfoManagerIfNeeded(
					DbContentProvider.TAGS_URI, InfoTables.KEY_TAG_NAME,
					getString(R.string.tags), EDIT_TAG_DIALOG_ID,
					DELETE_TAG_DIALOG_ID);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		}
		case EDIT_MODE_DIALOG_ID: {
			InfoManager i = createInfoManagerIfNeeded(
					DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME,
					getString(R.string.modes), EDIT_MODE_DIALOG_ID,
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
									DbContentProvider.THIRD_PARTY_URI,
									InfoTables.KEY_THIRD_PARTY_NAME,
									getString(R.string.third_parties),
									EDIT_THIRD_PARTY_DIALOG_ID,
									DELETE_THIRD_PARTY_DIALOG_ID);
							i.deleteInfo();
						}
					});
		case DELETE_TAG_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							InfoManager i = createInfoManagerIfNeeded(
									DbContentProvider.TAGS_URI,
									InfoTables.KEY_TAG_NAME,
									getString(R.string.tags),
									EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID);
							i.deleteInfo();
						}
					});
		case DELETE_MODE_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							InfoManager i = createInfoManagerIfNeeded(
									DbContentProvider.MODES_URI,
									InfoTables.KEY_MODE_NAME,
									getString(R.string.modes),
									EDIT_MODE_DIALOG_ID, DELETE_MODE_DIALOG_ID);
							i.deleteInfo();
						}
					});
		default:
			return Tools.onDefaultCreateDialog(this, id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case EDIT_THIRD_PARTY_DIALOG_ID:
		case EDIT_TAG_DIALOG_ID:
		case EDIT_MODE_DIALOG_ID:
			mInfoManagersMap.get(mCurrentInfoTable.toString()).initEditDialog(dialog);
			break;
		case THIRD_PARTIES_DIALOG_ID:
			mInfoManagersMap.get(DbContentProvider.THIRD_PARTY_URI.toString())
					.onPrepareDialog((AlertDialog) dialog);
			break;
		case TAGS_DIALOG_ID:
			mInfoManagersMap.get(DbContentProvider.TAGS_URI.toString())
					.onPrepareDialog((AlertDialog) dialog);
			break;
		case MODES_DIALOG_ID:
			mInfoManagersMap.get(DbContentProvider.MODES_URI.toString())
					.onPrepareDialog((AlertDialog) dialog);
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mOnRestore) {
			fetchOrCreateCurrentOp();
		} else {
			mOnRestore = false;
		}
		initViewAdapters();
		initListeners();
	}

	private void initAccountSpinner(Spinner spin, long accountId) {
		int pos = 0;
		SpinnerAdapter adapter = spin.getAdapter();
		while (pos < adapter.getCount()) {
			long id = adapter.getItemId(pos);
			if (id == accountId) {
				spin.setSelection(pos);
				break;
			} else {
				pos++;
			}
		}
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
//		getSupportLoaderManager().initLoader(GET_ALL_ACCOUNTS, null, this);
		populateTransfertSpinner(AccountList.getAllAccounts(this));
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

		findViewById(R.id.edit_op_tags_list).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDialog(TAGS_DIALOG_ID);
					}
				});

		findViewById(R.id.edit_op_modes_list).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDialog(MODES_DIALOG_ID);
					}
				});

		findViewById(R.id.edit_op_sign).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							invertSign();
						} catch (ParseException e) {
							// nothing to do
						}
					}
				});

		findViewById(R.id.cancel_op).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						setResult(RESULT_CANCELED);
						finish();
					}
				});

		findViewById(R.id.confirm_op).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						StringBuilder errMsg = new StringBuilder();

						if (isFormValid(errMsg)) {
							fillOperationWithInputs(mCurrentOp);
							saveOpAndExit();
						} else {
							Tools.popError(CommonOpEditor.this,
									errMsg.toString(), null);
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
		op.mMode = mOpModeText.getText().toString().trim();
		op.mTag = mOpTagText.getText().toString().trim();
		op.setSumStr(mOpSumText.getText().toString());
		op.mNotes = mNotesText.getText().toString().trim();

		DatePicker dp = mDatePicker;
		dp.clearChildFocus(getCurrentFocus());
		op.setDay(dp.getDayOfMonth());
		op.setMonth(dp.getMonth());
		op.setYear(dp.getYear());

		if (mIsTransfertCheck.isChecked()) {
			final Account srcAccount = (Account) mSrcAccount.getSelectedItem();
			final Account dstAccount = (Account) mDstAccount.getSelectedItem();
			if (srcAccount.mAccountId > 0 && dstAccount.mAccountId > 0
					&& srcAccount.mAccountId != dstAccount.mAccountId) {
				// a valid transfert has been setup
				op.mTransferAccountId = dstAccount.mAccountId;
				op.mAccountId = srcAccount.mAccountId;
				op.mThirdParty = dstAccount.mName.trim();
				op.mTransSrcAccName = srcAccount.mName;
			} else {
				op.mThirdParty = mOpThirdPartyText.getText().toString().trim();
			}
		} else {
			op.mTransferAccountId = 0;
			op.mTransSrcAccName = "";
			op.mThirdParty = mOpThirdPartyText.getText().toString().trim();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mRowId > 0) {
			outState.putLong(Tools.EXTRAS_OP_ID, mRowId);
		}

		Operation op = mCurrentOp;
		fillOperationWithInputs(op);
		outState.putParcelable("currentOp", op);
		outState.putLong("previousSum", mPreviousSum);
		outState.putParcelable("mCurrentInfoTable", mCurrentInfoTable);
		mOnRestore = true;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		mOnRestore = true;
		long rowId = savedInstanceState.getLong(Tools.EXTRAS_OP_ID);
		mRowId = rowId > 0 ? Long.valueOf(rowId) : 0;
		Operation op = savedInstanceState.getParcelable("currentOp");
		mCurrentOp = op;
		mCurrentInfoTable = (Uri)savedInstanceState.getParcelable("mCurrentInfoTable");
		populateFields();
		mPreviousSum = savedInstanceState.getLong("previousSum");
	}
}
