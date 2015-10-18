/*
 * Copyright 2012 David Cesarino de Sousa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.geobert.radis.ui

import android.R
import android.annotation.TargetApi
import android.app.Activity
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.DatePicker
import fr.geobert.radis.tools.TIME_ZONE
import hirondelle.date4j.DateTime

/**
 *
 * This class provides a usable [DatePickerDialog] wrapped as a [DialogFragment],
 * using the compatibility package v4. Its main advantage is handling Issue 34833
 * automatically for you.
 *
 * Current implementation (because I wanted that way =) ):
 *
 *  * Only two buttons, a `BUTTON_POSITIVE` and a `BUTTON_NEGATIVE`.
 *  * Buttons labeled from `android.R.string.ok` and `android.R.string.cancel`.*
 * @see [Android Issue 34833](http://code.google.com/p/android/issues/detail?id=34833)

 * @see [Jelly Bean DatePickerDialog — is there a way to cancel?](http://stackoverflow.com/q/11444238/489607)
 */
class DatePickerDialogFragment : DialogFragment() {

    private var mListener: ((DatePicker?, DateTime) -> Unit)? = null
    private var mCancelListener: ((DatePicker?, DateTime) -> Unit)? = null

    fun setOnDateSetListener(listener: (DatePicker?, DateTime) -> Unit) {
        mListener = listener
    }

    fun setOnCancelListener(listener: (DatePicker?, DateTime) -> Unit) {
        mCancelListener = listener
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
    }

    override fun onDetach() {
        super.onDetach()
    }

    @TargetApi(11)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val b = arguments
        val date = b.getSerializable(DATE) as DateTime? ?: DateTime.today(TIME_ZONE)
        val minDate = b.getSerializable(MIN_DATE) as DateTime?
        val title = b.getInt(TITLE)

        // Jelly Bean introduced a bug in DatePickerDialog (and possibly 
        // TimePickerDialog as well), and one of the possible solutions is 
        // to postpone the creation of both the listener and the BUTTON_* .
        // 
        // Passing a null here won't harm because DatePickerDialog checks for a null
        // whenever it reads the listener that was passed here. >>> This seems to be 
        // true down to 1.5 / API 3, up to 4.1.1 / API 16. <<< No worries. For now.
        //
        // See my own question and answer, and details I included for the issue:
        //
        // http://stackoverflow.com/a/11493752/489607
        // http://code.google.com/p/android/issues/detail?id=34833
        //
        // Of course, suggestions welcome.

        val picker = DatePickerDialog(activity, constructorListener, date.year, date.month - 1, date.day)

        picker.setTitle(title)
        if (hasJellyBeanAndAbove()) {
            picker.setButton(DialogInterface.BUTTON_POSITIVE,
                    activity.getString(R.string.ok),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            val dp = picker.datePicker
                            val f = mListener
                            if (f != null)
                                f(dp, DateTime.forDateOnly(dp.year, dp.month + 1, dp.dayOfMonth))
                        }
                    })
            picker.setButton(DialogInterface.BUTTON_NEGATIVE,
                    activity.getString(R.string.cancel),
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface, which: Int) {
                            val dp = picker.datePicker
                            val f = mCancelListener
                            if (f != null)
                                f(dp, DateTime.forDateOnly(dp.year, dp.month + 1, dp.dayOfMonth))
                        }
                    })

        }
        if (minDate != null) {
            picker.datePicker.minDate = minDate.getMilliseconds(TIME_ZONE)
        }
        return picker
    }

    private val constructorListener: OnDateSetListener?
        get() = if (hasJellyBeanAndAbove()) null else object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
                val dp = p0
                val f = mListener
                if (f != null)
                    f(dp, DateTime.forDateOnly(p1, p2 + 1, p3))
            }
        }

    companion object {

        val TITLE = "Title"
        val DATE = "Date"
        val MIN_DATE = "MinDate"

        private fun hasJellyBeanAndAbove(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
        }
    }

}
