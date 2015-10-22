package fr.geobert.radis.tools

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText

public open class CorrectCommaWatcher : TextWatcher {
    private val mListener: TextWatcher?
    private var mLocaleComma: Char = ' '
    private var mAutoNegate = false
    private var mEditText: EditText? = null
    private var mAllowNegativeSum = true

    public constructor(localeComma: Char, w: EditText) {
        mLocaleComma = localeComma
        mEditText = w
        mListener = null
    }

    public constructor(localeComma: Char, w: EditText, listener: TextWatcher) {
        mLocaleComma = localeComma
        mEditText = w
        mListener = listener
    }

    protected fun correctComma(s: Editable) {
        var haveComma = false
        for (i in 0..s.length - 1) {
            val c = s[i]
            if (c == mLocaleComma) {
                if (haveComma) {
                    s.replace(i, i + 1, "")
                } else {
                    haveComma = true
                }
            } else if (c == '.' || c == ',') {
                if (haveComma) {
                    s.replace(i, i + 1, "")
                } else {
                    s.replace(i, i + 1, mLocaleComma.toString())
                    haveComma = true
                }
            }
        }
    }

    protected fun autoNegate(s: Editable) {
        if (s.length > 0) {
            val c = s[0]
            if (c == '+') {
                mAutoNegate = false
            }
            if (mAutoNegate && mAllowNegativeSum) {
                if (c != '.' && c != ',' && c != '-') {
                    mAutoNegate = false
                    s.insert(0, "-")
                }
            }
            if (!mAllowNegativeSum && c == '-') {
                s.replace(0, 1, "")
            }
            mEditText!!.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        } else {
            mEditText!!.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            mAutoNegate = true
        }
    }

    override fun afterTextChanged(s: Editable) {
        correctComma(s)
        autoNegate(s)
        mListener?.afterTextChanged(s)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int,
                                   after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    public fun setAutoNegate(b: Boolean): CorrectCommaWatcher {
        mAutoNegate = b
        return this
    }

    public fun setAllowNegativeSum(allowNegativeSum: Boolean) {
        this.mAllowNegativeSum = allowNegativeSum
    }
}
