package fr.geobert.radis.test

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.database.Cursor
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import android.support.test.runner.lifecycle.Stage
import android.test.suitebuilder.annotation.LargeTest
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import com.google.android.apps.common.testing.deps.guava.base.Throwables
import com.google.android.apps.common.testing.deps.guava.collect.Sets
import fr.geobert.espresso.DebugEspresso
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.ConfigFragment
import fr.geobert.radis.ui.adapter.OpRowHolder
import hirondelle.date4j.DateTime
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
@LargeTest
public class RadisTest {

    @Rule
    private val activityRule = ActivityTestRule(MainActivity::class.java)

    private fun getActivity() = activityRule.activity

    @Before
    fun setUp() {
        val appCtx = InstrumentationRegistry.getTargetContext().applicationContext
        DBPrefsManager.getInstance(appCtx).resetAll()
        val client = appCtx.contentResolver.acquireContentProviderClient("fr.geobert.radis.db")
        val provider = client.localContentProvider as DbContentProvider
        provider.deleteDatabase(appCtx)
        client.release()

        val i = Intent()
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activityRule.launchActivity(i)
    }

    @After
    fun tearDown() {
        closeAllActivities(InstrumentationRegistry.getInstrumentation())
    }

    @Throws(Exception::class)
    public fun closeAllActivities(instrumentation: Instrumentation) {
        val NUMBER_OF_RETRIES = 100
        var i = 0
        while (closeActivity(instrumentation)) {
            if (i++ > NUMBER_OF_RETRIES) {
                throw AssertionError("Limit of retries excesses")
            }
            Thread.sleep(200)
        }
    }

    @Throws(Exception::class)
    public fun <X> callOnMainSync(instrumentation: Instrumentation, callable: Callable<X>): X {
        val retAtomic = AtomicReference<X>()
        val exceptionAtomic = AtomicReference<Throwable>()
        instrumentation.runOnMainSync(object : Runnable {
            override fun run() {
                try {
                    retAtomic.set(callable.call())
                } catch (e: Throwable) {
                    exceptionAtomic.set(e)
                }

            }
        })
        val exception = exceptionAtomic.get()
        if (exception != null) {
            //Throwables.propagateIfInstanceOf(exception, Exception::class.javaClass)
            Throwables.propagate(exception)
        }
        return retAtomic.get()
    }

    public fun getActivitiesInStages(vararg stages: Stage): HashSet<Activity> {
        val activities = Sets.newHashSet<Activity>()
        val instance = ActivityLifecycleMonitorRegistry.getInstance()
        for (stage in stages) {
            val activitiesInStage = instance.getActivitiesInStage(stage)
            if (activitiesInStage != null) {
                activities.addAll(activitiesInStage)
            }
        }
        return activities
    }

    @Throws(Exception::class)
    private fun closeActivity(instrumentation: Instrumentation): Boolean {
        val activityClosed = callOnMainSync(instrumentation, object : Callable<Boolean> {
            @Throws(Exception::class)
            override public fun call(): Boolean {
                val activities = getActivitiesInStages(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED)
                activities.removeAll(getActivitiesInStages(Stage.DESTROYED))
                if (activities.size() > 0) {
                    val activity = activities.iterator().next()
                    activity.finish()
                    return true
                } else {
                    return false
                }
            }
        })
        if (activityClosed) {
            instrumentation.waitForIdleSync()
        }
        return activityClosed
    }


    @Test public fun testEditOp() {
        TAG = "testEditOp"
        Helpers.addManyOps()
        Helpers.clickOnRecyclerViewAtPos(5)
        onView(withId(R.id.operation_list)).perform(scrollToPosition<OpRowHolder<Operation>>(6))
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(scrollTo())
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        onView(withId(R.id.edit_op_sum)).perform(typeText("103"))
        Helpers.clickOnActionItemConfirm()
        Helpers.checkAccountSumIs((-2.5).formatSum())
    }

    @Test public fun testQuickAddFromOpList() {
        TAG = "testQuickAddFromOpList"
        Helpers.addAccount()
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(1000.50.formatSum()))))
        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(typeText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(click())
        Helpers.checkAccountSumIs(999.50.formatSum())
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
    }

    @Test public fun testDisableAutoNegate() {
        TAG = "testDisableAutoNegate"
        Helpers.addAccount()
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_creation)
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "+$OP_AMOUNT")
        onView(withId(R.id.edit_op_sum)).check(matches(withText(equalTo("+10,50"))))
        Helpers.clickOnActionItemConfirm()
        Helpers.checkAccountSumIs(1011.0.formatSum())
        onView(withId(R.id.op_sum)).check(matches(withText(equalTo(10.50.formatSum()))))
    }

    /**
     * Schedule ops
     */


    @Test public fun testEditScheduledOp() {
        TAG = "testEditScheduledOp"
        Helpers.setUpSchOp()
        Helpers.addScheduleOp(DateTime.today(TIME_ZONE))
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
        Helpers.checkAccountSumIs(991.0.formatSum())

        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())

        Helpers.checkTitleBarDisplayed(R.string.op_edition)

        onView(withId(R.id.edit_op_sum)).perform(clearText())
        onView(withId(R.id.edit_op_sum)).perform(typeText("-7,50"))

        Helpers.clickOnActionItemConfirm()

        onView(allOf(withText(R.string.update), isDisplayed())).perform(click())
        Helpers.checkAccountSumIs(993.0.formatSum())
    }


    @Test public fun testDelFutureOccurences() {
        TAG = "testDelFutureOccurences"
        val nbOps = Helpers.setupDelOccFromOps()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_following)
        Helpers.checkAccountSumIs((1000.5 - (nbOps - 1)).formatSum())
    }


    @Test public fun testDelAllOccurencesFromOps() {
        TAG = "testDelAllOccurencesFromOps"
        Helpers.setupDelOccFromOps()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)
        onView(withText(R.string.no_operation)).check(matches(isDisplayed()))
        Helpers.checkAccountSumIs(1000.5.formatSum())
    }

    // issue 112 : it was about when clicking on + or - of date chooser then cancel that does not work
    // since android 3, the date picker has no buttons anymore, removed Picker usage
    @Test public fun testCancelSchEdition() {
        TAG = "testCancelSchEdition"
        Helpers.setupDelOccFromOps()
        Helpers.clickInDrawer(R.string.scheduled_ops)

        var date: CharSequence? = Helpers.getTextFrom(R.id.op_date)

        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)

        val today = Tools.createClearedCalendar()
        today.add(Calendar.MONTH, -2)
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)))

        Helpers.clickOnActionItemConfirm()
        Helpers.clickOnDialogButton(R.string.cancel)
        Espresso.pressBack()

        Log.d(TAG, "before date : " + date)
        onView(allOf(withId(R.id.op_date), isDisplayed())).check(matches(withText(equalTo(date.toString()))))
    }

    // issue 59 test
    @Test public fun testDeleteAllOccurences() {
        TAG = "testDeleteAllOccurences"
        Helpers.setUpSchOp()
        onView(withId(R.id.create_operation)).perform(click())

        val today = Tools.createClearedCalendar()
        today.add(Calendar.MONTH, -2)
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)))
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "9,50")
        Helpers.scrollThenTypeText(R.id.edit_op_tag, RadisTest.OP_TAG)
        Helpers.scrollThenTypeText(R.id.edit_op_mode, RadisTest.OP_MODE)
        Helpers.scrollThenTypeText(R.id.edit_op_notes, RadisTest.OP_DESC)

        Helpers.swipePagerLeft()

        onView(withId(R.id.periodicity_choice)).perform(scrollTo())
        onView(withId(R.id.periodicity_choice)).perform(click())
        val strs = getActivity().resources.getStringArray(R.array.periodicity_choices).get(1)
        onData(allOf(iz(instanceOf(String::class.java)), iz(equalTo(strs)))).perform(click())
        Helpers.clickOnActionItemConfirm()

        Espresso.pressBack()

        // TODO assertEquals(3, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())

        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)

        onView(withText(R.string.no_operation_sch)).check(matches(isDisplayed()))

        Espresso.pressBack()

        onView(withText(R.string.no_operation)).check(matches(isDisplayed()))
        Helpers.checkAccountSumIs(1000.5.formatSum())
    }

    /**
     * Infos
     */

    // test adding info with different casing
    @Test public fun testAddExistingInfo() {
        TAG = "testAddExistingInfo"
        Helpers.addAccount()
        onView(withId(R.id.create_operation)).perform(click())

        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        onView(withId(R.id.edit_op_third_parties_list)).perform(scrollTo()).perform(click())
        Helpers.clickOnDialogButton(R.string.create)
        onView(allOf(iz(instanceOf(EditText::class.java)), hasFocus()) as Matcher<View>).perform(replaceText("Atest"))
        Helpers.clickOnDialogButton(R.string.ok)

        onView(allOf(iz(instanceOf(ListView::class.java)), isDisplayed()) as Matcher<View>).check(has(1, ListView::class.java))

        // 3 following lines are hack because a bug of Espresso
        Helpers.pauseTest(1000)
        Helpers.clickOnDialogButton(R.string.cancel)
        Helpers.pauseTest(1000)
        onView(withId(R.id.edit_op_third_parties_list)).perform(scrollTo()).perform(click())

        Helpers.clickOnDialogButton(R.string.create)
        onView(allOf(iz(instanceOf(EditText::class.java)), hasFocus()) as Matcher<View>).perform(replaceText("ATest"))
        Helpers.clickOnDialogButton(R.string.ok)
        onView(withText(R.string.item_exists)).check(matches(isDisplayed()))
        Helpers.clickOnDialogButton(R.string.ok)
    }

    // issue 50 test // TODO : debug
    public fun _estAddInfoAndCreateOp() {
        TAG = "testAddInfoAndCreateOp"
        Helpers.addAccount()
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        onView(withId(R.id.edit_op_third_parties_list)).perform(click())
        Helpers.clickOnDialogButton(R.string.create)
        onView(allOf(iz(instanceOf(EditText::class.java)), hasFocus()) as Matcher<View>).perform(replaceText("Atest"))
        Helpers.clickOnDialogButton(R.string.ok)

        Helpers.pauseTest(1000)

        onView(allOf(iz(instanceOf(ListView::class.java)), isDisplayed()) as Matcher<View>).check(has(1, ListView::class.java))

        onData(iz(instanceOf(Cursor::class.java))).inAdapterView(iz(instanceOf(ListView::class.java)) as Matcher<View>).atPosition(0).perform(click())

        Helpers.pauseTest(1000)

        // BUG here
        //        Helpers.clickOnDialogButton(R.string.ok)
        onView(allOf(isDisplayed(), withText("Ok")) as Matcher<View>).inRoot(DebugEspresso.isAlertDialog()).perform(click())

        Helpers.clickOnActionItemConfirm()
        onView(withId(R.id.create_operation)).perform(click())
        onView(withId(R.id.edit_op_third_parties_list)).perform(click())
        onView(allOf(iz(instanceOf(ListView::class.java)), isDisplayed()) as Matcher<View>).check(has(1, ListView::class.java))
    }

    private fun addOpOnDate(t: DateTime, idx: Int) {
        onView(withId(R.id.create_operation)).perform(click())
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(t.year, t.month, t.day))
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, "$OP_TP/$idx")
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "1")
        Helpers.clickOnActionItemConfirm()
    }

    private fun setUpProjTest1() {
        Helpers.addAccount()
        val today = DateTime.today(TIME_ZONE)
        val cleanUpDay = DateTime.forDateOnly(today.year, today.month, Math.min(today.day, 28))
        var date = cleanUpDay.minusMonth(2)
        for (i in 0..5) {
            addOpOnDate(date, i)
            Helpers.pauseTest(500)
            date = date.plusMonth(1)
        }
    }

    @Test public fun testProjectionFromOpList() {
        TAG = "testProjectionFromOpList"

        // test mode 0 = furthest
        setUpProjTest1()
        val today = DateTime.today(TIME_ZONE)
        val cleanUpDay = DateTime.forDateOnly(today.year, today.month, Math.min(today.day, 28))
        val later3Month = cleanUpDay.plusMonth(3)
        Helpers.pauseTest(1000)
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))
        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(later3Month.formatDateLong()))))

        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkSelectedSumIs(994.50.formatSum())

        // test mode 1 = day of next month
        Helpers.callAccountEdit()

        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)

        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        Helpers.scrollThenTypeText(R.id.projection_date_value, cleanUpDay.day.toString())
        Helpers.clickOnActionItemConfirm()

        val oneMonthLater = cleanUpDay.plusMonth(1)
        Log.d(TAG, "1DATE : " + oneMonthLater)

        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(oneMonthLater.formatDateLong()))))
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(996.50.formatSum()))))

        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkSelectedSumIs(994.50.formatSum())

        // test mode 2 = absolute date
        Helpers.callAccountEdit()
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 2)

        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        val later3Month28th = DateTime.forDateOnly(later3Month.year, later3Month.month, 28)
        val f = SimpleDateFormat("dd/MM/yyyy")
        Helpers.scrollThenTypeText(R.id.projection_date_value, f.format(later3Month28th.getMilliseconds(TIME_ZONE)))
        Helpers.clickOnActionItemConfirm()

        Log.d(TAG, "2DATE : " + f.format(later3Month28th.getMilliseconds(TIME_ZONE)))
        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(later3Month28th.formatDateLong()))))
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))

        onView(withId(R.id.operation_list)).perform(actionOnItemAtPosition <OpRowHolder <Operation>>(5, scrollTo()))
        Helpers.clickOnRecyclerViewAtPos(5)

        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))

        Helpers.checkSelectedSumIs(999.50.formatSum())

        // test back to mode 0
        Helpers.callAccountEdit()
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 0)
        onView(withId(R.id.projection_date_value)).check(matches(not(isDisplayed())))
        Helpers.clickOnActionItemConfirm()

        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))

        Helpers.checkSelectedSumIs(999.50.formatSum())
    }

    public fun addOpMode1() {
        // add account
        Helpers.checkTitleBarDisplayed(R.string.account_creation)
        Helpers.scrollThenTypeText(R.id.edit_account_name, ACCOUNT_NAME)
        Helpers.scrollThenTypeText(R.id.edit_account_start_sum, ACCOUNT_START_SUM)
        Helpers.scrollThenTypeText(R.id.edit_account_desc, ACCOUNT_DESC)
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)
        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        // set projection to a day of next month
        val tomorrow = DateTime.today(TIME_ZONE).plusDays(1)
        Helpers.scrollThenTypeText(R.id.projection_date_value, Integer.toString(tomorrow.day))

        Helpers.pauseTest(10000)

        Helpers.clickOnActionItemConfirm()

        onView(withText(R.string.no_operation)).check(matches(isDisplayed()))
        Helpers.checkAccountSumIs(1000.50.formatSum())

        // add an op today, should change the account sum
        val today = DateTime.today(TIME_ZONE)
        addOpOnDate(today, 0)
        Helpers.pauseTest(1000)
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkSelectedSumIs(999.50.formatSum())


        // add op after proj date, should not change account sum
        val nextMonth = tomorrow.plusMonth(1).plusDays(1)
        addOpOnDate(nextMonth, 1)
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.checkSelectedSumIs(998.50.formatSum())

        // add op before X of next month, should update the current sum
        val prevMonth = today.minusMonth(1)
        addOpOnDate(prevMonth, 2)
        // Log.d(TAG, "addOpMode1 after three add " +
        // solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkAccountSumIs(998.50.formatSum())
        Helpers.checkSelectedSumIs(997.50.formatSum())
    }

    public fun editOpMode1() {
        addOpMode1()

        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "+2")
        Helpers.clickOnActionItemConfirm()

        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(998.50.formatSum())
        Helpers.checkSelectedSumIs(1000.50.formatSum())

        // Log.d(TAG, "editOpMode1 after one edit " + solo.getCurrentListViews().get(0).getCount());

        Helpers.clickOnRecyclerViewAtPos(2)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "+2")
        Helpers.clickOnActionItemConfirm()

        // Log.d(TAG, "editOpMode1 after one edit " + solo.getCurrentListViews().get(0).getCount());

        Helpers.clickOnRecyclerViewAtPos(0)
        //        onView(withId(android.R.id.list)).check(has())
        Helpers.checkAccountSumIs(1001.50.formatSum())
        Helpers.checkSelectedSumIs(1003.50.formatSum())
    }

    private fun delOps() {
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.yes)

        val sum = 1001.50.formatSum()
        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkAccountSumIs(sum)

        Helpers.clickOnRecyclerViewAtPos(1)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.yes)

        Helpers.clickOnRecyclerViewAtPos(0)
        val sum2 = 999.50.formatSum()
        Helpers.checkAccountSumIs(sum2)
        Helpers.checkSelectedSumIs(sum2)
    }

    @Test public fun testDelOpMode1() {
        TAG = "testDelOpMode1"
        editOpMode1()
        delOps()
    }

    public fun addOpMode2() {
        // add account
        Helpers.checkTitleBarDisplayed(R.string.account_creation)

        Helpers.scrollThenTypeText(R.id.edit_account_name, ACCOUNT_NAME)
        Helpers.scrollThenTypeText(R.id.edit_account_start_sum, ACCOUNT_START_SUM)
        Helpers.scrollThenTypeText(R.id.edit_account_desc, ACCOUNT_DESC)

        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)

        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        val today = DateTime.today(TIME_ZONE)
        val tomorrow = today.plusDays(1)

        Helpers.scrollThenTypeText(R.id.projection_date_value,
                "${Integer.toString(tomorrow.day)}/${Integer.toString(tomorrow.month)}/${Integer.toString(tomorrow.year)}")
        Helpers.clickOnActionItemConfirm()
        Helpers.pauseTest(1500)
        Helpers.checkAccountSumIs(1000.50.formatSum())

        addOpOnDate(today, 0)
        // Log.d(TAG, "addOpMode2 after one add " + solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.checkSelectedSumIs(999.50.formatSum())


        // add op after X
        val nextMonth = today.plusMonth(1)
        addOpOnDate(nextMonth, 1)
        // Log.d(TAG, "addOpMode2 after two add " +
        // solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.checkSelectedSumIs(998.50.formatSum())

        // add op before X of next month, should update the current sum
        val prevMonth = today.minusMonth(1)
        addOpOnDate(prevMonth, 2)
        // Log.d(TAG, "addOpMode2 after three add " +
        // solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(998.50.formatSum())
        Helpers.checkSelectedSumIs(997.50.formatSum())
    }

    public fun editOpMode2() {
        addOpMode2()

        Helpers.clickOnRecyclerViewAtPos(0)

        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "+2")
        Helpers.clickOnActionItemConfirm()


        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(998.50.formatSum())
        Helpers.checkSelectedSumIs(1000.50.formatSum())

        // Log.d(TAG, "editOpMode2 after one edit " + solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(2)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "+2")
        Helpers.clickOnActionItemConfirm()

        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(1001.50.formatSum())
        Helpers.checkSelectedSumIs(1003.50.formatSum())
        // Log.d(TAG, "editOpMode2 after two edit " + solo.getCurrentListViews().get(0).getCount());
    }

    @Test public fun testDelOpMode2() {
        TAG = "testDelOpMode2"
        editOpMode2()
        delOps()
    }

    // test transfert

    private fun addTransfertOp() {
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_creation)

        onView(withId(R.id.is_transfert)).perform(click())

        Helpers.pauseTest(700)

        Helpers.clickOnSpinner(R.id.trans_src_account, ACCOUNT_NAME)
        Helpers.clickOnSpinner(R.id.trans_dst_account, ACCOUNT_NAME_2)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        Helpers.scrollThenTypeText(R.id.edit_op_tag, OP_TAG)
        Helpers.scrollThenTypeText(R.id.edit_op_mode, OP_MODE)
        Helpers.scrollThenTypeText(R.id.edit_op_notes, OP_DESC)
        Helpers.clickOnActionItemConfirm()

        Helpers.pauseTest(700)

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)

        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(990.00.formatSum())
        Helpers.checkSelectedSumIs(990.00.formatSum())
        onView(allOf(withId(R.id.op_sum), isDisplayed())).check(matches(withText(containsString((-10.50).formatSum()))))
    }

    public fun simpleTransfert() {
        Helpers.addAccount()
        Helpers.addAccount2()
        addTransfertOp()

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2011.00.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
    }

    @Test public fun testDelSimpleTransfert() {
        TAG = "testDelSimpleTransfert"
        simpleTransfert()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.yes)
        Helpers.checkAccountSumIs(1000.50.formatSum())

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }

    @Test public fun testEditTransfertToNoTransfertAnymore() {
        TAG = "testEditTransfertToNoTransfertAnymore"
        simpleTransfert()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.is_transfert)).perform(click()).check(matches(not(isChecked())))
        Helpers.pauseTest(700)
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.clickOnActionItemConfirm()

        Helpers.checkAccountSumIs(990.00.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }


    @Test public fun testEditSimpleTrans3accounts() {
        TAG = "testEditSimpleTrans3accounts"
        simpleTransfert()
        Helpers.addAccount3()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        Helpers.clickOnSpinner(R.id.trans_dst_account, ACCOUNT_NAME_3)
        Helpers.clickOnActionItemConfirm()

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        Helpers.checkAccountSumIs(990.00.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_3)
        Helpers.checkAccountSumIs(1011.00.formatSum())
    }


    private fun setUpSchTransOp(): DateTime {
        Helpers.addAccount()
        Helpers.addAccount2()
        Helpers.clickInDrawer(R.string.preferences)
        onView(withText(R.string.prefs_insertion_date_label)).perform(click())
        val yesterday = DateTime.today(TIME_ZONE).minusDays(1)
        onView(allOf(iz(instanceOf(EditText::class.java)), hasFocus()) as Matcher<View>).perform(replaceText(Integer.toString(yesterday.day)))
        Espresso.closeSoftKeyboard()
        Helpers.pauseTest(2000) // needed to workaround espresso 2.0 bug
        Helpers.clickOnDialogButton(R.string.ok)
        Helpers.pauseTest(2000) // needed to workaround espresso 2.0 bug
        Espresso.pressBack()
        return yesterday
    }

    private fun addSchTransfert() {
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)

        val today = DateTime.today(TIME_ZONE)
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(today.year,
                today.month, today.day))

        onView(withId(R.id.is_transfert)).perform(click()).check(matches(isChecked()))
        Helpers.pauseTest(700)
        Helpers.clickOnSpinner(R.id.trans_dst_account, ACCOUNT_NAME_2)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        Helpers.scrollThenTypeText(R.id.edit_op_tag, OP_TAG)
        Helpers.scrollThenTypeText(R.id.edit_op_mode, OP_MODE)
        Helpers.scrollThenTypeText(R.id.edit_op_notes, OP_DESC)

        Helpers.clickOnActionItemConfirm()
    }

    public fun makeSchTransfertFromAccList() {
        setUpSchTransOp() // setup insert date to yesterday
        Helpers.clickInDrawer(R.string.scheduled_ops)
        addSchTransfert() // add sch op today
        Espresso.pressBack()
        val today = DateTime.today(TIME_ZONE)

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        val nb = if (today.day == 1) 1 else 2 // if today is the first day of month, we get only 1 insertion, 2 otherwise, yes, this is shitty, need to configure date on the phone
        val sum = nb * 10.50
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
    }

    @Test public fun testDelSchTransfert() {
        TAG = "testDelSchTransfert"
        makeSchTransfertFromAccList()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)
        onView(withText(R.string.no_operation_sch)).check(matches(isDisplayed()))
        Espresso.pressBack()
        Helpers.checkAccountSumIs(1000.50.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }

    private fun addSchTransfertHebdo(dayOfMonthForInsertion: DateTime): Int {
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)
        val today = DateTime.today(TIME_ZONE)
        val schOpDate = today.minusDays(28)

        Log.e(TAG, "date: ${schOpDate.formatDate()}")
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(schOpDate.year,
                schOpDate.month, schOpDate.day))

        onView(withId(R.id.is_transfert)).perform(click()).check(matches(isChecked()))
        Helpers.pauseTest(700)
        Helpers.clickOnSpinner(R.id.trans_dst_account, ACCOUNT_NAME_2)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)

        Helpers.scrollThenTypeText(R.id.edit_op_tag, OP_TAG)
        Helpers.scrollThenTypeText(R.id.edit_op_mode, OP_MODE)
        Helpers.scrollThenTypeText(R.id.edit_op_notes, OP_DESC)

        Helpers.swipePagerLeft()

        Helpers.clickOnSpinner(R.id.periodicity_choice, R.array.periodicity_choices, 0)
        Helpers.clickOnActionItemConfirm()

        val date = Helpers.getTextFrom (R.id.op_date)
        onView(allOf(withId(R.id.op_date), isDisplayed())).check(matches(withText(equalTo(date.toString()))))

        Espresso.pressBack()
        val d = if (today.day >= dayOfMonthForInsertion.day) today else schOpDate
        val endOfMonth = d.plusMonth(1).endOfMonth
        val nb = endOfMonth.getWeekIndex(schOpDate)
        val sum = nb * 10.50
        Log.e(TAG, "addSchTransfertHebdo nb ops inserted: $nb")

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)

        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)

        return nb
    }

    public fun makeSchTransfertHebdoFromAccList(): Int {
        val yesterday = setUpSchTransOp()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        val nb = addSchTransfertHebdo(yesterday)
        return nb
    }

    @Test public fun testDelSchTransfFromOpsList() {
        TAG = "testDelSchTransfFromOpsList"
        val nb = makeSchTransfertHebdoFromAccList()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_only_current)

        val sum = (nb - 1) * 10.50
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
    }

    @Test public fun testDelAllOccSchTransfFromOpsList() {
        TAG = "testDelAllOccSchTransfFromOpsList"
        makeSchTransfertHebdoFromAccList()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)

        Helpers.checkAccountSumIs(1000.50.formatSum())

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }


    @Test public fun testDelFutureSchTransfFromOpsList() {
        TAG = "testDelFutureSchTransfFromOpsList"
        val nb = makeSchTransfertHebdoFromAccList()
        Helpers.clickOnRecyclerViewAtPos(2)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_following)

        val sum = (nb - 3) * 10.50
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
    }

    @Test public fun testDelAllOccSchTransfFromSchList() {
        TAG = "testDelAllOccSchTransfFromSchList"
        makeSchTransfertHebdoFromAccList()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.pauseTest(1000)
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)

        Espresso.pressBack()

        Helpers.checkAccountSumIs(1000.50.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }

    // issue #30 on github
    @Test public fun testSumAtSelectionOnOthersAccount() {
        Helpers.addAccount()
        val today = DateTime.today(TIME_ZONE)
        var date = today.endOfMonth.minusMonth(1)

        for (i in 0..2) {
            addOpOnDate(date, i)
            date = date.plusMonth(1)
        }
        Helpers.clickInDrawer(R.string.account_edit)
        Helpers.checkTitleBarDisplayed(R.string.account_edit_title)
        Helpers.pauseTest(300)
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)
        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        date = today.endOfMonth
        Helpers.scrollThenTypeText(R.id.projection_date_value, Integer.toString(Math.min(date.day, 28)))

        Helpers.clickOnActionItemConfirm()
        Helpers.clickOnRecyclerViewAtPos(1)

        Helpers.checkSelectedSumIs((1000.50 - 2.00).formatSum())

        Helpers.addAccount2()
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.addOp()

        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkSelectedSumIs((2000.50 - 10.50).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)

        Helpers.clickOnRecyclerViewAtPos(1)

        Helpers.checkSelectedSumIs((1000.50 - 2.00).formatSum())
    }

    public fun addOpNoTagsNorMode() {
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_creation)
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        Helpers.clickOnActionItemConfirm()
    }

    // issue #31
    @Test public fun testEmptyInfoCreation() {
        Helpers.addAccount()
        addOpNoTagsNorMode()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_tags_list)).perform(click())
        onView(allOf(iz(instanceOf(ListView::class.java)), isDisplayed()) as Matcher<View>).check(has(0, TextView::class.java))
    }

    // issue #32
    @Test public fun testCorruptedCustomPeriodicity() {
        Helpers.addAccount()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)

        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)

        Helpers.swipePagerLeft()

        Helpers.clickOnSpinner(R.id.periodicity_choice, R.array.periodicity_choices, 3)
        onView(withId(R.id.custom_periodicity_value)).check(matches(isEnabled()))
        onView(withId(R.id.custom_periodicity_value)).perform(typeText(".2"))

        Helpers.swipePagerRight()
        Helpers.swipePagerLeft()
        Helpers.clickOnActionItemConfirm()
    }

    // will not work if launched last day of month
    @Test public fun testOverrideInsertDate() {
        Helpers.addAccount()

        Helpers.clickInDrawer(R.string.preferences)
        Helpers.setInsertDatePref(DateTime.today(TIME_ZONE).plusDays(1))
        Espresso.pressBack()

        // check adding a sch op today is not inserted
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.addScheduleOp(DateTime.today(TIME_ZONE).plusMonth(1))
        onView(withText(R.string.no_operation)).check(matches(isDisplayed()))
        Helpers.checkAccountSumIs(1000.50.formatSum())

        // delete the sch op
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)
        Helpers.checkAccountSumIs(1000.50.formatSum())

        // setup override
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.override_insert_date)).perform(click())
        val default = getActivity().getString(R.string.prefs_insertion_date_text).format(ConfigFragment.DEFAULT_INSERTION_DATE)
        onView(withText(default)).check(matches(isDisplayed()))
        val today = DateTime.today(TIME_ZONE)
        Helpers.setInsertDatePref(today)
        val str = getActivity().getString(R.string.prefs_insertion_date_text).format(today.day.toString())
        onView(withText(str)).check(matches(isDisplayed()))
        Helpers.clickOnActionItemConfirm()

        // check adding a sch op today is inserted
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.addScheduleOp(DateTime.today(TIME_ZONE).plusMonth(1))
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
        Helpers.checkAccountSumIs(991.00.formatSum())
    }


    @Test public fun testNbMonthAheadAndOverride() {
        Helpers.addAccount()
        Helpers.clickInDrawer(R.string.preferences)
        Helpers.setInsertDatePref(DateTime.today(TIME_ZONE))
        Helpers.setEdtPrefValue(R.string.prefs_nb_month_ahead_title, "2")
        Espresso.pressBack()

        // check adding a sch op today inserted 2 months ahead
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.addScheduleOp(DateTime.today(TIME_ZONE).plusMonth(1))
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
        Helpers.checkAccountSumIs(981.50.formatSum())

        // delete all
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)
        Espresso.pressBack()
        Helpers.checkAccountSumIs(1000.50.formatSum())

        // setup override
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.override_nb_month_ahead)).perform(click())
        val default = getActivity().getString(R.string.prefs_nb_month_ahead_text).format(ConfigFragment.DEFAULT_NB_MONTH_AHEAD)
        onView(withText(default)).check(matches(isDisplayed()))
        Helpers.setEdtPrefValue(R.string.prefs_nb_month_ahead_title, "1")
        val str = getActivity().getString(R.string.prefs_nb_month_ahead_text).format(1)
        onView(withText(str)).check(matches(isDisplayed()))
        Helpers.clickOnActionItemConfirm()

        // check adding a sch op today inserted 1 month ahead
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.addScheduleOp(DateTime.today(TIME_ZONE).plusMonth(1))
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
        Helpers.checkAccountSumIs(991.00.formatSum())
    }

    @Test public fun testOverrideHideQuickAdd() {
        Helpers.addAccount()

        fun checkQuickAddVisibilityAs(visible: Boolean) {
            if (visible) {
                onView(withId(R.id.quick_add_layout)).check(matches(isDisplayed()))
            } else {
                onView(withId(R.id.quick_add_layout)).check(matches(not(isDisplayed())))
            }
        }

        // check global pref to false, override activated but let false as value = quick add visible
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.override_hide_quickadd)).perform(click())
        Helpers.clickOnActionItemConfirm()
        checkQuickAddVisibilityAs(true)

        // check global pref to false, override activated, value to true = quick add hidden
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.hide_ops_quick_add)).perform(click())
        Helpers.clickOnActionItemConfirm()
        checkQuickAddVisibilityAs(false)

        // check global pref to false, override deactivated, value to true = quick add visible
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.override_hide_quickadd)).perform(click())
        Helpers.clickOnActionItemConfirm()
        checkQuickAddVisibilityAs(true)

        // GLOBAL PREF = true
        Helpers.clickInDrawer(R.string.preferences)
        onView(withText(R.string.hide_ops_quick_add)).perform(click())
        Espresso.pressBack()

        // check global pref to true, override activated, value to false = quick add visible
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.override_hide_quickadd)).perform(click())
        onView(withText(R.string.hide_ops_quick_add)).perform(click())
        Helpers.clickOnActionItemConfirm()
        checkQuickAddVisibilityAs(true)

        // check global pref to true, override deactivate, value to false = quick add hidden
        Helpers.goToCurAccountOptionPanel()
        onView(withText(R.string.override_hide_quickadd)).perform(click())
        Helpers.clickOnActionItemConfirm()
        checkQuickAddVisibilityAs(false)
    }

    @Test public fun testQuickAddActionOption() {
        Helpers.addAccount()

        // set long press to add today = simple click set to ask date
        Helpers.clickInDrawer(R.string.preferences)
        onView(withId(android.R.id.list)).perform(swipeUp())
        onView(withText(R.string.quick_add_long_press_action_title)).perform(click())
        val addToday = getActivity().resources.getStringArray(R.array.quickadd_actions)[1]
        onView(withText(addToday)).perform(click())
        Espresso.pressBack()

        onView(withId(R.id.account_sum)).check(matches(withText(containsString(1000.50.formatSum()))))
        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(typeText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(click())
        onView(withText(R.string.op_date)).check(matches(isDisplayed()))
        //Helpers.clickOnDialogButton(R.string.cancel) // click on "OK" makes the year doing +1, click on "Cancel" makes day goes +1, Espresso bug?
        Espresso.pressBack() // hack because of the bug above

        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(replaceText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(longClick())
        Helpers.checkAccountSumIs(999.50.formatSum())

        Helpers.goToCurAccountOptionPanel()
        onView(withId(android.R.id.list)).perform(swipeUp())
        onView(withText(R.string.override_quick_add_action)).perform(click())
        onView(withText(R.string.quick_add_long_press_action_title)).perform(click())
        Helpers.pauseTest(800)
        val askDate = getActivity().resources.getStringArray(R.array.quickadd_actions)[0]
        Helpers.pauseTest(800)
        onView(withText(askDate)).perform(click())
        Helpers.clickOnActionItemConfirm()

        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(replaceText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(click())
        Helpers.checkAccountSumIs(998.50.formatSum())
    }

    // test bug where only transaction older than 3 months exist in the account
    @Test public fun testOnlyOldOps() {
        Helpers.addAccount()
        val oldDate = DateTime.today(TIME_ZONE).minusMonth(3)
        Helpers.addOp(oldDate, "Toto", "-1", "", "", "")
        Helpers.checkSelectedSumIs(999.50.formatSum())
    }

    @Test public fun testOldOps() {
        Helpers.addAccount()
        val today = DateTime.today(TIME_ZONE)
        Helpers.addOp(today, "Toto", "-1", "", "", "")
        Helpers.checkSelectedSumIs(999.50.formatSum())

        fun addOpAndCheck(d: DateTime, pos: Int) {
            Helpers.addOp(d, "Toto", "-1", "", "", "")
            Helpers.scrollRecyclerViewToPos(pos)
            Helpers.pauseTest(800)
            Helpers.clickOnRecyclerViewAtPos(pos)
            Helpers.checkSelectedSumIs(999.50.formatSum())
        }

        for (i in 1..15) {
            val d = today.minusMonth(i)
            addOpAndCheck(d, i)
        }
    }


    // Espresso bug on DatePickerDialog prevent this to work
    public fun testOpUpdateDisplay() {
        Helpers.addAccount()
        val today = DateTime.today(TIME_ZONE)
        fun add5ops(d: DateTime) {
            for (i in 0..2) {
                Helpers.addOp(d)
            }
        }

        add5ops(today.minusMonth(1).endOfMonth.minusDays(1))
        add5ops(today.plusMonth(1).endOfMonth.minusDays(1))
        add5ops(today.endOfMonth.minusDays(1))

        onView(withId(R.id.operation_list)).perform(ViewActions.swipeUp())

        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(typeText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(longClick())


        //val date = today.minusMonth(1).getEndOfMonth()
        // -1 on year to work around Espresso bug
        //onView(iz(instanceOf(javaClass<DatePicker>())) as Matcher<View>).perform(PickerActions.setDate(date.getYear() - 1, date.getMonth(), date.getDay()))
        onView(withText(android.R.string.ok)).perform(click())
        Helpers.pauseTest(30000)
        val monthStr = today.minusMonth(1).format("MMMM", Locale.getDefault())
        val m = monthStr.substring(0, 1).capitalize().concat(monthStr.substring(1))
        onView(allOf(withText(m), isDisplayed())).check(matches(isDisplayed()))
    }

    companion object {
        private val WAIT_DIALOG_TIME = 2000

        var TAG = "RadisRobotium"
        val ACCOUNT_NAME = "Test"
        val ACCOUNT_START_SUM = "+1000,50"
        val ACCOUNT_START_SUM_FORMATED_IN_EDITOR = "1 000,50"
        val ACCOUNT_START_SUM_FORMATED_ON_LIST = "1 000,50 €"
        val ACCOUNT_DESC = "Test Description"
        val ACCOUNT_NAME_2 = "Test2"
        val ACCOUNT_START_SUM_2 = "+2000,50"
        val ACCOUNT_START_SUM_FORMATED_ON_LIST_2 = "2 000,50 €"
        val ACCOUNT_DESC_2 = "Test Description 2"
        val ACCOUNT_NAME_3 = "Test3"
        val OP_TP = "Operation 1"
        val OP_AMOUNT = "10.50"
        val OP_AMOUNT_FORMATED = "-10,50"
        val OP_TAG = "Tag 1"
        val OP_MODE = "Carte bleue"
        val OP_DESC = "Robotium Operation 1"
        val OP_AMOUNT_2 = "100"
    }
}
