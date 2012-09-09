package fr.geobert.radis.tools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import fr.geobert.radis.InfoAdapter;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.service.RadisService;

public class QuickAddController {
	private MyAutoCompleteTextView mQuickAddThirdParty;
	private EditText mQuickAddAmount;
	private ImageButton mQuickAddButton;
	private QuickAddTextWatcher mQuickAddTextWatcher;
	private CorrectCommaWatcher mCorrectCommaWatcher;
	private Activity mActivity;
	private UpdateDisplayInterface mProtocol;
	private long mAccountId = 0;

	public QuickAddController(Activity activity, UpdateDisplayInterface protocol) {
		mActivity = activity;

		mProtocol = protocol;
		mQuickAddThirdParty = (MyAutoCompleteTextView) activity
				.findViewById(R.id.quickadd_third_party);
		mQuickAddAmount = (EditText) activity
				.findViewById(R.id.quickadd_amount);
		mQuickAddButton = (ImageButton) activity
				.findViewById(R.id.quickadd_validate);
		mQuickAddThirdParty.setNextFocusDownId(R.id.quickadd_amount);
		mCorrectCommaWatcher = new CorrectCommaWatcher(Formater
				.getSumFormater().getDecimalFormatSymbols()
				.getDecimalSeparator(), mQuickAddAmount).setAutoNegate(true);

		mQuickAddTextWatcher = new QuickAddTextWatcher(mQuickAddThirdParty,
				mQuickAddAmount, mQuickAddButton);
	}

	public void setAccount(long accountId) {
		mAccountId = accountId;
		setEnabled(accountId != 0);
	}

	public void initViewBehavior() {
		mQuickAddThirdParty.setAdapter(new InfoAdapter(mActivity,
				DbContentProvider.THIRD_PARTY_URI,
				InfoTables.KEY_THIRD_PARTY_NAME));

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
		mQuickAddAmount.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				try {
					if (mQuickAddButton.isEnabled()) {
						quickAddOp();
					} else {
						Tools.popError(
								mActivity,
								mActivity
										.getString(R.string.quickadd_fields_not_filled),
								null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		});
	}

	private void quickAddOp() throws Exception {
		Operation op = new Operation();
		op.mThirdParty = mQuickAddThirdParty.getText().toString();
		op.setSumStr(mQuickAddAmount.getText().toString());
		assert (mAccountId != 0);
		if (OperationTable.createOp(mActivity, op, mAccountId)) {
			RadisService.updateAccountSum(op.mSum, mAccountId, op.getDate(),
					mActivity);
			mProtocol.updateDisplay(null);
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
		InputMethodManager imm = (InputMethodManager) mActivity
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mQuickAddThirdParty.getWindowToken(), 0);
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

	public static void setQuickAddButEnabled(ImageButton but, boolean b) {
		but.setEnabled(b);
		int drawable;
		if (b) {
			drawable = R.drawable.btn_check_buttonless_on;
		} else {
			drawable = R.drawable.btn_check_buttonless_off;
		}
		but.setImageResource(drawable);
	}

	public void setVisibility(int visibility) {
		mActivity.findViewById(R.id.quick_add_layout).setVisibility(visibility);
	}
}
