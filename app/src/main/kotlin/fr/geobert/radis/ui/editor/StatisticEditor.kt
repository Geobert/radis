package fr.geobert.radis.ui.editor

import android.app.Activity
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Message
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.Loader
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.tools.*
import hirondelle.date4j.DateTime
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import kotlin.properties.Delegates

public class StatisticEditor : BaseActivity(), LoaderCallbacks<Cursor>, EditorToolbarTrait {
    private val mNameEdt: EditText by Delegates.lazy { findViewById(R.id.stat_name_edt) as EditText }
    private val mAccountSpin: Spinner by Delegates.lazy { findViewById(R.id.stat_account_spinner) as Spinner }
    private val mFilterSpin: Spinner by Delegates.lazy { findViewById(R.id.stat_filter_spinner) as Spinner }
    private val mTimeScaleSpin: Spinner by Delegates.lazy { findViewById(R.id.stat_period_spinner) as Spinner }
    private val mxLastCont: LinearLayout by Delegates.lazy { findViewById(R.id.x_last_cont) as LinearLayout }
    private val mxLastEdt: EditText by Delegates.lazy { findViewById(R.id.x_last_edt) as EditText }
    private val mxLastSuffixLbl: TextView by Delegates.lazy { findViewById(R.id.x_last_suffix) as TextView }
    private val mAbsDateCont: LinearLayout by Delegates.lazy { findViewById(R.id.absolute_date_cont) as LinearLayout }
    private val mStartDate: Button by Delegates.lazy { findViewById(R.id.start_date_btn) as Button }
    private val mEndDate: Button by Delegates.lazy { findViewById(R.id.end_date_btn) as Button }
    private val mPieBtn: ToggleImageButton by Delegates.lazy { findViewById(R.id.pie_btn) as ToggleImageButton }
    private val mBarBtn: ToggleImageButton by Delegates.lazy { findViewById(R.id.bar_btn) as ToggleImageButton }
    private val mLineBtn: ToggleImageButton by Delegates.lazy { findViewById(R.id.line_btn) as ToggleImageButton }
    private var mAccountSpinAdapter: ArrayAdapter<Account> by Delegates.notNull()

    private var mStatId: Long = 0
    private var mStat: Statistic? = null
    private var mOrigStat: Statistic by Delegates.notNull()

    private val GET_STAT = 700

    companion object {
        public val ACTIVITY_STAT_CREATE: Int = 4000;
        public val ACTIVITY_STAT_EDIT: Int = 4001;

        public fun callMeForResult(ctx: Activity) {
            ctx.startActivityForResult(Intent(ctx, javaClass<StatisticEditor>()), ACTIVITY_STAT_CREATE)
        }

        public fun callMeForResult(ctx: Activity, statId: Long) {
            val i = Intent(ctx, javaClass<StatisticEditor>())
            i.putExtra(StatisticTable.KEY_STAT_ID, statId)
            ctx.startActivityForResult(i, ACTIVITY_STAT_EDIT)
        }
    }

    override protected fun onCreate(savedInstanceState: Bundle?): Unit {
        super<BaseActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.statistics_editor_fragment)
        setTitle(R.string.stat_editor_title)
        initToolbar(this)
        setupChartTypeButtons()
    }

    override fun onResume() {
        super<BaseActivity>.onResume()
        mAccountManager.fetchAllAccounts(false, { ->
            mStatId = getIntent()?.getLongExtra(StatisticTable.KEY_STAT_ID, 0) as Long
            if (mStatId == 0L) {
                if (null == mStat) {
                    mStat = Statistic()
                    mOrigStat = Statistic()
                }
                fillInfoSpinner()
                fillTimeScaleSpinner()
                fillAccountSpinner()
                initAccountSpinner()
                initTimeScaleSpinner()
                initInfoSpinner()
                initChartTypeButtons()
            } else {
                getSupportLoaderManager()?.initLoader(GET_STAT, Bundle(), this)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("mStat", mStat)
        outState.putParcelable("mOrigStat", mOrigStat)
        outState.putCharSequence("xLast", mxLastEdt.getText())
        outState.putCharSequence("name", mNameEdt.getText())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mStat = savedInstanceState.getParcelable<Statistic>("mStat")
        mOrigStat = savedInstanceState.getParcelable<Statistic>("mOrigStat")
        mxLastEdt.setText(savedInstanceState.getCharSequence("xLast"))
        mNameEdt.setText(savedInstanceState.getCharSequence("name"))
    }

    private fun initInfoSpinner() {
        mFilterSpin.setSelection(mStat?.filterType as Int)
    }

    private fun initTimeScaleSpinner() {
        mTimeScaleSpin.setSelection(mStat?.timeScaleType as Int)
        refreshTimeScaleCont()
    }

    private fun initChartTypeButtons() {
        when (mStat?.chartType) {
            Statistic.CHART_PIE -> mPieBtn.setChecked(true)
            Statistic.CHART_BAR -> mBarBtn.setChecked(true)
            Statistic.CHART_LINE -> mLineBtn.setChecked(true)
        }
    }

    private fun refreshTimeScaleCont() {
        fun showAbsCont(show: Boolean) {
            if (show) {
                mxLastCont.setVisibility(View.GONE)
                mAbsDateCont.setVisibility(View.VISIBLE)
                mStartDate.setText(mStat?.startDate?.formatDate())
                mEndDate.setText(mStat?.endDate?.formatDate())
            } else {
                mxLastCont.setVisibility(View.VISIBLE)
                mAbsDateCont.setVisibility(View.GONE)
                mxLastEdt.setText(mStat?.xLast.toString())
            }
        }

        when (mStat?.timeScaleType) {
            Statistic.PERIOD_DAYS -> {
                showAbsCont(show = false)
                mxLastSuffixLbl.setText(getString(R.string.last_male) + " " + getString(R.string.days).toLowerCase())
            }

            Statistic.PERIOD_MONTHES -> {
                showAbsCont(show = false)
                mxLastSuffixLbl.setText(getString(R.string.last_male) + " " + getString(R.string.monthes).toLowerCase())
            }
            Statistic.PERIOD_YEARS -> {
                showAbsCont(show = false)
                mxLastSuffixLbl.setText(getString(R.string.last_female) + " " + getString(R.string.years).toLowerCase())
            }
            Statistic.PERIOD_ABSOLUTE -> showAbsCont(show = true)
        }
    }

    private fun fillTimeScaleSpinner(): Unit {
        val values = array(getString(R.string.days), getString(R.string.monthes), getString(R.string.years),
                getString(R.string.absolute))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mTimeScaleSpin.setAdapter(adapter)
        mTimeScaleSpin.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
                // nothing to do
            }

            override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p: Int, id: Long) {
                val stat = mStat
                if (stat != null) stat.timeScaleType = p // TODO check https://youtrack.jetbrains.com/issue/KT-1213
                refreshTimeScaleCont()
            }
        })

        mStartDate.setOnClickListener { showDatePicker(mStartDate) }

        mEndDate.setOnClickListener { showDatePicker(mEndDate) }
    }

    fun showDatePicker(button: Button): Unit {
        val date = GregorianCalendar()
        date.setTime(button.getText().toString().parseDate())
        val datePicker = DatePickerDialog(this, { v: DatePicker?, y: Int, m: Int, d: Int ->
            val dt = DateTime.forDateOnly(y, m + 1, d)
            val stat = mStat
            if (stat != null) // TODO check https://youtrack.jetbrains.com/issue/KT-1213
                when (button.getId()) {
                    R.id.start_date_btn -> stat.startDate = dt
                    R.id.end_date_btn -> stat.endDate = dt
                }
            button.setText(dt.formatDate())
        }, date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
        datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, this.getString(android.R.string.cancel), null:Message?)
        datePicker.show()
    }

    private fun fillInfoSpinner(): Unit {
        val values = array(getString(R.string.third_party), getString(R.string.tag), getString(R.string.mode),
                getString(R.string.no_filter))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mFilterSpin.setAdapter(adapter)
        mFilterSpin.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
                // nothing to do
            }

            override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p: Int, id: Long) {
                val stat = mStat
                if (stat != null) stat.filterType = p
            }
        })
    }

    private fun fillAccountSpinner(): Unit {
        val adapter = ArrayAdapter<Account>(this, android.R.layout.simple_spinner_item)
        val allAccCursor = mAccountManager.allAccountsCursor
        if (allAccCursor != null) {
            allAccCursor.moveToPosition(-1)
            allAccCursor.forEach({ adapter.add(Account(it)) })
            mAccountSpinAdapter = adapter
            mAccountSpin.setAdapter(adapter)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mAccountSpin.setOnItemSelectedListener(object : OnItemSelectedListener {
                override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p2: Int, p3: Long) {
                    val stat = mStat
                    if (stat != null) {
                        // TODO https://youtrack.jetbrains.com/issue/KT-1213
                        stat.accountId = (a?.getSelectedItem() as Account).id
                        stat.accountName = (v as TextView).getText().toString()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
                    // nothing
                }
            })
        }
    }

    private fun initAccountSpinner(): Unit {
        val pos = mAccountSpinAdapter.getPosition(Account(mStat?.accountId as Long))
        if (pos > -1) {
            mAccountSpin.setSelection(pos)
        }
    }

    private fun setupChartTypeButtons() {
        fun onCheckedChanged(view: ToggleImageButton, checked: Boolean, chartType: Int) {
            when (checked) {
                true -> if (mStat?.chartType != chartType) {
                    val stat = mStat // TODO https://youtrack.jetbrains.com/issue/KT-1213
                    if (stat != null) stat.chartType = chartType
                    mPieBtn.setChecked(false)
                    mBarBtn.setChecked(false)
                    mLineBtn.setChecked(false)
                    view.setChecked(true)
                }
                false -> if (mStat?.chartType == chartType) view.setChecked(true)
            }
        }

        mPieBtn.setOnCheckedChangeListener {
            view: ToggleImageButton?, checked: Boolean ->
            if (view != null)
                onCheckedChanged(view, checked, Statistic.CHART_PIE)
        }

        mBarBtn.setOnCheckedChangeListener {
            view: ToggleImageButton?, checked: Boolean ->
            if (view != null)
                onCheckedChanged(view, checked, Statistic.CHART_BAR)
        }

        mLineBtn.setOnCheckedChangeListener {
            view: ToggleImageButton?, checked: Boolean ->
            if (view != null)
                onCheckedChanged(view, checked, Statistic.CHART_LINE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = getMenuInflater()
        inflater.inflate(R.menu.confirm_cancel_menu, menu)
        return true
    }

    private fun formErrorMessage(): String? {
        val builder = StringBuilder()
        if (mNameEdt.getText().toString().trim().length() <= 0) {
            builder.append("- ":CharSequence).append(getString(R.string.empty_stat_name):CharSequence).append('\n')
        }
        when (mStat?.timeScaleType) {
            Statistic.PERIOD_DAYS, Statistic.PERIOD_MONTHES, Statistic.PERIOD_YEARS -> {
                val valid = "([0-9]+)"
                val x = mxLastEdt.getText().toString()
                if (!x.matches(valid)) {
                    fun last(): String =
                            if (mStat?.timeScaleType == Statistic.PERIOD_YEARS)
                                getString(R.string.last_female)
                            else
                                getString(R.string.last_male)

                    builder.append("- ":CharSequence).append(getString(R.string.xlast_invalid).format(last(),
                            mTimeScaleSpin.getSelectedItem().toString().toLowerCase()):CharSequence)
                }
            }
            Statistic.PERIOD_ABSOLUTE ->
                if (mStat?.startDate?.compareTo(mStat?.endDate) as Int >= 0) {
                    builder.append("- ":CharSequence).append(getString(R.string.invalid_date_range):CharSequence)
                }
        }

        return if (builder.length() > 0) builder.toString() else null
    }

    private fun fillStat() {
        // filling missing info
        val stat = mStat
        if (stat != null) {
            stat.name = mNameEdt.getText().toString().trim()
            when (stat.timeScaleType) {
                Statistic.PERIOD_DAYS, Statistic.PERIOD_MONTHES, Statistic.PERIOD_YEARS ->
                    stat.xLast = Integer.parseInt(mxLastEdt.getText().toString())
            }
        }
    }

    private fun onConfirmPressed() {
        val msg = formErrorMessage()
        if (msg != null) {
            alert(this, R.string.error, msg)
        } else {
            fillStat()
            val stat = mStat
            if (stat != null)
                if (stat.id == 0L || stat != mOrigStat) {
                    // create or update stat
                    val success = if (stat.id == 0L) {
                        StatisticTable.createStatistic(stat, this) > 0L
                    } else {
                        StatisticTable.updateStatistic(stat, this) > 0L
                    }

                    if (success ) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        alert(this, R.string.error, getString(R.string.err_db_stat))
                    }
                } else {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
            when (item.getItemId()) {
                R.id.confirm -> {
                    onConfirmPressed()
                    true
                }
                else -> false
            }


    override fun onCreateLoader(p1: Int, p2: Bundle?): Loader<Cursor> {
        return StatisticTable.getStatisticLoader(mStatId, this)
    }

    override fun onLoaderReset(p1: Loader<Cursor>?) {
    }

    fun initForm() {
        mNameEdt.setText(mStat?.name)
    }

    override fun onLoadFinished(p1: Loader<Cursor>?, p2: Cursor?) {
        if (p2 != null && p2.moveToFirst() ) {
            mStat = Statistic(p2)
            mOrigStat = Statistic(p2)

            fillAccountSpinner()
            fillInfoSpinner()
            fillTimeScaleSpinner()
            initAccountSpinner()
            initTimeScaleSpinner()
            initInfoSpinner()
            initChartTypeButtons()
            initForm()
        }
    }
}
