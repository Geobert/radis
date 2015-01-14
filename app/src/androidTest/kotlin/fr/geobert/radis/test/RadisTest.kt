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

    public fun testEditOp() {
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

    public fun testQuickAddFromOpList() {
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

    public fun testDisableAutoNegate() {
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


    public fun testEditScheduledOp() {
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


    public fun testDelFutureOccurences() {
        TAG = "testDelFutureOccurences"
        val nbOps = Helpers.setupDelOccFromOps()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_following)
        Helpers.checkAccountSumIs((1000.5 - (nbOps - 1)).formatSum())
    }


    public fun testDelAllOccurencesFromOps() {
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
    public fun testCancelSchEdition() {
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

    // issue 59 test
    public fun testDeleteAllOccurences() {
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

    // test adding info with different casing
    public fun testAddExistingInfo() {
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
            Helpers.pauseTest(500)
            today.add(Calendar.MONTH, +1)
        }
    }

    public fun testProjectionFromOpList() {
        TAG = "testProjectionFromOpList"

        // test mode 0
        setUpProjTest1()
        var today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
        today.add(Calendar.MONTH, 3)
        Helpers.pauseTest(300)
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(994.50.formatSum()))))
        onView(withId(R.id.account_balance_at)).check(matches(withText(containsString(Tools.getDateStr(today)))))

        Helpers.clickOnRecyclerViewAtPos(0)

        Helpers.checkSelectedSumIs(994.50.formatSum())

        // test mode 1
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

        // test mode 2
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

        // test back to mode 0
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

    public fun addOpMode2() {
        // add account
        Helpers.checkTitleBarDisplayed(R.string.account_creation)

        Helpers.scrollThenTypeText(R.id.edit_account_name, ACCOUNT_NAME)
        Helpers.scrollThenTypeText(R.id.edit_account_start_sum, ACCOUNT_START_SUM)
        Helpers.scrollThenTypeText(R.id.edit_account_desc, ACCOUNT_DESC)

        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)

        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        val today = Tools.createClearedCalendar()
        today.add(Calendar.DAY_OF_MONTH, 1)
        Helpers.scrollThenTypeText(R.id.projection_date_value,
                "${Integer.toString(today.get(Calendar.DAY_OF_MONTH))}/${Integer.toString(today.get(Calendar.MONTH) + 1)}/${Integer.toString(today.get(Calendar.YEAR))}")
        Helpers.clickOnActionItemConfirm()

        Helpers.checkAccountSumIs(1000.50.formatSum())

        today.add(Calendar.DAY_OF_MONTH, -1)
        addOpOnDate(today, 0)
        // Log.d(TAG, "addOpMode2 after one add " + solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.checkSelectedSumIs(999.50.formatSum())


        // add op after X
        today.add(Calendar.MONTH, +1)
        addOpOnDate(today, 1)
        // Log.d(TAG, "addOpMode2 after two add " +
        // solo.getCurrentListViews().get(0).getCount());
        Helpers.clickOnRecyclerViewAtPos(0)
        Helpers.checkAccountSumIs(999.50.formatSum())
        Helpers.checkSelectedSumIs(998.50.formatSum())

        // add op before X of next month, should update the current sum
        today.add(Calendar.MONTH, -2)
        addOpOnDate(today, 2)
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
        // TODO assertEquals(3, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    }

    public fun testDelOpMode2() {
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

    public fun testDelSimpleTransfert() {
        TAG = "testDelSimpleTransfert"
        simpleTransfert()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.yes)
        Helpers.checkAccountSumIs(1000.50.formatSum())

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }

    public fun testEditTransfertToNoTransfertAnymore() {
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


    public fun testEditSimpleTrans3accounts() {
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


    private fun setUpSchTransOp() {
        Helpers.addAccount()
        Helpers.addAccount2()
        Helpers.clickInDrawer(R.string.preferences)
        onView(withText(R.string.prefs_insertion_date_label)).perform(click())
        val today = Tools.createClearedCalendar()
        today.add(Calendar.DAY_OF_MONTH, -1)
        onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(replaceText(Integer.toString(today.get(Calendar.DAY_OF_MONTH))))
        Espresso.closeSoftKeyboard()
        Helpers.pauseTest(2000) // needed to workaround espresso 2.0 bug
        Helpers.clickOnDialogButton(R.string.ok)
        Espresso.pressBack()
    }

    private fun addSchTransfert() {
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)

        val today = Tools.createClearedCalendar()
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)))

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
        setUpSchTransOp()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        addSchTransfert()
        Espresso.pressBack()
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        Helpers.checkAccountSumIs(990.00.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2011.00.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
    }

    public fun testDelSchTransfert() {
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

    private fun addSchTransfertHebdo() {
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)
        val today = Tools.createClearedCalendar()
        today.add(Calendar.DAY_OF_MONTH, -28)
        Log.d(TAG, "date: ${today.getTime().formatDate()}")
        onView(withId(R.id.edit_op_date)).perform(PickerActions.setDate(today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)))

        onView(withId(R.id.is_transfert)).perform(click()).check(matches(isChecked()))
        Helpers.pauseTest(700)
        Helpers.clickOnSpinner(R.id.trans_dst_account, ACCOUNT_NAME_2)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)

        Helpers.scrollThenTypeText(R.id.edit_op_tag, OP_TAG)
        Helpers.scrollThenTypeText(R.id.edit_op_mode, OP_MODE)
        Helpers.scrollThenTypeText(R.id.edit_op_notes, OP_DESC)

        onView(withText(R.string.scheduling)).perform(click())

        Helpers.clickOnSpinner(R.id.periodicity_choice, R.array.periodicity_choices, 0)
        Helpers.clickOnActionItemConfirm()

        Espresso.pressBack()
        val sum = 9 * 10.50
        Helpers.pauseTest(700)
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
        //        Log.d(TAG, "addSchTransfertHebdo sum = " + sum + " / nb = ")
        //        Log.d(TAG, "addSchTransfertHebdo CUR_ACC_SUM_IDX : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        //        Log.d(TAG, "addSchTransfertHebdo CUR_ACC_SUM_IDX 2 : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME)
    }

    public fun makeSchTransfertHebdoFromAccList() {
        setUpSchTransOp()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        addSchTransfertHebdo()
    }

    public fun testDelSchTransfFromOpsList() {
        TAG = "testDelSchTransfFromOpsList"
        makeSchTransfertHebdoFromAccList()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_only_current)

        val sum = 8 * 10.50
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
    }

    public fun testDelAllOccSchTransfFromOpsList() {
        TAG = "testDelAllOccSchTransfFromOpsList"
        makeSchTransfertHebdoFromAccList()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)

        Helpers.checkAccountSumIs(1000.50.formatSum())

        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }


    public fun testDelFutureSchTransfFromOpsList() {
        TAG = "testDelFutureSchTransfFromOpsList"
        makeSchTransfertHebdoFromAccList()
        Helpers.clickOnRecyclerViewAtPos(2)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_following)

        val sum = 6 * 10.50
        Helpers.checkAccountSumIs((1000.50 - sum).formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs((2000.50 + sum).formatSum())
    }

    public fun testDelAllOccSchTransfFromSchList() {
        TAG = "testDelAllOccSchTransfFromSchList"
        makeSchTransfertHebdoFromAccList()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.delete_op), isDisplayed())).perform(click())
        Helpers.clickOnDialogButton(R.string.del_all_occurrences)

        Espresso.pressBack()

        Helpers.checkAccountSumIs(1000.50.formatSum())
        Helpers.clickOnAccountSpinner(ACCOUNT_NAME_2)
        Helpers.checkAccountSumIs(2000.50.formatSum())
    }

    // issue #30 on github
    public fun testSumAtSelectionOnOthersAccount() {
        Helpers.addAccount()
        var today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
        today.add(Calendar.MONTH, -1)
        for (i in 0..2) {
            addOpOnDate(today, i)
            today.add(Calendar.MONTH, +1)
        }
        Helpers.clickInDrawer(R.string.account_edit)
        Helpers.checkTitleBarDisplayed(R.string.account_edit_title)
        Helpers.pauseTest(300)
        Helpers.clickOnSpinner(R.id.projection_date_spinner, R.array.projection_modes, 1)
        onView(withId(R.id.projection_date_value)).check(matches(isEnabled()))

        today = Tools.createClearedCalendar()
        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
        Helpers.scrollThenTypeText(R.id.projection_date_value, Integer.toString(Math.min(today.get(Calendar.DAY_OF_MONTH), 28)))

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
    public fun testEmptyInfoCreation() {
        Helpers.addAccount()
        addOpNoTagsNorMode()
        Helpers.clickOnRecyclerViewAtPos(0)
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_tags_list)).perform(click())
        onView(allOf(iz(instanceOf(javaClass<ListView>())), isDisplayed()) as Matcher<View>).check(has(0, javaClass<TextView>()))
    }

    // issue #32
    public fun testCorruptedCustomPeriodicity() {
        Helpers.addAccount()
        Helpers.clickInDrawer(R.string.scheduled_ops)
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)

        Helpers.scrollThenTypeText(R.id.edit_op_third_party, OP_TP)
        Helpers.scrollThenTypeText(R.id.edit_op_sum, OP_AMOUNT)

        onView(withText(R.string.scheduling)).perform(click())

        Helpers.clickOnSpinner(R.id.periodicity_choice, R.array.periodicity_choices, 3)
        onView(withId(R.id.custom_periodicity_value)).check(matches(isEnabled()))
        onView(withId(R.id.custom_periodicity_value)).perform(typeText(".2"))

        onView(withText(R.string.basics)).perform(click())
        onView(withText(R.string.scheduling)).perform(click())
        Helpers.clickOnActionItemConfirm()
    }

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
