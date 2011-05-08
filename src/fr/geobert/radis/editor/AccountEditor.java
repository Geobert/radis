package fr.geobert.radis.editor;

import java.math.BigDecimal;
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
import fr.geobert.radis.R;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

public class AccountEditor extends Activity {
	private CommonDbAdapter mDbHelper;
	private EditText mAccountNameText;
	private EditText mAccountStartSumText;
	private EditText mAccountDescText;
	private Spinner mAccountCurrency;
	private Long mRowId;
	private ArrayAdapter<CharSequence> mCurrAdapter;

	private boolean mOnRestore = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}

		setContentView(R.layout.account_creation);
		setTitle(R.string.account_edit);
		mAccountNameText = (EditText) findViewById(R.id.edit_account_name);
		mAccountDescText = (EditText) findViewById(R.id.edit_account_desc);
		mAccountStartSumText = (EditText) findViewById(R.id.edit_account_start_sum);
		mAccountStartSumText.addTextChangedListener(new CorrectCommaWatcher(
				Formater.SUM_FORMAT.getDecimalFormatSymbols()
						.getDecimalSeparator(), mAccountStartSumText));
		mAccountStartSumText
				.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							((EditText) v).selectAll();
						}
					}
				});
		mAccountCurrency = (Spinner) findViewById(R.id.currency_spinner);
		Button confirmButton = (Button) findViewById(R.id.confirm_creation);
		Button cancelButton = (Button) findViewById(R.id.cancel_creation);

		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState
						.getSerializable(Tools.EXTRAS_ACCOUNT_ID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID)
					: null;
		}

		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				StringBuilder errMsg = new StringBuilder();
				if (isFormValid(errMsg)) {
					setResult(RESULT_OK);
					saveState();
					finish();
					AccountEditor.this.overridePendingTransition(
							R.anim.enter_from_right, 0);
				} else {
					Tools.popError(AccountEditor.this, errMsg.toString(), null);
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
				AccountEditor.this.overridePendingTransition(
						R.anim.enter_from_right, 0);
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
			errMsg.append(getString(R.string.no_start_amount));
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
			mAccountNameText.setText(account.getString(account
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_NAME)));
			mAccountDescText.setText(account.getString(account
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_DESC)));
			mAccountStartSumText
					.setText(Formater.SUM_FORMAT.format(account.getLong(account
							.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_START_SUM)) / 100.0d));
			String currencyStr = account
					.getString(account
							.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_CURRENCY));
			if (currencyStr.length() == 0) {
				currencyStr = Currency.getInstance(Locale.getDefault())
						.getCurrencyCode();
			}
			int pos = Arrays.binarySearch(allCurrencies, currencyStr);
			mAccountCurrency.setSelection(pos);
		} else {
			int pos = Arrays
					.binarySearch(allCurrencies,
							Currency.getInstance(Locale.getDefault())
									.getCurrencyCode());
			mAccountCurrency.setSelection(pos);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("name", mAccountNameText.getText().toString());
		outState.putString("startSum", mAccountStartSumText.getText()
				.toString());
		outState.putInt("currency", mAccountCurrency.getSelectedItemPosition());
		outState.putString("desc", mAccountDescText.getText().toString());
		mOnRestore = true;
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mAccountNameText.setText(state.getString("name"));
		mAccountStartSumText.setText(state.getString("startSum"));
		mAccountCurrency.setSelection(state.getInt("currency"));
		mAccountDescText.setText(state.getString("desc"));
		mOnRestore = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper = CommonDbAdapter.getInstance(this);
		mDbHelper.open();
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
			long startSum = Math
					.round(Formater.SUM_FORMAT.parse(
							mAccountStartSumText.getText().toString())
							.doubleValue() * 100);
			String currency = mAccountCurrency.getSelectedItem().toString();
			if (mRowId == null) {
				long id = mDbHelper.createAccount(name, desc, startSum,
						currency);
				if (id > 0) {
					mRowId = id;
				}
			} else {
				mDbHelper.updateAccount(mRowId, name, desc, startSum, currency);
				mDbHelper.updateCurrentSum(mRowId, 0);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
