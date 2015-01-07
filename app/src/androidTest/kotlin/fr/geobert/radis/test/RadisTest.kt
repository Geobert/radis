package fr.geobert.radis.test

import android.content.Intent
import android.test.ActivityInstrumentationTestCase2
import fr.geobert.radis.R
import fr.geobert.radis.tools.*

import android.support.test.espresso.contrib.RecyclerViewActions.*
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.assertion.ViewAssertions.*
import org.hamcrest.Matchers.*

import fr.geobert.radis.ui.adapter.OpRowHolder
import fr.geobert.radis.data.Operation
import fr.geobert.radis.MainActivity
import fr.geobert.radis.db.DbContentProvider
import android.support.test.espresso.ViewAction
import android.view.View
import org.hamcrest.Matcher
import android.support.test.espresso.UiController
import android.widget.TextView
import java.util.Calendar
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.Espresso
import android.util.Log
import android.widget.EditText
import android.widget.ListView
import android.database.Cursor
import java.util.GregorianCalendar
import java.text.SimpleDateFormat
import fr.geobert.espresso.DebugEspresso

public class RadisTest : ActivityInstrumentationTestCase2<MainActivity>(javaClass<MainActivity>()) {

    throws(javaClass<Exception>())
    override fun setUp() {
        super.setUp()
        val i = Intent()
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        setActivityIntent(i)

        val appCtx = getInstrumentation().getTargetContext().getApplicationContext()
        DBPrefsManager.getInstance(appCtx).resetAll()
        val client = appCtx.getContentResolver().acquireContentProviderClient("fr.geobert.radis.db")
        val provider = client.getLocalContentProvider() as DbContentProvider
        provider.deleteDatabase(appCtx)
        client.release()

        Helpers.radisTest = this
        Helpers.activity = getActivity()
    }

    public fun _testEditOp() {
        TAG = "testEditOp"
        Helpers.addManyOps()
        Helpers.clickOnRecyclerViewAtPos(5)
        onView(withId(android.R.id.list)).perform(scrollToPosition<OpRowHolder<Operation>>(6))
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(scrollTo())
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        onView(withId(R.id.edit_op_sum)).perform(typeText("103"))
        Helpers.clickOnActionItemConfirm()
        Helpers.checkAccountSumIs((-2.5).formatSum())
    }

    public fun _testQuickAddFromOpList() {
        TAG = "testQuickAddFromOpList"
        Helpers.addAccount()
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(1000.50.formatSum()))))
        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(typeText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(click())
        // TODO       assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
        Helpers.checkAccountSumIs(999.50.formatSum())
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
    }

    public fun _testDisableAutoNegate() {
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


    public fun _testEditScheduledOp() {
        TAG = "testEditScheduledOp"
        Helpers.addScheduleOp()

        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())

        Helpers.checkTitleBarDisplayed(R.string.op_edition)

        onView(withId(R.id.edit_op_sum)).perform(clearText())
        onView(withId(R.id.edit_op_sum)).perform(typeText("-7,50"))

        Helpers.clickOnActionItemConfirm()

        onView(allOf(withText(R.string.update), isDisplayed())).perform(click())
        Helpers.checkAccountSumIs(993.0.formatSum())
    }


    public fun _testDelFutureOccurences() {
        TAG = "testDelFutureOccurences"
        val nbOps = Helpers.setupDelOccFromOps()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_following)
        Helpers.checkAccountSumIs((1000.5 - (nbOps - 1)).formatSum())
    }


    public fun _testDelAllOccurencesFromOps() {
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
    public fun _testCancelSchEdition() {
        TAG = "testCancelSchEdition"
        Helpers.setupDelOccFromOps()
        Helpers.clickInDrawer(R.string.scheduled_ops)

        var date: CharSequence? = null

        onView(allOf(withId(R.id.op_date), isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), iz(instanceOf(javaClass<TextView>()))) as Matcher<View>
            }

            override fun getDescription(): String {
                return "get date from text view"
            }

            override fun perform(p0: UiController?, p1: View?) {
                val textView = p1 as TextView
                date = textView.getText()
            }

        })

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

    // issue 59 _test
    public fun _testDeleteAllOccurences() {
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

        onView(withText(R.string.scheduling)).perform(click())

        onView(withId(R.id.periodicity_choice)).perform(scrollTo())
        onView(withId(R.id.periodicity_choice)).perform(click())
        val strs = getActivity().getResources().getStringArray(R.array.periodicity_choices).get(1)
        onData(allOf(iz(instanceOf(javaClass<String>())), iz(equalTo(strs)))).perform(click())
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

    // _test adding info with different casing
    public fun _testAddExistingInfo() {
        TAG = "testAddExistingInfo"
        Helpers.addAccount()
        onView(withId(R.id.create_operation)).perform(click())

        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        onView(withId(R.id.edit_op_third_parties_list)).perform(scrollTo()).perform(click())
        Helpers.clickOnDialogButton(R.string.create)
        onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(replaceText("Atest"))
        Helpers.clickOnDialogButton(R.string.ok)

        onView(allOf(iz(instanceOf(javaClass<ListView>())), isDisplayed()) as Matcher<View>).check(has(1, javaClass<ListView>()))

        // 2 following lines are hack because a bug of Espresso
        Helpers.clickOnDialogButton(R.string.cancel)
        onView(withId(R.id.edit_op_third_parties_list)).perform(scrollTo()).perform(click())

        Helpers.clickOnDialogButton(R.string.create)
        onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(replaceText("ATest"))
        Helpers.clickOnDialogButton(R.string.ok)
        onView(withText(R.string.item_exists)).check(matches(isDisplayed()))
        Helpers.clickOnDialogButton(R.string.ok)
    }

    // issue 50 test // TODO : stabilize
    public fun _testAddInfoAndCreateOp() {
        TAG = "testAddInfoAndCreateOp"
        Helpers.addAccount()
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)
        onView(withId(R.id.edit_op_third_parties_list)).perform(click())
        Helpers.clickOnDialogButton(R.string.create)
        onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(replaceText("Atest"))
        Helpers.clickOnDialogButton(R.string.ok)

        onView(allOf(iz(instanceOf(javaClass<ListView>())), isDisplayed()) as Matcher<View>).check(has(1, javaClass<ListView>()))

        onData(iz(instanceOf(javaClass<Cursor>()))).inAdapterView(iz(instanceOf(javaClass<ListView>())) as Matcher<View>).atPosition(0).perform(click())

        onView(withId(android.R.id.button1)).inRoot(DebugEspresso.isAlertDialog()).perform(click())

        Helpers.clickOnActionItemConfirm()
        onView(withId(R.id.create_operation)).perform(click())
        onView(withId(R.id.edit_op_third_parties_list)).perform(click())
        onView(allOf(iz(instanceOf(javaClass<ListView>())), isDisplayed()) as Matcher<View>).check(has(1, javaClass<ListView>()))
    }

    private fun addOpOnDate(t: GregorianCalendar, idx: Int) {
        onView(withId(R.id.create_operation)).perform(click())
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(
                t.get(Calendar.YEAR), t.get(Calendar.MONTH) + 1, t.get(Calendar.DAY_OF_MONTH)
        ))
        Helpers.scrollThenTypeText(R.id.edit_op_third_party, "$OP_TP/$idx")
        Helpers.scrollThenTypeText(R.id.edit_op_sum, "1")
        Helpers.clickOnActionItemConfirm()
    }

    private fun setUpProjTest1() {
        Helpers.addAccount()
        val today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
        today.add(Calendar.MONTH, -2)
        for (i in 0..6 - 1) {
            addOpOnDate(today, i)
            today.add(Calendar.MONTH, +1)
        }
    }

    public fun _testProjectionFromOpList() {
        TAG = "testProjectionFromOpList"

        // _test mode 0
        setUpProjTest1()
        var today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
        today.add(Calendar.MONTH, 3)

        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))
        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(Tools.getDateStr(today)))))

        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkSelectedSumIs(994.50.formatSum())

        // _test mode 1
        Helpers.callAccountEdit()

        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)

        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
        Helpers.scrollThenTypeText(R.id.projection_date_value, Integer.toString(Math.min(today.get(Calendar.DAY_OF_MONTH), 28)))
        Helpers.clickOnActionItemConfirm()

        today.add(Calendar.MONTH, 1)
        Log.d(TAG, "1DATE : " + Tools.getDateStr(today))

        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(Tools.getDateStr(today)))))
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(996.50.formatSum()))))

        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkSelectedSumIs(994.50.formatSum())

        // _test mode 2
        Helpers.callAccountEdit()
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 2)

        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, 28)
        today.add(Calendar.MONTH, +3)
        val f = SimpleDateFormat("dd/MM/yyyy")
        Helpers.scrollThenTypeText(R.id.projection_date_value, f.format(today.getTime()))
        Helpers.clickOnActionItemConfirm()

        Log.d(TAG, "2DATE : " + f.format(today.getTime()))
        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(Tools.getDateStr(today)))))
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))

        onView(withId(android.R.id.list)).perform(actionOnItemAtPosition <OpRowHolder <Operation>>(5, scrollTo()))
        Helpers.clickOnRecyclerViewAtPos(5)

        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))

        Helpers.checkSelectedSumIs(999.50.formatSum())

        // _test back to mode 0
        Helpers.callAccountEdit()
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 0)
        onView(withId(R.id.projection_date_value)).check(matches(not(isEnabled())))
        Helpers.clickOnActionItemConfirm()

        Log.d(TAG, "0DATE : " + Tools.getDateStr(today))

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

        val today = Tools.createClearedCalendar()
        today.add(Calendar.DAY_OF_MONTH, 1)

        Helpers.scrollThenTypeText(R.id.projection_date_value, Integer.toString(today.get(Calendar.DAY_OF_MONTH)))
        Helpers.clickOnActionItemConfirm()

        onView(withText(R.string.no_operation)).check(matches(isDisplayed()))
        Helpers.checkAccountSumIs(1000.50.formatSum())

        //        Log.d(TAG, "addOpMode1 before add " + solo.getCurrentViews(ListView.class).get(0).getCount());
        today.add(Calendar.DAY_OF_MONTH, -1)
        addOpOnDate(today, 0)

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        Helpers.checkAccountSumIs(999.50.formatSum())

        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkSelectedSumIs(999.50.formatSum())

        //        Log.d(TAG, "addOpMode1 after one add " + solo.getCurrentViews(ListView.class).get(0).getCount());
        // add op after X
        today.add(Calendar.MONTH, +1)
        addOpOnDate(today, 1)

        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.checkSelectedSumIs(998.50.formatSum())

        //        Log.d(TAG, "addOpMode1 after two add " + solo.getCurrentViews(ListView.class).get(0).getCount());
        // add op before X of next month, should update the current sum
        today.add(Calendar.MONTH, -2)
        addOpOnDate(today, 2)
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

    //    public fun addOpMode2() {
    //        // add account
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<AccountEditor>()))
    //        solo!!.enterText(0, ACCOUNT_NAME)
    //        solo!!.enterText(1, ACCOUNT_START_SUM)
    //        solo!!.enterText(4, ACCOUNT_DESC)
    //        solo!!.pressSpinnerItem(1, 2)
    //        tools!!.hideKeyboard()
    //        Assert.assertTrue(solo!!.waitForView(javaClass<EditText>()))
    //        Assert.assertTrue(solo!!.getEditText(3).isEnabled())
    //        val today = Tools.createClearedCalendar()
    //        today.add(Calendar.DAY_OF_MONTH, 1)
    //        solo!!.enterText(3, Integer.toString(today.get(Calendar.DAY_OF_MONTH)) + "/" + Integer.toString(today.get(Calendar.MONTH) + 1) + "/" + Integer.toString(today.get(Calendar.YEAR)))
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.50.formatSum()))
    //        today.add(Calendar.DAY_OF_MONTH, -1)
    //        addOpOnDate(today, 0, javaClass<OperationListFragment>())
    //        // Log.d(TAG, "addOpMode2 after one add " + solo.getCurrentListViews().get(0).getCount());
    //        tools!!.printCurrentTextViews()
    //        solo!!.clickInList(0)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(999.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(999.50.formatSum()))
    //
    //        // add op after X
    //        today.add(Calendar.MONTH, +1)
    //        addOpOnDate(today, 1, javaClass<OperationListFragment>())
    //        // Log.d(TAG, "addOpMode2 after two add " +
    //        // solo.getCurrentListViews().get(0).getCount());
    //        solo!!.clickInList(0)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(999.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(998.50.formatSum()))
    //
    //        // add op before X of next month, should update the current sum
    //        today.add(Calendar.MONTH, -2)
    //        addOpOnDate(today, 2, javaClass<OperationListFragment>())
    //        // Log.d(TAG, "addOpMode2 after three add " +
    //        // solo.getCurrentListViews().get(0).getCount());
    //        solo!!.clickInList(0)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(998.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(997.50.formatSum()))
    //    }
    //
    //    public fun editOpMode2() {
    //        addOpMode2()
    //
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        solo!!.clearEditText(SUM_FIELD_IDX)
    //        solo!!.enterText(SUM_FIELD_IDX, "+2")
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.clickInList(0)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(998.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(1000.50.formatSum()))
    //        // Log.d(TAG, "editOpMode2 after one edit " + solo.getCurrentListViews().get(0).getCount());
    //        solo!!.clickInList(3)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        solo!!.clearEditText(4)
    //        solo!!.enterText(4, "+2")
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.clickInList(0)
    //        // Log.d(TAG, "editOpMode2 after two edit " + solo.getCurrentListViews().get(0).getCount());
    //        assertEquals(3, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1001.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(1003.50.formatSum()))
    //    }

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

    public fun testDelOpMode1() {
        TAG = "testDelOpMode1"
        editOpMode1()
        delOps()
    }

    //
    //    public fun _testDelOpMode2() {
    //        TAG = "testDelOpMode2"
    //        editOpMode2()
    //        delOps()
    //    }
    //
    //    // _test transfert
    //
    //    private fun addTransfertOp() {
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<CheckBox>()))
    //        solo!!.clickOnCheckBox(0)
    //        solo!!.pressSpinnerItem(0, -2)
    //        solo!!.pressSpinnerItem(0, 1)
    //        solo!!.pressSpinnerItem(1, 2)
    //        for (i in 0..OP_AMOUNT.length() - 1) {
    //            solo!!.enterText(3, String.valueOf(OP_AMOUNT.charAt(i)))
    //        }
    //        solo!!.enterText(4, OP_TAG)
    //        solo!!.enterText(5, OP_MODE)
    //        solo!!.enterText(6, OP_DESC)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.pressSpinnerItem(0, -1)
    //        tools!!.sleep(600)
    //        tools!!.printCurrentTextViews()
    //        solo!!.clickInList(0)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(990.00.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(990.00.formatSum()))
    //        Assert.assertTrue(solo!!.getText(8).getText().toString().equals((-10.50).formatSum()))
    //    }
    //
    //    public fun simpleTransfert() {
    //        addAccount()
    //        addAccount2()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        addTransfertOp()
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2011.00.formatSum()))
    //        solo!!.pressSpinnerItem(0, -1)
    //    }
    //
    //    public fun _testDelSimpleTransfert() {
    //        TAG = "testDelSimpleTransfert"
    //        simpleTransfert()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.yes))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.50.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2000.50.formatSum()))
    //    }
    //
    //    public fun _testEditTransfertToNoTransfertAnymore() {
    //        TAG = "testEditTransfertToNoTransfertAnymore"
    //        simpleTransfert()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        tools!!.sleep(1000)
    //        solo!!.clickOnCheckBox(0)
    //        solo!!.enterText(3, OP_TP)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(990.00.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2000.50.formatSum()))
    //    }
    //
    //    public fun _testEditSimpleTrans3accounts() {
    //        TAG = "testEditSimpleTrans3accounts"
    //        simpleTransfert()
    //        addAccount3()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<Spinner>()))
    //        solo!!.pressSpinnerItem(1, 1)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        tools!!.printCurrentTextViews()
    //        solo!!.pressSpinnerItem(0, -1)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(990.00.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2000.50.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1011.00.formatSum()))
    //    }
    //
    //    private fun setUpSchTransOp() {
    //        addAccount()
    //        addAccount2()
    //        clickInDrawer(R.string.preferences)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<RadisConfiguration>()))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.prefs_insertion_date_label)))
    //        solo!!.clickOnText(solo!!.getString(R.string.prefs_insertion_date_label))
    //        solo!!.clearEditText(0)
    //        val today = Tools.createClearedCalendar()
    //        today.add(Calendar.DAY_OF_MONTH, -1)
    //        solo!!.enterText(0, Integer.toString(today.get(Calendar.DAY_OF_MONTH)))
    //        solo!!.clickOnButton(solo!!.getString(R.string.ok))
    //        solo!!.goBack()
    //    }
    //
    //    private fun addSchTransfert() {
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<ScheduledOperationEditor>()))
    //        val today = Tools.createClearedCalendar()
    //        solo!!.setDatePicker(0, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    //        solo!!.clickOnCheckBox(0)
    //        Assert.assertTrue(solo!!.waitForView(javaClass<Spinner>()))
    //        solo!!.pressSpinnerItem(1, 2)
    //        for (i in 0..OP_AMOUNT.length() - 1) {
    //            solo!!.enterText(3, String.valueOf(OP_AMOUNT.charAt(i)))
    //        }
    //        solo!!.enterText(4, OP_TAG)
    //        solo!!.enterText(5, OP_MODE)
    //        solo!!.enterText(6, OP_DESC)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //    }
    //
    //    public fun makeSchTransfertFromAccList() {
    //        setUpSchTransOp()
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        addSchTransfert()
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        solo!!.pressSpinnerItem(0, -1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(990.00.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2011.00.formatSum()))
    //        solo!!.pressSpinnerItem(0, -1)
    //    }
    //
    //    public fun _testDelSchTransfert() {
    //        TAG = "testDelSchTransfert"
    //        makeSchTransfertFromAccList()
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        solo!!.clickInList(0)
    //        solo!!.clickOnView(solo!!.getView(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.del_all_occurrences))
    //        tools!!.sleep(1000)
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.no_operation_sch)))
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.50.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2000.50.formatSum()))
    //    }
    //
    //    private fun addSchTransfertHebdo() {
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<ScheduledOperationEditor>()))
    //        val today = Tools.createClearedCalendar()
    //        today.set(Calendar.DAY_OF_MONTH, 12)
    //        today.add(Calendar.MONTH, -1)
    //        solo!!.setDatePicker(0, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    //        solo!!.clickOnCheckBox(0)
    //        Assert.assertTrue(solo!!.waitForView(javaClass<Spinner>()))
    //        solo!!.pressSpinnerItem(1, 2)
    //        for (i in 0..OP_AMOUNT.length() - 1) {
    //            solo!!.enterText(3, String.valueOf(OP_AMOUNT.charAt(i)))
    //        }
    //        solo!!.enterText(4, OP_TAG)
    //        solo!!.enterText(5, OP_MODE)
    //        solo!!.enterText(6, OP_DESC)
    //        tools!!.scrollUp()
    //        solo!!.clickOnText(solo!!.getString(R.string.scheduling))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.account)))
    //        solo!!.pressSpinnerItem(1, -1)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        tools!!.sleep(500)
    //        tools!!.printCurrentTextViews()
    //        val n = solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount()
    //        val sum = n.toDouble() * 10.50
    //        solo!!.pressSpinnerItem(0, -1)
    //        Log.d(TAG, "addSchTransfertHebdo sum = " + sum + " / nb = " + n)
    //        Log.d(TAG, "addSchTransfertHebdo CUR_ACC_SUM_IDX : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((1000.50 - sum).formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        Log.d(TAG, "addSchTransfertHebdo CUR_ACC_SUM_IDX 2 : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((2000.50 + sum).formatSum()))
    //        solo!!.pressSpinnerItem(0, -1)
    //    }
    //
    //    public fun makeSchTransfertHebdoFromAccList() {
    //        setUpSchTransOp()
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        addSchTransfertHebdo()
    //    }
    //
    //    public fun _testDelSchTransfFromOpsList() {
    //        TAG = "testDelSchTransfFromOpsList"
    //        makeSchTransfertHebdoFromAccList()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnText(solo!!.getString(R.string.del_only_current))
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        val n = solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount()
    //        val sum = n.toDouble() * 10.50
    //        Log.d(TAG, "CUR_ACC_SUM_IDX: " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((1000.50 - sum).formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Log.d(TAG, "CUR_ACC_SUM_IDX2: " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((2000.50 + sum).formatSum()))
    //    }
    //
    //    public fun _testDelAllOccSchTransfFromOpsList() {
    //        TAG = "testDelAllOccSchTransfFromOpsList"
    //        makeSchTransfertHebdoFromAccList()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnText(solo!!.getString(R.string.del_all_occurrences))
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        val startSum = 1000.50.formatSum()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(startSum))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2000.50.formatSum()))
    //    }
    //
    //    public fun _testDelFutureSchTransfFromOpsList() {
    //        TAG = "testDelFutureSchTransfFromOpsList"
    //        makeSchTransfertHebdoFromAccList()
    //        solo!!.clickInList(3)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnText(solo!!.getString(R.string.del_all_following))
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        val n = solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount()
    //        val sum = n.toDouble() * 10.50
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((1000.50 - sum).formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((2000.50 + sum).formatSum()))
    //    }
    //
    //    public fun _testDelAllOccSchTransfFromSchList() {
    //        TAG = "testDelAllOccSchTransfFromSchList"
    //        makeSchTransfertHebdoFromAccList()
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        solo!!.clickInList(0)
    //        solo!!.clickOnView(solo!!.getView(R.id.delete_op))
    //        solo!!.clickOnText(solo!!.getString(R.string.del_all_occurrences))
    //        tools!!.sleep(1000)
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.50.formatSum()))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(2000.50.formatSum()))
    //    }
    //
    //    // issue #30 on github
    //    public fun _testSumAtSelectionOnOthersAccount() {
    //        addAccount()
    //        var today = Tools.createClearedCalendar()
    //        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
    //        today.add(Calendar.MONTH, -1)
    //        for (i in 0..3 - 1) {
    //            addOpOnDate(today, i, javaClass<OperationListFragment>())
    //            today.add(Calendar.MONTH, +1)
    //        }
    //        clickInDrawer(R.string.account_edit)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<AccountEditor>()))
    //        solo!!.pressSpinnerItem(1, 1)
    //        tools!!.hideKeyboard()
    //        Assert.assertTrue(solo!!.waitForView(javaClass<EditText>()))
    //        Assert.assertTrue(solo!!.getCurrentViews(javaClass<EditText>()).get(3).isEnabled())
    //        today = Tools.createClearedCalendar()
    //        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
    //        solo!!.enterText(3, Integer.toString(Math.min(today.get(Calendar.DAY_OF_MONTH), 28)))
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.clickInList(2)
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(11).getText().toString().contains((1000.50 - 2.00).formatSum()))
    //
    //        addAccount2()
    //        solo!!.pressSpinnerItem(0, 1)
    //        addOp()
    //        solo!!.clickInList(0)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains((2000.50 - 10.50).formatSum()))
    //
    //        solo!!.pressSpinnerItem(0, -1)
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.clickInList(2)
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(11).getText().toString().contains((1000.50 - 2.00).formatSum()))
    //    }
    //
    //    public fun addOpNoTagsNorMode() {
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        solo!!.enterText(3, OP_TP)
    //        solo!!.enterText(4, OP_AMOUNT)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //    }
    //
    //    // issue #31
    //    public fun _testEmptyInfoCreation() {
    //        addAccount()
    //        addOpNoTagsNorMode()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        Assert.assertTrue(solo!!.waitForView(R.id.edit_op_tags_list))
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op_tags_list))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        assertEquals(0, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    //    }
    //
    //    // issue #32
    //    public fun _testCorruptedCustomPeriodicity() {
    //        addAccount()
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<ScheduledOperationEditor>()))
    //        solo!!.enterText(3, OP_TP)
    //        solo!!.enterText(4, OP_AMOUNT)
    //        tools!!.scrollUp()
    //        solo!!.clickOnText(solo!!.getString(R.string.scheduling))
    //        solo!!.pressSpinnerItem(1, 2)
    //        solo!!.enterText(0, ".")
    //        solo!!.enterText(0, "2")
    //        solo!!.clickOnText(solo!!.getString(R.string.basics))
    //        solo!!.clickOnText(solo!!.getString(R.string.scheduling))
    //        tools!!.sleep(5000)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //    }

    class object {
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
