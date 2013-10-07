package fr.geobert.radis.tools;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;

public class CorrectCommaWatcher implements TextWatcher {
    private char mLocaleComma;
    private boolean mAutoNegate = false;
    private EditText mEditText;

    public CorrectCommaWatcher(char localeComma, EditText w) {
        mLocaleComma = localeComma;
        mEditText = w;
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
        if (s.length() > 0) {
            char c = s.charAt(0);
            if (c == '+') {
                mAutoNegate = false;
            }
            if (mAutoNegate) {
                if (c != '.' && c != ',' && c != '-') {
                    mAutoNegate = false;
                    s.insert(0, "-");
                }
            }
            mEditText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        } else {
            mEditText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            mAutoNegate = true;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public CorrectCommaWatcher setAutoNegate(boolean b) {
        mAutoNegate = b;
        return this;
    }
}
