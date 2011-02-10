package fr.geobert.radis;

import android.text.Editable;
import android.text.TextWatcher;

public class CorrectCommaWatcher implements TextWatcher {
	private char mLocaleComma;
	private boolean mAutoNegate = false;

	public CorrectCommaWatcher(char localeComma) {
		mLocaleComma = localeComma;
	}

	@Override
	public void afterTextChanged(Editable s) {
		boolean haveComma = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == mLocaleComma) {
				if (haveComma) {
					s.replace(i, i + 1, "");
				} else {
					haveComma = true;
				}
			} else if (c == '.' || c == ',') {
				if (haveComma) {
					s.replace(i, i + 1, "");
				} else {
					s.replace(i, i + 1, String.valueOf(mLocaleComma));
					haveComma = true;
				}
			}
		}
		if (mAutoNegate) {
			if (s.length() > 0) {
				char c = s.charAt(0);
				if (c != '.' && c != ',' && c != '-') {
					mAutoNegate = false;
					s.insert(0, "-");
				}
			}
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	public void setAutoNegate(boolean b) {
		mAutoNegate = b;
	}

}
