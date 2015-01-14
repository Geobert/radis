package fr.geobert.radis.test

import fr.geobert.radis.R

import android.support.test.espresso.Espresso.*
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.assertion.ViewAssertions.*
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.contrib.DrawerActions.*
import android.support.test.espresso.contrib.DrawerMatchers.*
import android.support.test.espresso.contrib.PickerActions.*
import org.hamcrest.Matchers.*
import android.util.Log
import fr.geobert.radis.ui.drawer.NavDrawerItem
import android.app.Activity
import kotlin.properties.Delegates
import android.view.KeyEvent
import fr.geobert.radis.tools.formatSum
import org.hamcrest.Matcher
import android.view.View
import fr.geobert.radis.tools.Tools
import java.util.Calendar
import android.support.test.espresso.Espresso
import android.widget.EditText
import android.widget.Button
import android.support.test.espresso.ViewAction
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import fr.geobert.radis.ui.adapter.OpRowHolder
import fr.geobert.radis.data.Operation
import android.support.test.espresso.matcher.RootMatchers
import android.widget.ListView
import android.widget.RelativeLayout

class Helpers {

    class object {
        var radisTest: RadisTest by Delegates.notNull()
        var activity: Activity by Delegates.notNull()

        fun backOutToHome() {
            var more = true
            while (more) {
                try {
                    radisTest.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                } catch (e: SecurityException) {
                    // Done, at Home.
                    more = false
                }

            }
        }

        fun clickOnActionItemConfirm() {
            onView(withId(R.id.confirm)).perform(click())
            pauseTest(700)
        }

        fun ACTIONBAR_TITLE_MATCHER(title: String): Matcher<View> {
            val actionBarId = radisTest.getInstrumentation().getTargetContext().getResources().getIdentifier("action_bar_container", "id", "android");
            // isDescendantOfA(withId(actionBarId)),
            return allOf(withText(title))
        }

        fun checkTitleBarDisplayed(title: Int) =
                onView(ACTIONBAR_TITLE_MATCHER(activity.getString(title))).check(matches(isDisplayed()))


        fun clickInDrawer(text: Int) {
            val str = activity.getString(text)
            Log.d("clickInDrawer", "wanted text : " + str)
            openDrawer(R.id.drawer_layout)
            pauseTest(100)
            onView(withId(R.id.drawer_layout)).check(matches(isOpen()))
            onData(allOf(iz(instanceOf(javaClass<NavDrawerItem>())), withNavDrawerItem(str))).perform(click())
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
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM))
            scrollThenTypeText(R.id.edit_account_desc, RadisTest.ACCOUNT_DESC)
            clickOnActionItemConfirm()
            Helpers.pauseTest(1000)
            onView(withText(equalTo(activity.getString(R.string.no_operation)))).check(matches(isDisplayed()))
            onView(withId(android.R.id.text1)).check(matches(withText(equalTo(RadisTest.ACCOUNT_NAME))))
            onView(withId(R.id.account_sum)).check(matches(withText(equalTo(RadisTest.ACCOUNT_START_SUM_FORMATED_ON_LIST))))
            // TODO test if only one entry once converted the actionbar to Toolbar
        }

        public fun addAccount2() {
            callAccountCreation()
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME_2))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM_2))
            scrollThenTypeText(R.id.edit_account_desc, RadisTest.ACCOUNT_DESC_2)
            clickOnActionItemConfirm()

            onView(allOf(isActionBarSpinner(), isDisplayed())).perform(click())
            onView(allOf(iz(instanceOf(javaClass<ListView>())), isDisplayed()) as Matcher<View>).inRoot(RootMatchers.isPlatformPopup()).check(has(2, javaClass<RelativeLayout>()))
            onView(withText(RadisTest.ACCOUNT_NAME)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        }

        public fun addAccount3() {
            callAccountCreation()
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME_3))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM))
            scrollThenTypeText(R.id.edit_account_desc, RadisTest.ACCOUNT_DESC)
            clickOnActionItemConfirm()

            onView(allOf(isActionBarSpinner(), isDisplayed())).perform(click())
            onView(allOf(iz(instanceOf(javaClass<ListView>())), isDisplayed()) as Matcher<View>).inRoot(RootMatchers.isPlatformPopup()).check(has(3, javaClass<RelativeLayout>()))
            onView(withText(RadisTest.ACCOUNT_NAME)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        }

        fun addManyOps() {
            addAccount()
            for (j in 0..9) {
                onView(withId(R.id.create_operation)).perform(click())
                checkTitleBarDisplayed(R.string.op_creation)
                scrollThenTypeText(R.id.edit_op_third_party, RadisTest.OP_TP + j)
                scrollThenTypeText(R.id.edit_op_sum, RadisTest.OP_AMOUNT_2)
                scrollThenTypeText(R.id.edit_op_tag, RadisTest.OP_TAG)
                scrollThenTypeText(R.id.edit_op_mode, RadisTest.OP_MODE)
                scrollThenTypeText(R.id.edit_op_notes, RadisTest.OP_DESC)
                clickOnActionItemConfirm()
                Helpers.pauseTest(600)
            }
            onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
            checkAccountSumIs(0.5.formatSum())
        }

        fun addOp() {
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.op_creation)
            scrollThenTypeText(R.id.edit_op_third_party, RadisTest.OP_TP)
            scrollThenTypeText(R.id.edit_op_sum, RadisTest.OP_AMOUNT)
            scrollThenTypeText(R.id.edit_op_tag, RadisTest.OP_TAG)
            scrollThenTypeText(R.id.edit_op_mode, RadisTest.OP_MODE)
            scrollThenTypeText(R.id.edit_op_notes, RadisTest.OP_DESC)
            clickOnActionItemConfirm()
        }

        fun setUpSchOp() {
            addAccount()
            clickInDrawer(R.string.preferences)
            onView(withText(R.string.prefs_insertion_date_label)).perform(click())
            val today = Tools.createClearedCalendar()
            today.add(Calendar.DAY_OF_MONTH, 1)
            onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(replaceText(Integer.toString(today.get(Calendar.DAY_OF_MONTH))))
            Espresso.closeSoftKeyboard()
            pauseTest(2000) // needed to workaround espresso 2.0 bug
            clickOnDialogButton(R.string.ok)

            Espresso.pressBack()

            clickInDrawer(R.string.scheduled_ops)
            onView(withText(R.string.no_operation_sch)).check(matches(isDisplayed()))
        }

        fun addScheduleOp() {
            setUpSchOp()
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.sch_edition)

            val today = Tools.createClearedCalendar()
            // +1 is because MONTH is 0 indexed
            onView(withId(R.id.edit_op_date)).perform(setDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)))

            scrollThenTypeText(R.id.edit_op_third_party, RadisTest.OP_TP)
            scrollThenTypeText(R.id.edit_op_sum, "9,50")
            scrollThenTypeText(R.id.edit_op_tag, RadisTest.OP_TAG)
            scrollThenTypeText(R.id.edit_op_mode, RadisTest.OP_MODE)
            scrollThenTypeText(R.id.edit_op_notes, RadisTest.OP_DESC)

            onView(withText(R.string.scheduling)).perform(click())

            clickOnSpinner(R.id.periodicity_choice, R.array.periodicity_choices, 1)
            clickOnActionItemConfirm()

            onView(withText(R.string.no_operation_sch)).check(matches(not(isDisplayed())))
            // TODO assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())

            Espresso.pressBack()

            // TODO assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
            //            onView(withText(R.string.no_operation)).check(doesNotExist())
            onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
            checkAccountSumIs(991.0.formatSum())
        }


        fun setupDelOccFromOps(): Int {
            setUpSchOp()
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.sch_edition)
            val today = Tools.createClearedCalendar()
            today.add(Calendar.DAY_OF_MONTH, -14)
            onView(withId(R.id.edit_op_date)).perform(setDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1,
                    today.get(Calendar.DAY_OF_MONTH)))

            scrollThenTypeText(R.id.edit_op_third_party, RadisTest.OP_TP)
            scrollThenTypeText(R.id.edit_op_sum, "1,00")
            scrollThenTypeText(R.id.edit_op_tag, RadisTest.OP_TAG)
            scrollThenTypeText(R.id.edit_op_mode, RadisTest.OP_MODE)
            scrollThenTypeText(R.id.edit_op_notes, RadisTest.OP_DESC)

            onView(withText(R.string.scheduling)).perform(click())

            onView(withId(R.id.periodicity_choice)).perform(scrollTo())
            onView(withId(R.id.periodicity_choice)).perform(click())
            val strs = activity.getResources().getStringArray(R.array.periodicity_choices).get(0)
            onData(allOf(iz(instanceOf(javaClass<String>())), iz(equalTo(strs)))).perform(click())

            clickOnActionItemConfirm()
            Espresso.pressBack()
            Helpers.pauseTest(1000)
            checkAccountSumIs((1000.5 - 3).formatSum())
            return 3
        }

        fun pauseTest(t: Long) {
            try {
                Thread.sleep(t)

            } catch (e: Exception) {

            }
        }

        fun actionOnOpListAtPosition(pos: Int, viewAction: ViewAction) =
                actionOnItemAtPosition<OpRowHolder<Operation>>(pos, viewAction)

        fun clickOnDialogButton(textId: Int) = onView(allOf(iz(instanceOf(javaClass<Button>())), withText(textId),
                isDisplayed()) as Matcher<View>).perform(click())

        fun clickOnRecyclerViewAtPos(pos: Int) =
                onView(withId(android.R.id.list)).perform(Helpers.actionOnOpListAtPosition(pos, click()))

        fun checkAccountSumIs(text: String) =
                onView(withId(R.id.account_sum)).check(matches(withText(containsString(text))))

        fun scrollThenTypeText(edtId: Int, str: String) = onView(withId(edtId)).perform(scrollTo()).perform(typeText(str))

        fun clickOnSpinner(spinnerId: Int, arrayResId: Int, pos: Int) {
            onView(withId(spinnerId)).perform(scrollTo())
            onView(withId(spinnerId)).perform(click())
            val strs = activity.getResources().getStringArray(arrayResId).get(pos)
            onData(allOf(iz(instanceOf(javaClass<String>())), iz(equalTo(strs)))).perform(click())
        }

        fun clickOnSpinner(spinnerId: Int, text: String) {
            onView(withId(spinnerId)).perform(scrollTo())
            onView(withId(spinnerId)).perform(click())
            onView(withText(equalTo(text))).perform(click())
        }

        fun clickOnAccountSpinner(accName: String) {
            onView(allOf(isActionBarSpinner(), isDisplayed())).perform(click())
            pauseTest(500)
            onView(withText(accName)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        }

        fun checkSelectedSumIs(text: String) =
                onView(allOf(withId(R.id.today_amount), isDisplayed(),
                        withText(not(equalTo(""))))).check(matches(withText(containsString(text))))
    }
}