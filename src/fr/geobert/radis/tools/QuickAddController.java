package fr.geobert.radis.tools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import fr.geobert.radis.InfoAdapter;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.R.drawable;
import fr.geobert.radis.R.id;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.service.RadisService;

public class QuickAddController {
	private MyAutoCompleteTextView mQuickAddThirdParty;
	private EditText mQuickAddAmount;
	private Button mQuickAddButton;
	private QuickAddTextWatcher mQuickAddTextWatcher;
	private CorrectCommaWatcher mCorrectCommaWatcher;
	private Activity mActivity;
	private CommonDbAdapter mDbHelper;
	private QuickAddInterface mProtocol;
	private long mAccountId = 0;

	public QuickAddController(Activity activity, QuickAddInterface protocol) {
		mActivity = activity;

		mProtocol = protocol;
		mQuickAddThirdParty = (MyAutoCompleteTextView) activity
				.findViewById(R.id.quickadd_third_party);
		mQuickAddAmount = (EditText) activity
				.findViewById(R.id.quickadd_amount);
		mQuickAddButton = (Button) activity
				.findViewById(R.id.quickadd_validate);
		mQuickAddThirdParty.setNextFocusDownId(R.id.quickadd_amount);
		mCorrectCommaWatcher = new CorrectCommaWatcher(Formater.SUM_FORMAT
				.getDecimalFormatSymbols().getDecimalSeparator(),
				mQuickAddAmount).setAutoNegate(true);

		mQuickAddTextWatcher = new QuickAddTextWatcher(mQuickAddThirdParty,
				mQuickAddAmount, mQuickAddButton);
	}

	public void setDbHelper(CommonDbAdapter dbHelper) {
		mDbHelper = dbHelper;
	}

	public void setAccount(long accountId) {
		mAccountId = accountId;
		setEnabled(accountId != 0);
	}

	public void initViewBehavior() {
		mQuickAddThirdParty.setAdapter(new InfoAdapter(mActivity, mDbHelper,
				CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
				CommonDbAdapter.KEY_THIRD_PARTY_NAME));

		mQuickAddAmount.addTextChangedListener(mCorrectCommaWatcher);

		mQuickAddButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					quickAddOp();
				} catch (Exception e) {
					Tools.popError(mActivity, e.getMessage(), null);
					e.printStackTrace();
				}
			}
		});
		QuickAddController.setQuickAddButEnabled(mQuickAddButton, false);

		mQuickAddThirdParty.addTextChangedListener(mQuickAddTextWatcher);
		mQuickAddAmount.addTextChangedListener(mQuickAddTextWatcher);
	}

	private void quickAddOp() throws Exception {
		Operation op = new Operation();
		op.mThirdParty = mQuickAddThirdParty.getText().toString();
		op.setSumStr(mQuickAddAmount.getText().toString());
		assert (mAccountId != 0);
		if (mDbHelper.createOp(op, mAccountId)) {
			RadisService.updateAccountSum(op.mSum, mAccountId, op.getDate(),
					mDbHelper);
			mProtocol.updateSumsDisplay();
		}
		mQuickAddAmount.setText("");
		mQuickAddThirdParty.setText("");
		InputMethodManager mgr = (InputMethodManager) mActivity
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(mQuickAddAmount.getWindowToken(), 0);
		mCorrectCommaWatcher.setAutoNegate(true);
		mQuickAddAmount.clearFocus();
		mQuickAddThirdParty.clearFocus();
	}

	public void clearFocus() {
		mQuickAddThirdParty.clearFocus();
	}

	public void setAutoNegate(boolean autoNeg) {
		mCorrectCommaWatcher.setAutoNegate(autoNeg);
	}

	public void setEnabled(boolean enabled) {
		mQuickAddThirdParty.setEnabled(enabled);
		mQuickAddAmount.setEnabled(enabled);
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putCharSequence("third_party", mQuickAddThirdParty.getText());
		outState.putCharSequence("amount", mQuickAddAmount.getText());
	}

	public void onRestoreInstanceState(Bundle state) {
		mQuickAddThirdParty.setText(state.getCharSequence("third_party"));
		mQuickAddAmount.setText(state.getCharSequence("amount"));
	}

	public static void setQuickAddButEnabled(Button but, boolean b) {
		but.setEnabled(b);
		int drawable;
		if (b) {
			drawable = R.drawable.btn_check_buttonless_on;
		} else {
			drawable = R.drawable.btn_check_buttonless_off;
		}
		but.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
	}
}
