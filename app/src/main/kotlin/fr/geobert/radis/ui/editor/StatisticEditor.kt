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
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.tools.ToggleImageButton
import fr.geobert.radis.tools.alert
import fr.geobert.radis.tools.formatDateLong
import fr.geobert.radis.tools.parseDate
import hirondelle.date4j.DateTime
import java.util.*
import kotlin.properties.Delegates

public class StatisticEditor : BaseActivity(), LoaderCallbacks<Cursor>, EditorToolbarTrait {
    private val mNameEdt: EditText by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.stat_name_edt) as EditText }
    private val mAccountSpin: Spinner by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.stat_account_spinner) as Spinner }
    private val mFilterSpin: Spinner by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.stat_filter_spinner) as Spinner }
    private val mTimePeriodSpin: Spinner by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.stat_period_spinner) as Spinner }
    private val mTimeScaleSpin: Spinner by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.stat_timescale_spinner) as Spinner }
    private val mxLastCont: LinearLayout by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.x_last_cont) as LinearLayout }
    private val mxLastEdt: EditText by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.x_last_edt) as EditText }
    private val mxLastSuffixLbl: TextView by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.x_last_suffix) as TextView }
    private val mAbsDateCont: LinearLayout by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.absolute_date_cont) as LinearLayout }
    private val mStartDate: Button by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.start_date_btn) as Button }
    private val mEndDate: Button by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.end_date_btn) as Button }
    private val mPieBtn: ToggleImageButton by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.pie_btn) as ToggleImageButton }
    private val mBarBtn: ToggleImageButton by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.bar_btn) as ToggleImageButton }
    private val mLineBtn: ToggleImageButton by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.line_btn) as ToggleImageButton }
    private var mAccountSpinAdapter: ArrayAdapter<Account> by Delegates.notNull()

    private var mStatId: Long = 0
    private var mStat: Statistic? = null
    private var mOrigStat: Statistic = Statistic()

    private val GET_STAT = 700

    companion object {
        public val ACTIVITY_STAT_CREATE: Int = 4000;
        public val ACTIVITY_STAT_EDIT: Int = 4001;

        public fun callMeForResult(ctx: Activity) {
            ctx.startActivityForResult(Intent(ctx, StatisticEditor::class.java), ACTIVITY_STAT_CREATE)
        }

        public fun callMeForResult(ctx: Activity, statId: Long) {
            val i = Intent(ctx, StatisticEditor::class.java)
            i.putExtra(StatisticTable.KEY_STAT_ID, statId)
            ctx.startActivityForResult(i, ACTIVITY_STAT_EDIT)
        }
    }

    override protected fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.statistics_editor_fragment)
        setTitle(R.string.stat_editor_title)
        initToolbar(this)
        setupChartTypeButtons()
    }

    override fun onResume() {
        super.onResume()
        mAccountManager.fetchAllAccounts(false, { ->
            mStatId = intent?.getLongExtra(StatisticTable.KEY_STAT_ID, 0) as Long
            if (mStatId == 0L) {
                if (null == mStat) {
                    mStat = Statistic()
                }
                fillInfoSpinner()
                fillTimePeriodSpinner()
                fillTimeScaleSpinner()
                fillAccountSpinner()
                initAccountSpinner()
                initTimePeriodSpinner()
                initTimeScaleSpinner()
                initInfoSpinner()
                initChartTypeButtons()
            } else {
                supportLoaderManager?.initLoader(GET_STAT, Bundle(), this)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("mStat", mStat)
        outState.putParcelable("mOrigStat", mOrigStat)
        outState.putCharSequence("xLast", mxLastEdt.text)
        outState.putCharSequence("name", mNameEdt.text)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mStat = savedInstanceState.getParcelable<Statistic>("mStat")
        mOrigStat = savedInstanceState.getParcelable<Statistic>("mOrigStat")
        mxLastEdt.setText(savedInstanceState.getCharSequence("xLast"))
        mNameEdt.setText(savedInstanceState.getCharSequence("name"))
    }

    private fun initInfoSpinner() {
        mFilterSpin.setSelection(mStat?.filterType as Int, false)
//        mFilterSpin.onItemSelectedListener = object : OnItemSelectedListener {
//            override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
//                // nothing to do
//            }
//
//            override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p: Int, id: Long) {
//                val stat = mStat
//                if (stat != null) stat.filterType = p
//            }
//        }
    }

    private fun initTimePeriodSpinner() {
        mTimePeriodSpin.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
                // nothing to do
            }

            override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p: Int, id: Long) {
                refreshTimePeriodCont(p)
            }
        }
        mTimePeriodSpin.setSelection(mStat?.timePeriodType as Int, false)
        refreshTimePeriodCont(mStat?.timePeriodType as Int)
    }

    private fun initTimeScaleSpinner() {
        mTimeScaleSpin.setSelection(mStat?.timeScaleType as Int, false)
//        mTimeScaleSpin.onItemSelectedListener = object : OnItemSelectedListener {
//            override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
//                // nothing to do
//            }
//
//            override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p: Int, id: Long) {
//                mStat?.timeScaleType = p
//            }
//        }
    }
    private fun initChartTypeButtons() {
        when (mStat?.chartType) {
            Statistic.CHART_PIE -> mPieBtn.isChecked = true
            Statistic.CHART_BAR -> mBarBtn.isChecked = true
            Statistic.CHART_LINE -> mLineBtn.isChecked = true
        }
    }

    private fun refreshTimePeriodCont(p: Int) {
        fun showAbsCont(show: Boolean) {
            if (show) {
                mxLastCont.visibility = View.GONE
                mAbsDateCont.visibility = View.VISIBLE
                mStartDate.text = mStat?.startDate?.formatDateLong()
                mEndDate.text = mStat?.endDate?.formatDateLong()
            } else {
                mxLastCont.visibility = View.VISIBLE
                mAbsDateCont.visibility = View.GONE
                mxLastEdt.setText(mStat?.xLast.toString())
            }
        }

        when (mStat?.timePeriodType) {
            Statistic.PERIOD_DAYS -> {
                showAbsCont(show = false)
                mxLastSuffixLbl.text = getString(R.string.last_male) + " " + getString(R.string.days).toLowerCase()
            }

            Statistic.PERIOD_MONTHES -> {
                showAbsCont(show = false)
                mxLastSuffixLbl.text = getString(R.string.last_male) + " " + getString(R.string.monthes).toLowerCase()
            }
            Statistic.PERIOD_YEARS -> {
                showAbsCont(show = false)
                mxLastSuffixLbl.text = getString(R.string.last_female) + " " + getString(R.string.years).toLowerCase()
            }
            Statistic.PERIOD_ABSOLUTE -> showAbsCont(show = true)
        }
    }

    private fun fillTimeScaleSpinner(): Unit {
        val values = arrayOf(getString(R.string.days), getString(R.string.monthes), getString(R.string.years))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mTimeScaleSpin.adapter = adapter

    }

    private fun fillTimePeriodSpinner(): Unit {
        val values = arrayOf(getString(R.string.days), getString(R.string.monthes), getString(R.string.years),
                getString(R.string.absolute))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mTimePeriodSpin.adapter = adapter

        mStartDate.setOnClickListener { showDatePicker(mStartDate) }
        mEndDate.setOnClickListener { showDatePicker(mEndDate) }
    }

    fun showDatePicker(button: Button): Unit {
        val date = GregorianCalendar()
        date.time = button.text.toString().parseDate()
        val datePicker = DatePickerDialog(this, { v: DatePicker?, y: Int, m: Int, d: Int ->
            val dt = DateTime.forDateOnly(y, m + 1, d)
            when (button.id) {
                R.id.start_date_btn -> mStat?.startDate = dt
                R.id.end_date_btn -> mStat?.endDate = dt
            }
            button.text = dt.formatDateLong()
        }, date[Calendar.YEAR], date[Calendar.MONTH], date[Calendar.DAY_OF_MONTH])
        datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, this.getString(android.R.string.cancel), null as Message?)
        datePicker.show()
    }

    private fun fillInfoSpinner(): Unit {
        val values = arrayOf(getString(R.string.third_party), getString(R.string.tag), getString(R.string.mode),
                getString(R.string.no_filter))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mFilterSpin.adapter = adapter
    }

    private fun fillAccountSpinner(): Unit {
        val adapter = ArrayAdapter<Account>(this, android.R.layout.simple_spinner_item)
        val allAccCursor = mAccountManager.mAccountAdapter

        allAccCursor.forEach({ adapter.add(it) })
        mAccountSpinAdapter = adapter
        mAccountSpin.adapter = adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    private fun initAccountSpinner(): Unit {
        val pos = mAccountSpinAdapter.getPosition(Account(mStat?.accountId as Long))
        if (pos > -1) {
            mAccountSpin.setSelection(pos)
        }
        mAccountSpin.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<out Adapter?>?, v: View?, p2: Int, p3: Long) {
                mStat?.accountId = (a?.selectedItem as Account).id
                mStat?.accountName = (v as TextView).text.toString()
            }

            override fun onNothingSelected(p0: AdapterView<out Adapter?>?) {
                // nothing
            }
        }
    }

    private fun setupChartTypeButtons() {
        fun onCheckedChanged(view: ToggleImageButton, checked: Boolean, chartType: Int) {
            when (checked) {
                true -> if (mStat?.chartType != chartType) {
                    mStat?.chartType = chartType
                    mPieBtn.isChecked = false
                    mBarBtn.isChecked = false
                    mLineBtn.isChecked = false
                    view.isChecked = true
                }
                false -> if (mStat?.chartType == chartType) view.isChecked = true
            }
        }

        mPieBtn.setOnCheckedChangeListener(object : ToggleImageButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: ToggleImageButton, isChecked: Boolean) {
                onCheckedChanged(buttonView, isChecked, Statistic.CHART_PIE)
            }
        })

        mBarBtn.setOnCheckedChangeListener(object : ToggleImageButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: ToggleImageButton, isChecked: Boolean) {
                onCheckedChanged(buttonView, isChecked, Statistic.CHART_BAR)
            }
        })

        mLineBtn.setOnCheckedChangeListener(object : ToggleImageButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: ToggleImageButton, isChecked: Boolean) {
                onCheckedChanged(buttonView, isChecked, Statistic.CHART_LINE)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.confirm_cancel_menu, menu)
        return true
    }

    private fun formErrorMessage(): String? {
        val builder = StringBuilder()
        if (mNameEdt.text.toString().trim().length <= 0) {
            builder.append("- ").append(getString(R.string.empty_stat_name)).append('\n')
        }
        when (mStat?.timePeriodType) {
            Statistic.PERIOD_DAYS, Statistic.PERIOD_MONTHES, Statistic.PERIOD_YEARS -> {
                val valid = "([0-9]+)"
                val x = mxLastEdt.text.toString()
                if (!x.matches(valid.toRegex())) {
                    fun last(): String =
                            if (mStat?.timePeriodType == Statistic.PERIOD_YEARS)
                                getString(R.string.last_female)
                            else
                                getString(R.string.last_male)

                    builder.append("- ").append(getString(R.string.xlast_invalid).format(last(),
                            mTimePeriodSpin.selectedItem.toString().toLowerCase()))
                }
            }
            Statistic.PERIOD_ABSOLUTE ->
                if (mStat?.startDate?.compareTo(mStat?.endDate) as Int >= 0) {
                    builder.append("- ").append(getString(R.string.invalid_date_range))
                }
        }

        return if (builder.length > 0) builder.toString() else null
    }

    private fun fillStat() {
        // filling missing info
        val stat = mStat
        if (stat != null) {
            stat.name = mNameEdt.text.toString().trim()
            stat.timePeriodType = mTimePeriodSpin.selectedItemPosition
            stat.timeScaleType = mTimeScaleSpin.selectedItemPosition
            stat.filterType = mFilterSpin.selectedItemPosition
            when (stat.timePeriodType) {
                Statistic.PERIOD_DAYS, Statistic.PERIOD_MONTHES, Statistic.PERIOD_YEARS ->
                    stat.xLast = Integer.parseInt(mxLastEdt.text.toString())
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

                    if (success) {
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
            when (item.itemId) {
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
            fillTimePeriodSpinner()
            fillTimeScaleSpinner()
            initAccountSpinner()
            initTimePeriodSpinner()
            initTimeScaleSpinner()
            initInfoSpinner()
            initChartTypeButtons()
            initForm()
        }
    }
}
