package fr.geobert.radis.test

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.*
import android.support.test.espresso.Espresso.*
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.*
import android.support.test.espresso.contrib.*
import android.support.test.espresso.contrib.DrawerMatchers.isOpen
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.util.Log
import android.view.View
import android.widget.*
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.adapter.OpRowHolder
import hirondelle.date4j.DateTime
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*

class Helpers {

    companion object {
        fun getContext() = InstrumentationRegistry.getTargetContext()

        fun clickOnActionItemConfirm() {
            Espresso.closeSoftKeyboard()
            pauseTest(1000)
            onView(withId(R.id.confirm)).perform(click())
            pauseTest(1200)
            onView(withId(R.id.confirm)).check(doesNotExist())
            pauseTest(1000)
        }

        fun ACTIONBAR_TITLE_MATCHER(title: String): Matcher<View> {
            //            val actionBarId = instrumentationTest.getInstrumentation().getTargetContext().getResources().getIdentifier("action_bar_container", "id", "android");
            // isDescendantOfA(withId(actionBarId)),
            return allOf(withText(title))
        }

        fun checkTitleBarDisplayed(title: Int) =
                onView(ACTIONBAR_TITLE_MATCHER(getContext().getString(title))).check(matches(isDisplayed()))


        fun clickInDrawer(text: Int) {
            val str = getContext().getString(text)
            Log.d("clickInDrawer", "wanted text : " + str)
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            pauseTest(100)
            onView(withId(R.id.drawer_layout)).check(matches(isOpen()))
            onData(withNavDrawerItem(str)).inAdapterView(withId(R.id.left_drawer)).perform(click())
        }

        fun callAccountCreation() {
            clickInDrawer(R.string.create_account)
        }

        fun callAccountEdit() {
            clickInDrawer(R.string.account_edit)
            checkTitleBarDisplayed(R.string.account_edit_title)
            Helpers.pauseTest(1000)
        }

        fun addAccount() {
            Helpers.pauseTest(1000)
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM))
            scrollThenTypeText(R.id.edit_account_desc, RadisTest.ACCOUNT_DESC)
            clickOnActionItemConfirm()
            Helpers.pauseTest(500)
            onView(withText(equalTo(getContext().getString(R.string.no_operation)))).check(matches(isDisplayed()))
            onView(withId(android.R.id.text1)).check(matches(withText(equalTo(RadisTest.ACCOUNT_NAME))))
            onView(withId(R.id.account_sum)).check(matches(withText(equalTo(RadisTest.ACCOUNT_START_SUM_FORMATED_ON_LIST))))
            // TODO test if only one entry once converted the actionbar to Toolbar
        }

        fun addAccount2() {
            callAccountCreation()
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME_2))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM_2))
            scrollThenTypeText(R.id.edit_account_desc, RadisTest.ACCOUNT_DESC_2)
            clickOnActionItemConfirm()

            onView(allOf(withId(R.id.account_spinner), isDisplayed())).perform(click())
            onView(allOf(iz(instanceOf(ListView::class.java)), isDisplayed()) as Matcher<View>).inRoot(RootMatchers.isPlatformPopup()).check(has(2, RelativeLayout::class.java))
            onView(withText(RadisTest.ACCOUNT_NAME)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        }

        fun addAccount3() {
            callAccountCreation()
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME_3))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM))
            scrollThenTypeText(R.id.edit_account_desc, RadisTest.ACCOUNT_DESC)
            clickOnActionItemConfirm()

            onView(allOf(withId(R.id.account_spinner), isDisplayed())).perform(click())
            onView(allOf(iz(instanceOf(ListView::class.java)), isDisplayed()) as Matcher<View>).inRoot(RootMatchers.isPlatformPopup()).check(has(3, RelativeLayout::class.java))
            onView(withText(RadisTest.ACCOUNT_NAME)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        }

        fun addOp(date: DateTime, third: String = "Toto", amount: String = "-1", tag: String = "", mode: String = "", desc: String = "") {
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.op_creation)
            Helpers.setDateOnPicker(R.id.op_date_btn, date)
            fillOpForm(third, amount, tag, mode, desc)
            clickOnActionItemConfirm()
        }

        fun addOp(third: String, amount: String, tag: String, mode: String, desc: String) {
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.op_creation)
            fillOpForm(third, amount, tag, mode, desc)
            clickOnActionItemConfirm()
        }

        fun addManyOps() {
            addAccount()
            for (j in 0..9) {
                addOp(RadisTest.OP_TP, RadisTest.OP_AMOUNT_2, RadisTest.OP_TAG, RadisTest.OP_MODE, RadisTest.OP_DESC)
                Helpers.pauseTest(600)
            }
            onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
            checkAccountSumIs(0.5.formatSum())
        }

        fun addOp() {
            addOp(RadisTest.OP_TP, RadisTest.OP_AMOUNT, RadisTest.OP_TAG, RadisTest.OP_MODE, RadisTest.OP_DESC)
        }

        fun setEdtPrefValue(key: Int, value: String) {
            onView(withText(key)).perform(click())
            onView(allOf(iz(instanceOf(EditText::class.java)), hasFocus()) as Matcher<View>).perform(replaceText(value))
            Espresso.closeSoftKeyboard()
            pauseTest(2000) // needed to workaround espresso 2.0 bug
            clickOnDialogButton(R.string.ok)
        }

        fun setInsertDatePref(date: DateTime) {
            setEdtPrefValue(R.string.prefs_insertion_date_label, Integer.toString(date.day))
        }

        fun setUpSchOp() {
            addAccount()

            clickInDrawer(R.string.preferences)
            setInsertDatePref(DateTime.today(TIME_ZONE).plusDays(1))

            Espresso.pressBack()

            clickInDrawer(R.string.scheduled_ops)
            onView(withText(R.string.no_operation_sch)).check(matches(isDisplayed()))
        }

        // add a sch op today, 9.50, monthly
        fun addScheduleOp(date: DateTime) {
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.sch_edition)

            Helpers.setDateOnPicker(R.id.op_date_btn, date)

            fillOpForm(RadisTest.OP_TP, "9.50", RadisTest.OP_TAG, RadisTest.OP_MODE, RadisTest.OP_DESC)

            clickOnSpinner(R.id.periodicity_choice, R.array.periodicity_choices, 1)
            clickOnActionItemConfirm()
            onView(withText(R.string.no_operation_sch)).check(matches(not(isDisplayed())))
            // TODO assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())

            Espresso.pressBack()

            // TODO assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
        }


        fun setupDelOccFromOps(): Int {
            setUpSchOp()
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.sch_edition)
            val today = DateTime.today(TIME_ZONE)
            val schOpDate = today.minusDays(14)

            Helpers.setDateOnPicker(R.id.op_date_btn, schOpDate)

            fillOpForm(RadisTest.OP_TP, "1.00", RadisTest.OP_TAG, RadisTest.OP_MODE, RadisTest.OP_DESC)

            onView(withId(R.id.periodicity_choice)).perform(scrollTo())
            onView(withId(R.id.periodicity_choice)).perform(click())
            val strs = getContext().resources.getStringArray(R.array.periodicity_choices).get(0)
            onData(allOf(iz(instanceOf(String::class.java)), iz(equalTo(strs)))).perform(click())
            clickOnActionItemConfirm()

            Espresso.pressBack() // back to operations list
            Helpers.pauseTest(2000)
            val endOfMonth = today.endOfMonth
            val nbOp = endOfMonth.getWeekIndex(schOpDate)

            Log.d("RadisTest", "nb op inserted: $nbOp")

            checkAccountSumIs((1000.5 - nbOp).formatSum())
            return nbOp
        }

        fun swipePagerLeft() {
            onView(withId(R.id.pager)).perform(ViewActions.swipeLeft())
            Espresso.closeSoftKeyboard()
        }

        fun swipePagerRight() {
            onView(withId(R.id.pager)).perform(ViewActions.swipeRight())
            Espresso.closeSoftKeyboard()
        }

        fun pauseTest(t: Long) {
            try {
                Thread.sleep(t)
            } catch (e: Exception) {
            }
        }

        fun actionOnOpListAtPosition(pos: Int, viewAction: ViewAction) =
                actionOnItemAtPosition<OpRowHolder<Operation>>(pos, viewAction)

        fun clickOnDialogButton(textId: Int) = onView(allOf(iz(instanceOf(Button::class.java)), withText(textId),
                isDisplayed()) as Matcher<View>).perform(click())

        fun clickOnDialogButton(text: String) = onView(allOf(iz(instanceOf(Button::class.java)), withText(text),
                isDisplayed()) as Matcher<View>).perform(click())

        fun scrollRecyclerViewToPos(pos: Int) {
            onView(withId(R.id.operation_list)).perform(RecyclerViewActions.scrollToPosition<OpRowHolder<Operation>>(pos))
        }

        fun clickOnRecyclerViewAtPos(pos: Int) {
            onView(withId(R.id.operation_list)).perform(Helpers.actionOnOpListAtPosition(pos, click()))
            Helpers.pauseTest(500)
        }

        fun checkAccountSumIs(text: String) =
                onView(withId(R.id.account_sum)).check(matches(withText(containsString(text))))

        fun scrollThenTypeText(edtId: Int, str: String) {
            val v = onView(withId(edtId)).perform(scrollTo())
            Helpers.pauseTest(500)
            v.perform(replaceText("")).perform(typeText(str))
        }

        fun clickOnSpinner(spinnerId: Int, arrayResId: Int, pos: Int) {
            onView(withId(spinnerId)).perform(scrollTo())
            Helpers.pauseTest(500)
            onView(withId(spinnerId)).perform(click())
            Helpers.pauseTest(500)
            val strs = getContext().resources.getStringArray(arrayResId).get(pos)
            onData(allOf(iz(instanceOf(String::class.java)), iz(equalTo(strs)))).perform(click())
        }

        fun clickOnSpinner(spinnerId: Int, text: String) {
            onView(withId(spinnerId)).perform(scrollTo())
            Helpers.pauseTest(500)
            onView(withId(spinnerId)).perform(click())
            Helpers.pauseTest(500)
            onView(withText(equalTo(text))).perform(click())
        }

        fun clickOnAccountSpinner(accName: String) {
            onView(allOf(withId(R.id.account_spinner), isDisplayed())).perform(click())
            pauseTest(500)
            onView(withText(accName)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        }

        fun checkSelectedSumIs(text: String) =
                onView(allOf(withId(R.id.today_amount), isDisplayed(),
                        withText(not(equalTo(""))))).check(matches(withText(containsString(text))))

        fun fillOpForm(third: String, amount: String, tag: String, mode: String, desc: String) {
            scrollThenTypeText(R.id.edit_op_third_party, third)
            scrollThenTypeText(R.id.edit_op_sum, amount)
            scrollThenTypeText(R.id.edit_op_tag, tag)
            scrollThenTypeText(R.id.edit_op_mode, mode)
            scrollThenTypeText(R.id.edit_op_notes, desc)
        }

        fun goToCurAccountOptionPanel() {
            Helpers.clickInDrawer(R.string.account_edit)
            Helpers.checkTitleBarDisplayed(R.string.account_edit_title)
            Espresso.closeSoftKeyboard()
            Helpers.swipePagerLeft()
        }

        fun getTextFrom(id: Int): CharSequence? {
            var res: CharSequence? = null

            onView(allOf(withId(id), isDisplayed())).perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return allOf(isDisplayed(), iz(instanceOf(TextView::class.java)))
                }

                override fun getDescription(): String {
                    return "get date from text view"
                }

                override fun perform(p0: UiController?, p1: View?) {
                    val textView = p1 as TextView
                    res = textView.text
                }

            })

            return res
        }

        fun setDateOnPicker(id: Int, date: DateTime) {
            Log.d("Helpers", "setDateOnPicker ${date.format("dd/MM/yyyy")}")
            onView(withId(id)).perform(click())
            //iz(instanceOf(DatePicker::class.java))
            //withClassName(equalTo(DatePicker::class.java.name))
            onView(iz(instanceOf(DatePicker::class.java))).perform(PickerActions.setDate(date.year,
                    date.month, date.day))
            //onView(allOf(withId(android.R.id.button1), iz(instanceOf(AppCompatButton::class.java)))).perform(click());

            Helpers.clickOnDialogButton(android.R.string.ok)
            pauseTest(500)
        }
    }
}
