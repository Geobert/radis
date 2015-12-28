package fr.geobert.radis.tools

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import com.crashlytics.android.Crashlytics
import fr.geobert.radis.BuildConfig

public open class CorrectCommaWatcher : TextWatcher {
    private val mListener: TextWatcher?
    private var mLocaleComma: Char = ' '
    private var mLocaleGroupSep: Char = ' '
    private var mAutoNegate = false
    private var mEditText: EditText? = null
    private var mAllowNegativeSum = true
    public var mEnable = true

    public constructor(localeComma: Char, localeGroupSep: Char, w: EditText) {
        mLocaleComma = localeComma
        mLocaleGroupSep = localeGroupSep
        mEditText = w
        mListener = null
    }

    public constructor(localeComma: Char, localeGroupSep: Char, w: EditText, listener: TextWatcher) {
        mLocaleComma = localeComma
        mLocaleGroupSep = localeGroupSep
        mEditText = w
        mListener = listener
    }

    protected fun correctComma(s: Editable) {
        try {
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
                    if (c != mLocaleGroupSep) {
                        if (haveComma) {
                            s.replace(i, i + 1, "")
                        } else {
                            s.replace(i, i + 1, mLocaleComma.toString())
                            haveComma = true
                        }
                    }
                }
            }
            if (!BuildConfig.DEBUG)
                Crashlytics.log("haveComma : $haveComma")
        } catch(e: IndexOutOfBoundsException) {
            if (!BuildConfig.DEBUG) {
                Crashlytics.setString("mLocaleComma", mLocaleComma.toString())
                Crashlytics.setString("mLocaleGroupSep", mLocaleGroupSep.toString())
            }
            throw e
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
        if (mEnable) {
            correctComma(s)
            autoNegate(s)
            mListener?.afterTextChanged(s)
        }
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
