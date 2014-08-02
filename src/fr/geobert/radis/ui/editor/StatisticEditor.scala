package fr.geobert.radis.ui.editor

import java.util.{Calendar, GregorianCalendar}

import android.app.{Activity, DatePickerDialog}
import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.Loader
import android.view._
import android.widget._
import fr.geobert.radis.data.{Account, Statistic}
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.tools.scala.RadisImplicits._
import fr.geobert.radis.tools.scala.{RadisImplicits, STools}
import fr.geobert.radis.tools.{Formater, ToggleImageButton}
import fr.geobert.radis.{BaseActivity, R}
import org.scaloid.common._

object StatisticEditor {
  def callMeForResult(statId: Long = 0)(implicit ctx: Activity) = {
    ctx.startActivityForResult(SIntent[StatisticEditor].putExtra(StatisticTable.KEY_STAT_ID, statId), 0)
  }
}

class StatisticEditor extends BaseActivity with SActivity with LoaderCallbacks[Cursor] with Implicits
with RadisImplicits {
  private var mNameEdt: EditText = _
  private var mAccountSpin: Spinner = _
  private var mFilterSpin: Spinner = _
  private var mTimeScaleSpin: Spinner = _
  private var mxLastCont: LinearLayout = _
  private var mxLastEdt: EditText = _
  private var mxLastSuffixLbl: TextView = _
  private var mAbsDateCont: LinearLayout = _
  private var mStartDate: Button = _
  private var mEndDate: Button = _
  private var mPieBtn: ToggleImageButton = _
  private var mBarBtn: ToggleImageButton = _
  private var mLineBtn: ToggleImageButton = _

  private var mStatId: Long = _
  private var mStat: Statistic = _
  private var mOrigStat: Statistic = _

  private val GET_STAT = 700


  override protected def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.statistics_editor_fragment)

    val bar = getSupportActionBar
    bar.setTitle(R.string.stat_editor_title)
    bar.setDisplayHomeAsUpEnabled(true)

    initRefs()
    setupChartTypeButtons()
  }

  override def onResume() {
    super.onResume()
    mAccountManager.fetchAllAccounts(this, false, () => {
      mStatId = getIntent.getLongExtra(StatisticTable.KEY_STAT_ID, 0)
      if (mStatId == 0) {
        if (null == mStat) {
          mStat = new Statistic
          mOrigStat = new Statistic
        }
        fillInfoSpinner()
        fillTimeScaleSpinner()
        fillAccountSpinner()
        initAccountSpinner()
        initTimeScaleSpinner()
        initInfoSpinner()
        initChartTypeButtons()
      } else {
        getSupportLoaderManager.initLoader(GET_STAT, null, this)
      }
    })
  }


  override def onSaveInstanceState(outState: Bundle): Unit = {
    outState.putSerializable("mStat", mStat)
    outState.putSerializable("mOrigStat", mOrigStat)
    outState.putCharSequence("xLast", mxLastEdt.text)
    outState.putCharSequence("name", mNameEdt.text)
  }

  override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {
    mStat = savedInstanceState.getSerializable("mStat").asInstanceOf[Statistic]
    mOrigStat = savedInstanceState.getSerializable("mOrigStat").asInstanceOf[Statistic]
    mxLastEdt.text = savedInstanceState.getCharSequence("xLast")
    mNameEdt.text = savedInstanceState.getCharSequence("name")
  }

  private def initInfoSpinner() {
    mFilterSpin.selection = mStat.filterType
  }

  private def initTimeScaleSpinner() {
    mTimeScaleSpin.selection = mStat.timeScaleType
    refreshTimeScaleCont()
  }

  private def initChartTypeButtons() {
    mStat.chartType match {
      case Statistic.CHART_PIE =>
        mPieBtn.setChecked(true)
      case Statistic.CHART_BAR =>
        mBarBtn.setChecked(true)
      case Statistic.CHART_LINE =>
        mLineBtn.setChecked(true)
    }
  }

  private def refreshTimeScaleCont() {
    def showAbsCont(show: Boolean) {
      if (show) {
        mxLastCont.visibility = View.GONE
        mAbsDateCont.visibility = View.VISIBLE
        mStartDate.text = Formater.getFullDateFormater.format(mStat.startDate)
        mEndDate.text = Formater.getFullDateFormater.format(mStat.endDate)
      } else {
        mxLastCont.visibility = View.VISIBLE
        mAbsDateCont.visibility = View.GONE
        mxLastEdt.text = mStat.xLast.toString
      }
    }

    mStat.timeScaleType match {
      case Statistic.PERIOD_DAYS =>
        showAbsCont(show = false)
        mxLastSuffixLbl.text = getString(R.string.last_male) + " " + getString(R.string.days).toLowerCase
      case Statistic.PERIOD_MONTHES =>
        showAbsCont(show = false)
        mxLastSuffixLbl.text = getString(R.string.last_male) + " " + getString(R.string.monthes).toLowerCase
      case Statistic.PERIOD_YEARS =>
        showAbsCont(show = false)
        mxLastSuffixLbl.text = getString(R.string.last_female) + " " + getString(R.string.years).toLowerCase
      case Statistic.PERIOD_ABSOLUTE =>
        showAbsCont(show = true)
    }
  }

  private def fillTimeScaleSpinner(): Unit = {
    val adapter = SArrayAdapter(android.R.layout.simple_spinner_item,
      getString(R.string.days), getString(R.string.monthes), getString(R.string.years),
      getString(R.string.absolute)).dropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    mTimeScaleSpin.setAdapter(adapter)
    mTimeScaleSpin.onItemSelected((a: AdapterView[_], v: View, p: Int, id: Long) => {
      mStat.timeScaleType = p
      refreshTimeScaleCont()
    })

    mStartDate.onClick((view: View) => {
      showDatePicker(mStartDate)
    })

    mEndDate.onClick((view: View) => {
      showDatePicker(mEndDate)
    })
  }

  def showDatePicker(button: Button): Unit = {
    val date = new GregorianCalendar
    date.setTime(Formater.getFullDateFormater.parse(button.text.toString))
    val datePicker = new DatePickerDialog(this, (v: DatePicker, y: Int, m: Int, d: Int) => {
      val date = new GregorianCalendar(y, m, d).getTime
      try {
        button.id match {
          case R.id.start_date_btn =>
            mStat.startDate = date
          case R.id.end_date_btn =>
            mStat.endDate = date
        }
        button.text = Formater.getFullDateFormater.format(date)
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
    datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, this.getString(android.R.string.cancel), null)
    datePicker.show()
  }

  private def fillInfoSpinner(): Unit = {
    val adapter = SArrayAdapter(android.R.layout.simple_spinner_item,
      getString(R.string.third_party), getString(R.string.tag), getString(R.string.mode),
      getString(R.string.no_filter)).dropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    mFilterSpin.setAdapter(adapter)
    mFilterSpin.onItemSelected((a: AdapterView[_], v: View, p: Int, id: Long) => {
      mStat.filterType = p
    })
  }

  private def fillAccountSpinner(): Unit = {
    val adapter = new ArrayAdapter[Account](this, android.R.layout.simple_spinner_item)
    val allAccCursor = mAccountManager.getAllAccountsCursor
    allAccCursor.moveToPosition(-1)
    allAccCursor.foreach(c => adapter.add(Account(c)))
    mAccountSpin.setAdapter(adapter)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    mAccountSpin.onItemSelected((a: AdapterView[_], v: View, p: Int, id: Long) => {
      mStat.accountId = a.getSelectedItem.asInstanceOf[Account].id
      mStat.accountName = v.asInstanceOf[TextView].text.toString
    })
  }

  private def initAccountSpinner(): Unit = {
    val adapter = mAccountSpin.getAdapter.asInstanceOf[ArrayAdapter[Account]]
    val pos = adapter.getPosition(new Account(mStat.accountId))
    if (pos > -1) {
      mAccountSpin.setSelection(pos)
    }
  }

  private def initRefs() {
    mNameEdt = find[EditText](R.id.stat_name_edt)
    mAccountSpin = find[Spinner](R.id.stat_account_spinner)
    mFilterSpin = find[Spinner](R.id.stat_filter_spinner)
    mTimeScaleSpin = find[Spinner](R.id.stat_period_spinner)
    mAbsDateCont = find[LinearLayout](R.id.absolute_date_cont)
    mxLastSuffixLbl = find[TextView](R.id.x_last_suffix)
    mxLastCont = find[LinearLayout](R.id.x_last_cont)
    mxLastEdt = find[EditText](R.id.x_last_edt)
    mPieBtn = find[ToggleImageButton](R.id.pie_btn)
    mBarBtn = find[ToggleImageButton](R.id.bar_btn)
    mLineBtn = find[ToggleImageButton](R.id.line_btn)
    mStartDate = find[Button](R.id.start_date_btn)
    mEndDate = find[Button](R.id.end_date_btn)

    // TODO remove
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
  }

  private def setupChartTypeButtons() {
    def onCheckedChanged(view: ToggleImageButton, checked: Boolean, chartType: Int) = {
      checked match {
        case true if mStat.chartType != chartType =>
          mStat.chartType = chartType
          mPieBtn.setChecked(false)
          mBarBtn.setChecked(false)
          mLineBtn.setChecked(false)
          view.setChecked(true)
        case false if mStat.chartType == chartType =>
          view.setChecked(true)
        case _ => // nothing to do
      }
    }

    mPieBtn.onCheckedChanged {
      (view: ToggleImageButton, checked: Boolean) =>
        onCheckedChanged(view, checked, Statistic.CHART_PIE)
    }

    mBarBtn.onCheckedChanged {
      (view: ToggleImageButton, checked: Boolean) =>
        onCheckedChanged(view, checked, Statistic.CHART_BAR)
    }

    mLineBtn.onCheckedChanged {
      (view: ToggleImageButton, checked: Boolean) =>
        onCheckedChanged(view, checked, Statistic.CHART_LINE)
    }
  }


  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater: MenuInflater = getMenuInflater
    inflater.inflate(R.menu.confirm_cancel_menu, menu)
    true
  }

  private def onCancelPressed() = {
    setResult(Activity.RESULT_CANCELED)
    finish()
  }

  private def formErrorMessage: Option[String] = {
    val builder = new StringBuilder
    if (mNameEdt.getText.toString.trim.length <= 0) {
      builder.append("- ").append(getString(R.string.empty_stat_name)).append('\n')
    }
    mStat.timeScaleType match {
      case Statistic.PERIOD_DAYS | Statistic.PERIOD_MONTHES | Statistic.PERIOD_YEARS =>
        val valid = "([0-9]+)".r
        mxLastEdt.getText.toString match {
          case valid(s) => info(s)
          case s =>
            def last: String = {
              if (mStat.timeScaleType == Statistic.PERIOD_YEARS)
                getString(R.string.last_female)
              else
                getString(R.string.last_male)
            }
            builder.append("- ").append(getString(R.string.xlast_invalid).format(last,
              mTimeScaleSpin.getSelectedItem.toString.toLowerCase))
        }
      case Statistic.PERIOD_ABSOLUTE =>
        if (mStat.startDate.compareTo(mStat.endDate) >= 0) {
          builder.append("- ").append(getString(R.string.invalid_date_range))
        }
    }

    if (builder.length > 0) Some(builder.toString()) else None
  }

  private def fillStat() {
    // filling missing infos
    mStat.name = mNameEdt.getText.toString.trim
    mStat.timeScaleType match {
      case Statistic.PERIOD_DAYS | Statistic.PERIOD_MONTHES | Statistic.PERIOD_YEARS =>
        mStat.xLast = Integer.parseInt(mxLastEdt.getText.toString)
      case _ => // nothing to do
    }
  }

  private def onConfirmPressed() = {
    if (mStat.id == 0 || mStat != mOrigStat) {
      formErrorMessage match {
        case Some(msg) =>
          STools.popError(msg)
        case None =>
          fillStat()

          // create or update stat
          val success = if (mStat.id == 0) {
            StatisticTable.createStatistic(mStat) > 0
          } else {
            StatisticTable.updateStatistic(mStat) > 0
          }

          if (success) {
            setResult(Activity.RESULT_OK)
            finish()
          } else {
            STools.popError(getString(R.string.err_db_stat))
          }
      }
    } else {
      setResult(Activity.RESULT_CANCELED)
      finish()
    }
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home =>
        onBackPressed()
        true
      case R.id.cancel =>
        onCancelPressed()
        true
      case R.id.confirm =>
        onConfirmPressed()
        true
      case _ =>
        false
    }
  }

  override def onCreateLoader(p1: Int, p2: Bundle): Loader[Cursor] = {
    StatisticTable.getStatisticLoader(mStatId)
  }

  override def onLoaderReset(p1: Loader[Cursor]): Unit = {
  }

  def initForm() = {
    mNameEdt.text = mStat.name

  }

  override def onLoadFinished(p1: Loader[Cursor], p2: Cursor): Unit = {
    if (p2.moveToFirst()) {
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
