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

    override fun tearDown() {
        super.tearDown()
        getActivity().finish()
    }

    public fun testEditOp() {
        TAG = "testEditOp"
        Helpers.addManyOps()
        onView(withId(android.R.id.list)).perform(actionOnItemAtPosition<OpRowHolder<Operation>>(5, click()))
        onView(withId(android.R.id.list)).perform(scrollToPosition<OpRowHolder<Operation>>(6))
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_edition)
        onView(withId(R.id.edit_op_sum)).perform(scrollTo())
        onView(withId(R.id.edit_op_sum)).perform(clearText())
        onView(withId(R.id.edit_op_sum)).perform(typeText("103"))
        Helpers.clickOnActionItemConfirm()
        onView(withId(R.id.account_sum)).check(matches(withText(containsString((-2.5).formatSum()))))
    }

    public fun testQuickAddFromOpList() {
        TAG = "testQuickAddFromOpList"
        Helpers.addAccount()
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(1000.50.formatSum()))))
        onView(withId(R.id.quickadd_third_party)).perform(typeText("Toto"))
        onView(withId(R.id.quickadd_amount)).perform(typeText("-1"))
        onView(withId(R.id.quickadd_validate)).perform(click())
        // TODO       assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(999.50.formatSum()))))
        onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
    }

    public fun testDisableAutoNegate() {
        TAG = "testDisableAutoNegate"
        Helpers.addAccount()
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.op_creation)
        onView(withId(R.id.edit_op_third_party)).perform(scrollTo())
        onView(withId(R.id.edit_op_third_party)).perform(typeText(OP_TP))
        onView(withId(R.id.edit_op_sum)).perform(scrollTo())
        onView(withId(R.id.edit_op_sum)).perform(typeText("+$OP_AMOUNT"))
        onView(withId(R.id.edit_op_sum)).check(matches(withText(equalTo("+10,50"))))
        Helpers.clickOnActionItemConfirm()
        onView(withId(R.id.account_sum)).check(matches(withText(containsString(1011.00.formatSum()))))
        onView(withId(R.id.op_sum)).check(matches(withText(equalTo(10.50.formatSum()))))
    }

    /**
     * Schedule ops
     */


    public fun testEditScheduledOp() {
        TAG = "testEditScheduledOp"
        Helpers.addScheduleOp()

        onView(withId(android.R.id.list)).perform(actionOnItemAtPosition<OpRowHolder<Operation>>(0, click()))
        onView(allOf(withId(R.id.edit_op), isDisplayed())).perform(click())

        Helpers.checkTitleBarDisplayed(R.string.op_edition)

        onView(withId(R.id.edit_op_sum)).perform(clearText())
        onView(withId(R.id.edit_op_sum)).perform(typeText("-7,50"))

        Helpers.clickOnActionItemConfirm()

        onView(allOf(withText(R.string.update), isDisplayed())).perform(click())

        onView(withId(R.id.account_sum)).check(matches(withText(containsString(993.00.formatSum()))))
    }
    //
    //    private fun setupDelOccFromOps(): Int {
    //        setUpSchOp()
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<ScheduledOperationEditor>()))
    //        val today = Tools.createClearedCalendar()
    //        today.add(Calendar.DAY_OF_MONTH, -14)
    //        solo!!.setDatePicker(0, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    //        solo!!.enterText(3, OP_TP)
    //        solo!!.enterText(4, "1,00")
    //        solo!!.enterText(5, OP_TAG)
    //        solo!!.enterText(6, OP_MODE)
    //        solo!!.enterText(7, OP_DESC)
    //        solo!!.clickOnText(solo!!.getString(R.string.scheduling))
    //        solo!!.pressSpinnerItem(1, -1)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        tools!!.scrollDown()
    //        tools!!.sleep(1000)
    //        val nbOps = solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount()
    //        tools!!.printCurrentTextViews()
    //        Log.d(TAG, "interface text : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString() + " / " + (1000.5 - nbOps.toDouble()).formatSum())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((1000.5 - nbOps.toDouble()).formatSum()))
    //        return nbOps
    //    }
    //
    //    public fun testDelFutureOccurences() {
    //        TAG = "testDelFutureOccurences"
    //        val nbOps = setupDelOccFromOps()
    //        Log.d(TAG, "nbOPS : " + nbOps)
    //        solo!!.clickInList(nbOps - (nbOps - 2))
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.del_all_following))
    //        tools!!.sleep(1000)
    //        //        solo.clickInList(solo.getCurrentViews(ListView.class).get(0).getCount());
    //        //        assertEquals(2, solo.getCurrentViews(ListView.class).get(0).getCount() - 1);
    //        val newNbOps = solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount()
    //        Assert.assertTrue(newNbOps < nbOps)
    //        tools!!.printCurrentTextViews()
    //        Log.d(TAG, "interface text : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString() + " / " + (1000.5 - newNbOps.toDouble()).formatSum() + " nbops / newnbops " + nbOps + " / " + newNbOps)
    //        Assert.assertTrue(solo!!.waitForText((1000.5 - newNbOps.toDouble()).formatSum()))
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains((1000.5 - newNbOps.toDouble()).formatSum()))
    //    }
    //
    //    public fun testDelAllOccurencesFromOps() {
    //        TAG = "testDelAllOccurencesFromOps"
    //        val nbOps = setupDelOccFromOps()
    //        solo!!.clickInList(nbOps - (nbOps - 2))
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.del_all_occurrences))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.no_operation)))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.5.formatSum()))
    //    }
    //
    //    // issue 112 : it was about when clicking on + or - of date chooser then cancel that does not work
    //    // since android 3, the date picker has no buttons anymore, removed Picker usage
    //    public fun testCancelSchEdition() {
    //        TAG = "testCancelSchEdition"
    //        //        Picker picker = new Picker(solo);
    //        setupDelOccFromOps()
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        tools!!.printCurrentTextViews()
    //        val date = solo!!.getCurrentViews(javaClass<TextView>()).get(2).getText()
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<ScheduledOperationEditor>()))
    //        Assert.assertTrue(solo!!.waitForDialogToClose(WAIT_DIALOG_TIME))
    //        val today = Tools.createClearedCalendar()
    //        today.add(Calendar.MONTH, -2)
    //        //        picker.clickOnDatePicker(today.get(Calendar.MONTH) + 1,
    //        //                today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.YEAR));
    //        solo!!.setDatePicker(0, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        solo!!.clickOnButton(solo!!.getString(R.string.cancel))
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        tools!!.printCurrentTextViews()
    //        Log.d(TAG, "before date : " + date)
    //        assertEquals(date, solo!!.getCurrentViews(javaClass<TextView>()).get(2).getText())
    //    }
    //
    //    // issue 59 test
    //    public fun testDeleteAllOccurences() {
    //        TAG = "testDeleteAllOccurences"
    //        setUpSchOp()
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        val today = Tools.createClearedCalendar()
    //        today.add(Calendar.MONTH, -2)
    //        solo!!.setDatePicker(0, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    //        solo!!.enterText(3, OP_TP)
    //        solo!!.enterText(4, "9,50")
    //        solo!!.enterText(5, OP_TAG)
    //        solo!!.enterText(6, OP_MODE)
    //        solo!!.enterText(7, OP_DESC)
    //        tools!!.scrollUp()
    //        solo!!.clickOnText(solo!!.getString(R.string.scheduling))
    //        solo!!.pressSpinnerItem(0, 1)
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        tools!!.sleep(1000)
    //        assertEquals(3, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    //        clickInDrawer(R.string.scheduled_ops)
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<ScheduledOpListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.clickInList(0)
    //        solo!!.clickOnView(solo!!.getView(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.del_all_occurrences))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.no_operation_sch)))
    //        solo!!.goBack()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.no_operation)))
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.50.formatSum()))
    //    }
    //
    //    /**
    //     * Infos
    //     */
    //
    //    // test adding info with different casing
    //    public fun testAddExistingInfo() {
    //        TAG = "testAddExistingInfo"
    //        setUpOpTest()
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        solo!!.enterText(3, OP_TP)
    //        for (i in 0..OP_AMOUNT.length() - 1) {
    //            solo!!.enterText(4, String.valueOf(OP_AMOUNT.charAt(i)))
    //        }
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op_third_parties_list))
    //        solo!!.clickOnButton(solo!!.getString(R.string.create))
    //        solo!!.enterText(0, "Atest")
    //        solo!!.clickOnButton(solo!!.getString(R.string.ok))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    //        solo!!.clickOnButton(solo!!.getString(R.string.create))
    //        solo!!.enterText(0, "ATest")
    //        solo!!.clickOnButton(solo!!.getString(R.string.ok))
    //        Assert.assertNotNull(solo!!.getText(solo!!.getString(R.string.item_exists)))
    //        solo!!.clickOnButton(solo!!.getString(R.string.ok))
    //    }
    //
    //    // issue 50 test
    //    public fun testAddInfoAndCreateOp() {
    //        TAG = "testAddInfoAndCreateOp"
    //        setUpOpTest()
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        solo!!.enterText(3, OP_TP)
    //        for (i in 0..OP_AMOUNT.length() - 1) {
    //            solo!!.enterText(4, String.valueOf(OP_AMOUNT.charAt(i)))
    //        }
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op_third_parties_list))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.create)))
    //        solo!!.clickOnButton(solo!!.getString(R.string.create))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<EditText>()))
    //        solo!!.enterText(0, "Atest")
    //        solo!!.clickOnButton(solo!!.getString(R.string.ok))
    //        tools!!.sleep(1000)
    //        solo!!.clickInList(0)
    //        tools!!.sleep(500)
    //        solo!!.clickOnButton(solo!!.getString(R.string.ok))
    //        Assert.assertTrue(solo!!.waitForDialogToClose(500))
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ImageButton>()))
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op_third_parties_list))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    //    }
    //
    //    private fun addOpOnDate(t: GregorianCalendar, idx: Int, fragmentClass: Class<Any>) {
    //        solo!!.clickOnActionBarItem(R.id.create_operation)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<DatePicker>()))
    //        solo!!.setDatePicker(0, t.get(Calendar.YEAR), t.get(Calendar.MONTH), t.get(Calendar.DAY_OF_MONTH))
    //        solo!!.enterText(3, OP_TP + "/" + idx)
    //        solo!!.enterText(4, "1")
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(fragmentClass.getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //    }
    //
    //    private fun setUpProjTest1() {
    //        addAccount()
    //        val today = Tools.createClearedCalendar()
    //        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
    //        today.add(Calendar.MONTH, -2)
    //        for (i in 0..6 - 1) {
    //            addOpOnDate(today, i, javaClass<OperationListFragment>())
    //            today.add(Calendar.MONTH, +1)
    //        }
    //    }
    //
    //    public fun testProjectionFromOpList() {
    //        TAG = "testProjectionFromOpList"
    //
    //        // test mode 0
    //        setUpProjTest1()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        var today = Tools.createClearedCalendar()
    //        today.set(Calendar.DAY_OF_MONTH, Math.min(today.get(Calendar.DAY_OF_MONTH), 28))
    //        today.add(Calendar.MONTH, 3)
    //        tools!!.printCurrentTextViews()
    //        var projSumTxt = solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_SUM_IDX).getText().toString()
    //        var projDateTxt = solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_PROJ_DATE_IDX).getText().toString()
    //        Log.d(TAG, "testProjectionFromOpList : " + Tools.getDateStr(today) + " VS " + projDateTxt + "/" + projSumTxt)
    //        Assert.assertTrue(projDateTxt.contains(Tools.getDateStr(today)))
    //        Assert.assertTrue(projSumTxt.contains(994.50.formatSum()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        tools!!.scrollUp()
    //        solo!!.clickInList(0)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(994.50.formatSum()))
    //
    //        // test mode 1
    //        callAccountEdit()
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
    //        today.add(Calendar.MONTH, 1)
    //        Log.d(TAG, "1DATE : " + Tools.getDateStr(today))
    //        projDateTxt = solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_PROJ_DATE_IDX).getText().toString()
    //        Log.d(TAG, "1DATE displayed : " + projDateTxt)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(projDateTxt.contains(Tools.getDateStr(today)))
    //        Assert.assertTrue(solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_SUM_IDX).getText().toString().contains(996.50.formatSum()))
    //        solo!!.clickInList(0)
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(994.50.formatSum()))
    //
    //        // test mode 2
    //        callAccountEdit()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<AccountEditor>()))
    //        solo!!.pressSpinnerItem(1, 1)
    //        tools!!.hideKeyboard()
    //        Assert.assertTrue(solo!!.waitForView(javaClass<EditText>()))
    //        Assert.assertTrue(solo!!.getEditText(3).isEnabled())
    //        today = Tools.createClearedCalendar()
    //        today.set(Calendar.DAY_OF_MONTH, 28)
    //        today.add(Calendar.MONTH, +3)
    //        val f = SimpleDateFormat("dd/MM/yyyy")
    //        solo!!.enterText(3, f.format(today.getTime()))
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        Log.d(TAG, "2DATE : " + f.format(today.getTime()))
    //        projDateTxt = solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_PROJ_DATE_IDX).getText().toString()
    //        Log.d(TAG, "2DATE displayed : " + projDateTxt)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(projDateTxt.contains(Tools.getDateStr(today)))
    //        solo!!.clickInList(0)
    //        //        assertTrue(solo.getText(1).getText().toString().contains("994,50"));
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(994.50.formatSum()))
    //        tools!!.scrollDown()
    //        tools!!.sleep(1000)
    //        tools!!.scrollDown()
    //        tools!!.sleep(1000)
    //        tools!!.scrollDown()
    //        tools!!.sleep(1000)
    //        solo!!.clickInList(5)
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_SUM_IDX).getText().toString().contains(994.50.formatSum()))
    //        Log.d(TAG, "solo.getText(24).getText() : " + solo!!.getText(24).getText() + " / " + 998.50.formatSum())
    //        Assert.assertTrue(solo!!.getText(24).getText().toString().contains(998.50.formatSum()))
    //
    //        // test back to mode 0
    //        callAccountEdit()
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<AccountEditor>()))
    //        solo!!.pressSpinnerItem(1, -2)
    //        tools!!.hideKeyboard()
    //        Assert.assertTrue(solo!!.waitForView(javaClass<EditText>()))
    //        Assert.assertFalse(solo!!.getEditText(3).isEnabled())
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        Log.d(TAG, "0DATE : " + Tools.getDateStr(today))
    //        projDateTxt = solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_PROJ_DATE_IDX).getText().toString()
    //        projSumTxt = solo!!.getCurrentViews(javaClass<TextView>()).get(CUR_ACC_SUM_IDX).getText().toString()
    //        Log.d(TAG, "0DATE displayed : " + projDateTxt)
    //        Log.d(TAG, "solo.getButton(0).getText() : " + projSumTxt)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(projSumTxt.contains(994.50.formatSum()))
    //        assertEquals(solo!!.getCurrentViews(javaClass<TextView>()).get(27).getText().toString(), 998.50.formatSum())
    //    }
    //
    //    public fun addOpMode1() {
    //        // add account
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<AccountEditor>()))
    //        solo!!.enterText(0, ACCOUNT_NAME)
    //        solo!!.enterText(1, ACCOUNT_START_SUM)
    //        solo!!.enterText(4, ACCOUNT_DESC)
    //
    //        solo!!.pressSpinnerItem(1, 1)
    //        tools!!.hideKeyboard()
    //        Assert.assertTrue(solo!!.waitForView(javaClass<EditText>()))
    //        Assert.assertTrue(solo!!.getEditText(3).isEnabled())
    //        val today = Tools.createClearedCalendar()
    //        today.add(Calendar.DAY_OF_MONTH, 1)
    //        solo!!.enterText(3, Integer.toString(today.get(Calendar.DAY_OF_MONTH)))
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForText(solo!!.getString(R.string.no_operation)))
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1000.50.formatSum()))
    //        //        Log.d(TAG, "addOpMode1 before add " + solo.getCurrentViews(ListView.class).get(0).getCount());
    //        today.add(Calendar.DAY_OF_MONTH, -1)
    //        addOpOnDate(today, 0, javaClass<OperationListFragment>())
    //        tools!!.printCurrentTextViews()
    //        solo!!.pressSpinnerItem(0, -1)
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(999.50.formatSum()))
    //        solo!!.clickInList(0)
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(999.50.formatSum()))
    //        //        Log.d(TAG, "addOpMode1 after one add " + solo.getCurrentViews(ListView.class).get(0).getCount());
    //        // add op after X
    //        today.add(Calendar.MONTH, +1)
    //        addOpOnDate(today, 1, javaClass<OperationListFragment>())
    //        solo!!.clickInList(0)
    //        tools!!.sleep(1000)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(999.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(998.50.formatSum()))
    //        //        Log.d(TAG, "addOpMode1 after two add " + solo.getCurrentViews(ListView.class).get(0).getCount());
    //        // add op before X of next month, should update the current sum
    //        today.add(Calendar.MONTH, -2)
    //        addOpOnDate(today, 2, javaClass<OperationListFragment>())
    //        // Log.d(TAG, "addOpMode1 after three add " +
    //        // solo.getCurrentListViews().get(0).getCount());
    //        solo!!.clickInList(0)
    //        tools!!.printCurrentButtons()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(998.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(997.50.formatSum()))
    //    }
    //
    //    public fun editOpMode1() {
    //        addOpMode1()
    //
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<OperationEditor>()))
    //        solo!!.clearEditText(SUM_FIELD_IDX)
    //        solo!!.enterText(SUM_FIELD_IDX, "+2")
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        solo!!.clickInList(0)
    //        tools!!.printCurrentTextViews()
    //
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(998.50.formatSum()))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(1000.50.formatSum()))
    //        // Log.d(TAG, "editOpMode1 after one edit " + solo.getCurrentListViews().get(0).getCount());
    //
    //        solo!!.clickInList(3)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.edit_op))
    //        solo!!.clearEditText(SUM_FIELD_IDX)
    //        solo!!.enterText(SUM_FIELD_IDX, "+2")
    //        tools!!.clickOnActionItemCompat(R.id.confirm)
    //        Assert.assertTrue(solo!!.waitForActivity(javaClass<MainActivity>()))
    //        Assert.assertTrue(solo!!.waitForFragmentByTag(javaClass<OperationListFragment>().getName()))
    //        Assert.assertTrue(solo!!.waitForView(javaClass<ListView>()))
    //        // Log.d(TAG, "editOpMode1 after one edit " + solo.getCurrentListViews().get(0).getCount());
    //        solo!!.clickInList(0)
    //        assertEquals(3, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
    //        Log.d(TAG, "editOpMode1 CUR_ACC_SUM : " + solo!!.getText(CUR_ACC_SUM_IDX).getText().toString())
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(1001.50.formatSum()))
    //        Log.d(TAG, "editOpMode1 FIRST_SUM_AT_SEL_IDX : " + solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString())
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(1003.50.formatSum()))
    //
    //    }
    //
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
    //
    //    private fun delOps() {
    //        solo!!.clickInList(0)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.yes))
    //        Assert.assertTrue(solo!!.waitForDialogToClose(WAIT_DIALOG_TIME))
    //        val sum = 1001.50.formatSum()
    //        solo!!.clickInList(0)
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(sum))
    //        solo!!.clickInList(2)
    //        solo!!.clickOnImageButton(tools!!.findIndexOfImageButton(R.id.delete_op))
    //        solo!!.clickOnButton(solo!!.getString(R.string.yes))
    //        Assert.assertTrue(solo!!.waitForDialogToClose(WAIT_DIALOG_TIME))
    //        solo!!.clickInList(0)
    //        val sum2 = 999.50.formatSum()
    //        tools!!.printCurrentTextViews()
    //        Assert.assertTrue(solo!!.getText(CUR_ACC_SUM_IDX).getText().toString().contains(sum2))
    //        Assert.assertTrue(solo!!.getText(FIRST_SUM_AT_SEL_IDX).getText().toString().contains(sum2))
    //    }
    //
    //    public fun testDelOpMode1() {
    //        TAG = "testDelOpMode1"
    //        editOpMode1()
    //        delOps()
    //    }
    //
    //    public fun testDelOpMode2() {
    //        TAG = "testDelOpMode2"
    //        editOpMode2()
    //        delOps()
    //    }
    //
    //    // test transfert
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
    //    public fun testDelSimpleTransfert() {
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
    //    public fun testEditTransfertToNoTransfertAnymore() {
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
    //    public fun testEditSimpleTrans3accounts() {
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
    //    public fun testDelSchTransfert() {
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
    //    public fun testDelSchTransfFromOpsList() {
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
    //    public fun testDelAllOccSchTransfFromOpsList() {
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
    //    public fun testDelFutureSchTransfFromOpsList() {
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
    //    public fun testDelAllOccSchTransfFromSchList() {
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
    //    public fun testSumAtSelectionOnOthersAccount() {
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
    //    public fun testEmptyInfoCreation() {
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
    //    public fun testCorruptedCustomPeriodicity() {
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
