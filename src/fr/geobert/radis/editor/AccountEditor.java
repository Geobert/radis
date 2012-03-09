package fr.geobert.radis.editor;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import fr.geobert.radis.R;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.ProjectionDateController;
import fr.geobert.radis.tools.Tools;

public class AccountEditor extends Activity {
	private CommonDbAdapter mDbHelper;
	private EditText mAccountNameText;
	private EditText mAccountStartSumText;
	private EditText mAccountDescText;
	private Spinner mAccountCurrency;
	private EditText mCustomCurrency;
	private ProjectionDateController mProjectionController;
	private Long mRowId;
	private ArrayAdapter<CharSequence> mCurrAdapter;
	private int customCurrencyIdx;

	private boolean mOnRestore = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init(this);
		}

		setContentView(R.layout.account_creation);
		setTitle(R.string.account_edit);
		mAccountNameText = (EditText) findViewById(R.id.edit_account_name);
		mAccountDescText = (EditText) findViewById(R.id.edit_account_desc);
		mAccountStartSumText = (EditText) findViewById(R.id.edit_account_start_sum);
		mAccountStartSumText.addTextChangedListener(new CorrectCommaWatcher(Formater.SUM_FORMAT
				.getDecimalFormatSymbols().getDecimalSeparator(), mAccountStartSumText));
		mAccountStartSumText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					((EditText) v).selectAll();
				}
			}
		});
		mAccountCurrency = (Spinner) findViewById(R.id.currency_spinner);
		mCustomCurrency = (EditText) findViewById(R.id.custom_currency);
		Button confirmButton = (Button) findViewById(R.id.confirm_creation);
		Button cancelButton = (Button) findViewById(R.id.cancel_creation);

		mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState
				.getSerializable(Tools.EXTRAS_ACCOUNT_ID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID) : null;
		}

		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				StringBuilder errMsg = new StringBuilder();
				if (isFormValid(errMsg)) {
					setResult(RESULT_OK);
					saveState();
					finish();
					AccountEditor.this.overridePendingTransition(R.anim.enter_from_right, 0);
				} else {
					Tools.popError(AccountEditor.this, errMsg.toString(), null);
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
				AccountEditor.this.overridePendingTransition(R.anim.enter_from_right, 0);
			}
		});

		fillCurrencySpinner();
		mProjectionController = new ProjectionDateController(this);
	}

	private void fillCurrencySpinner() {
		mCurrAdapter = ArrayAdapter.createFromResource(this, R.array.all_currencies,
				android.R.layout.simple_spinner_item);
		mCurrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountCurrency.setAdapter(mCurrAdapter);
		mAccountCurrency.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
				mCustomCurrency.setEnabled(pos == customCurrencyIdx);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});
	}

	private boolean isFormValid(StringBuilder errMsg) {
		String name = mAccountNameText.getText().toString();
		String startSumStr = mAccountStartSumText.getText().toString();
		boolean res = true;
		if (name == null || name.length() == 0) {
			errMsg.append("Nom de compte vide");
			res = false;
		}
		if (startSumStr.length() == 0) {
			mAccountStartSumText.setText("0");
		}
		// check if currency is correct
		if (mAccountCurrency.getSelectedItemPosition() == customCurrencyIdx) {
			String currency = mCustomCurrency.getText().toString().trim().toUpperCase();
			if (currency.length() == 0 || currency.length() > 3) {
				if (errMsg.length() > 0)
					errMsg.append("\n");
				errMsg.append(getString(R.string.bad_format_for_currency));
			} else {
				try {
					Currency.getInstance(currency);
				} catch (IllegalArgumentException e) {
					if (errMsg.length() > 0)
						errMsg.append("\n");
					errMsg.append(getString(R.string.bad_format_for_currency));
					res = false;
				}
			}
		}
		// check projection date format
		if (mProjectionController.mProjectionDate.isEnabled()
				&& mProjectionController.getDate().trim().length() == 0) {
			if (errMsg.length() > 0)
				errMsg.append("\n");
			errMsg.append(getString(R.string.bad_format_for_date));
			res = false;
		}
		return res;
	}

	private void populateFields() {
		Resources res = getResources();
		String[] allCurrencies = res.getStringArray(R.array.all_currencies);
		customCurrencyIdx = allCurrencies.length - 1;
		if (mRowId != null) {
			Cursor account = mDbHelper.fetchAccount(mRowId);
			startManagingCursor(account);
			mAccountNameText.setText(account.getString(account
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_NAME)));
			mAccountDescText.setText(account.getString(account
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_DESC)));
			mAccountStartSumText.setText(Formater.SUM_FORMAT.format(account.getLong(account
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_START_SUM)) / 100.0d));
			String currencyStr = account.getString(account
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_CURRENCY));
			if (currencyStr.length() == 0) {
				currencyStr = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
			}
			int pos = Arrays.binarySearch(allCurrencies, currencyStr);
			if (pos >= 0) {
				mAccountCurrency.setSelection(pos);
				mCustomCurrency.setEnabled(false);
			} else {
				mAccountCurrency.setSelection(customCurrencyIdx);
				mCustomCurrency.setEnabled(true);
			}
			mProjectionController.populateFields(account);
		} else {
			int pos = Arrays.binarySearch(allCurrencies, Currency.getInstance(Locale.getDefault())
					.getCurrencyCode());
			mAccountCurrency.setSelection(pos);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("name", mAccountNameText.getText().toString());
		outState.putString("startSum", mAccountStartSumText.getText().toString());
		outState.putInt("currency", mAccountCurrency.getSelectedItemPosition());
		outState.putInt("customCurrencyIdx", customCurrencyIdx);
		if (mAccountCurrency.getSelectedItemPosition() == customCurrencyIdx) {
			outState.putString("customCurrency", mCustomCurrency.getText().toString());
		}
		outState.putString("desc", mAccountDescText.getText().toString());
		mProjectionController.onSaveInstanceState(outState);
		mOnRestore = true;
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mAccountNameText.setText(state.getString("name"));
		mAccountStartSumText.setText(state.getString("startSum"));
		mAccountCurrency.setSelection(state.getInt("currency"));
		customCurrencyIdx = state.getInt("customCurrencyIdx");
		if (mAccountCurrency.getSelectedItemPosition() == customCurrencyIdx) {
			mCustomCurrency.setText(state.getString("customCurrency"));
			mCustomCurrency.setEnabled(true);
		} else {
			mCustomCurrency.setEnabled(false);
		}
		mAccountDescText.setText(state.getString("desc"));
		mProjectionController.onRestoreInstanceState(state);
		mOnRestore = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper = CommonDbAdapter.getInstance(this);
		mDbHelper.open();
		mProjectionController.setDbHelper(mDbHelper);
		if (!mOnRestore) {
			populateFields();
		} else {
			mOnRestore = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// mDbHelper.close();
	}

	private void saveState() {
		String name = mAccountNameText.getText().toString().trim();
		String desc = mAccountDescText.getText().toString().trim();
		try {
			long startSum = Math.round(Formater.SUM_FORMAT.parse(
					mAccountStartSumText.getText().toString().trim()).doubleValue() * 100);
			String currency = null;
			if (mAccountCurrency.getSelectedItemPosition() == customCurrencyIdx) {
				currency = mCustomCurrency.getText().toString().trim().toUpperCase();
			} else {
				currency = mAccountCurrency.getSelectedItem().toString();
			}
			if (mRowId == null) {
				long id = mDbHelper.createAccount(name, desc, startSum, currency,
						mProjectionController.getMode(), mProjectionController.getDate());
				if (id > 0) {
					mRowId = id;
				}
			} else {
				mDbHelper.updateAccount(mRowId, name, desc, startSum, currency,
						mProjectionController);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
