package fr.geobert.Radis;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class AccountEditor extends Activity {
	private AccountsDbAdapter mDbHelper;
	private EditText mAccountNameText;
	private EditText mAccountStartSumText;
	private EditText mAccountDescText;
	private Spinner mAccountCurrency;
	private Long mRowId;
	private ArrayAdapter<CharSequence> mCurrAdapter;
	private DecimalFormat mFormatSum;

	// to let inner class access to the context
	private AccountEditor context = this; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFormatSum = new DecimalFormat("0.00");
		mDbHelper = new AccountsDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.account_creation);

		mAccountNameText = (EditText) findViewById(R.id.edit_account_name);
		mAccountDescText = (EditText) findViewById(R.id.edit_account_desc);
		mAccountStartSumText = (EditText) findViewById(R.id.edit_account_start_sum);
		mAccountCurrency = (Spinner) findViewById(R.id.currency_spinner);
		Button confirmButton = (Button) findViewById(R.id.confirm_creation);
		Button cancelButton = (Button) findViewById(R.id.cancel_creation);

		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState
						.getSerializable(Tools.EXTRAS_ACCOUNT_ID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras
					.getLong(Tools.EXTRAS_ACCOUNT_ID) : null;
		}

		populateFields();

		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				StringBuilder errMsg = new StringBuilder();
				if (isFormValid(errMsg)) {
					setResult(RESULT_OK);
					saveState();
					finish();
				} else {
					Tools.getInstance(context).popError(errMsg.toString());
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		fillCurrencySpinner();
	}

	private void fillCurrencySpinner() {
		mCurrAdapter = ArrayAdapter.createFromResource(this,
				R.array.all_currencies, android.R.layout.simple_spinner_item);
		mAccountCurrency.setAdapter(mCurrAdapter);
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
			if (errMsg.length() > 0)
				errMsg.append("\n");
			errMsg.append("Somme de départ vide");
			res = false;
		}
		return res;
	}

	private void populateFields() {
		Resources res = getResources();
		String[] allCurrencies = res.getStringArray(R.array.all_currencies);
		if (mRowId != null) {
			Cursor account = mDbHelper.fetchAccount(mRowId);
			startManagingCursor(account);
			mAccountNameText
					.setText(account
							.getString(account
									.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_NAME)));
			mAccountDescText
					.setText(account
							.getString(account
									.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_DESC)));
			mAccountStartSumText
					.setText(mFormatSum
							.format(account
									.getDouble(account
											.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_START_SUM))));
			String currencyStr = account
					.getString(account
							.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_CURRENCY));
			if (currencyStr.length() == 0) {
				currencyStr = Currency.getInstance(Locale.getDefault())
						.getCurrencyCode();
			}
			int pos = Arrays.binarySearch(allCurrencies, currencyStr);
			mAccountCurrency.setSelection(pos);
		} else {
			mAccountStartSumText.setText(mFormatSum.format(0.00));
			int pos = Arrays.binarySearch(allCurrencies, Currency.getInstance(
					Locale.getDefault()).getCurrencyCode());
			mAccountCurrency.setSelection(pos);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
//		StringBuilder errMsg = new StringBuilder();
//		if (isFormValid(errMsg)) {
//			saveState();
//			outState.putSerializable(AccountsDbAdapter.KEY_ACCOUNT_ROWID,
//					mRowId);
//		}
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

	private void saveState() {
		String name = mAccountNameText.getText().toString();
		String desc = mAccountDescText.getText().toString();
		double startSum = 0;
		try {
			startSum = mFormatSum.parse(mAccountStartSumText.getText()
					.toString()).doubleValue();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String currency = mAccountCurrency.getSelectedItem().toString();
		if (mRowId == null) {
			long id = mDbHelper.createAccount(name, desc, startSum, currency);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateAccount(mRowId, name, desc, startSum, currency);
			mDbHelper.updateCurrentSum(mRowId);
		}
	}
}
