package fr.geobert.radis.tools;

import fr.geobert.radis.QuickAddController;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

public class QuickAddTextWatcher implements TextWatcher {
	private AutoCompleteTextView mThirdParty;
	private EditText mAmount;
	private Button mQuickAdd;

	public QuickAddTextWatcher(AutoCompleteTextView thirdParty,
			EditText amount, Button quickAdd) {
		mThirdParty = thirdParty;
		mAmount = amount;
		mQuickAdd = quickAdd;
	}

	@Override
	public void afterTextChanged(Editable s) {
		EditText amount = mAmount;
		QuickAddController
				.setQuickAddButEnabled(
						mQuickAdd,
						((mThirdParty.length() != 0) && (amount.length() != 0) && !(amount
								.length() == 1 && amount.getText().charAt(0) == '-')));
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

}
