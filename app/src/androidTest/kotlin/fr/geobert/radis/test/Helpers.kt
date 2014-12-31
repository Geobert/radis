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

        fun clickOnActionItemConfirm() = onView(withId(R.id.confirm)).perform(click())

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
            onView(withId(R.id.drawer_layout)).check(matches(isOpen()))
            onData(allOf(iz(instanceOf(javaClass<NavDrawerItem>())), withNavDrawerItem(str))).perform(click())
        }

        fun callAccountCreation() {
            clickInDrawer(R.string.create_account)
        }

        fun callAccountEdit() {
            clickInDrawer(R.string.account_edit)
        }

        fun addAccount() {
            checkTitleBarDisplayed(R.string.account_creation)
            onView(withId(R.id.edit_account_name)).perform(typeText(RadisTest.ACCOUNT_NAME))
            onView(withId(R.id.edit_account_start_sum)).perform(typeText(RadisTest.ACCOUNT_START_SUM))
            onView(withId(R.id.edit_account_desc)).perform(scrollTo())
            onView(withId(R.id.edit_account_desc)).perform(typeText(RadisTest.ACCOUNT_DESC))
            clickOnActionItemConfirm()
            onView(withText(equalTo(activity.getString(R.string.no_operation)))).check(matches(isDisplayed()))
            onView(withId(android.R.id.text1)).check(matches(withText(equalTo(RadisTest.ACCOUNT_NAME))))
            onView(withId(R.id.account_sum)).check(matches(withText(equalTo(RadisTest.ACCOUNT_START_SUM_FORMATED_ON_LIST))))
            // TODO test if only one entry once converted the actionbar to Toolbar
        }

        fun addManyOps() {
            addAccount()
            for (j in 0..9) {
                onView(withId(R.id.create_operation)).perform(click())
                checkTitleBarDisplayed(R.string.op_creation)
                onView(withId(R.id.edit_op_third_party)).perform(typeText(RadisTest.OP_TP + j))
                onView(withId(R.id.edit_op_sum)).perform(scrollTo())
                onView(withId(R.id.edit_op_sum)).perform(typeText(RadisTest.OP_AMOUNT_2))
                onView(withId(R.id.edit_op_tag)).perform(scrollTo())
                onView(withId(R.id.edit_op_tag)).perform(typeText(RadisTest.OP_TAG))
                onView(withId(R.id.edit_op_mode)).perform(scrollTo())
                onView(withId(R.id.edit_op_mode)).perform(typeText(RadisTest.OP_MODE))
                onView(withId(R.id.edit_op_notes)).perform(scrollTo())
                onView(withId(R.id.edit_op_notes)).perform(typeText(RadisTest.OP_DESC))
                clickOnActionItemConfirm()
            }
            onView(withText(R.string.no_operation)).check(matches(not(isDisplayed())))
            onView(withId(R.id.account_sum)).check(matches(withText(containsString(0.5.formatSum()))))
        }

        fun setUpSchOp() {
            addAccount()
            clickInDrawer(R.string.preferences)
            onView(withText(R.string.prefs_insertion_date_label)).perform(click())
            onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(clearText())

            val today = Tools.createClearedCalendar()
            today.add(Calendar.DAY_OF_MONTH, 1)
            onView(allOf(iz(instanceOf(javaClass<EditText>())), hasFocus()) as Matcher<View>).perform(typeText(Integer.toString(today.get(Calendar.DAY_OF_MONTH))))
            Espresso.closeSoftKeyboard()
            pauseTest(1200) // needed to workaround espresso 2.0 bug
            onView(allOf(iz(instanceOf(javaClass<Button>())), withText(R.string.ok), isDisplayed()) as Matcher<View>).perform(click())

            Espresso.pressBack()

            clickInDrawer(R.string.scheduled_ops)
            onView(withText(R.string.no_operation_sch)).check(matches(isDisplayed()))
        }

        fun addScheduleOp() {
            setUpSchOp()
            onView(withId(R.id.create_operation)).perform(click())
            checkTitleBarDisplayed(R.string.sch_edition)

            val today = Tools.createClearedCalendar()
            onView(withId(R.id.edit_op_date)).perform(setDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH)))

            onView(withId(R.id.edit_op_third_party)).perform(scrollTo())
            onView(withId(R.id.edit_op_third_party)).perform(typeText(RadisTest.OP_TP))
            onView(withId(R.id.edit_op_sum)).perform(scrollTo())
            onView(withId(R.id.edit_op_sum)).perform(typeText("9,50"))
            onView(withId(R.id.edit_op_tag)).perform(scrollTo())
            onView(withId(R.id.edit_op_tag)).perform(typeText(RadisTest.OP_TAG))
            onView(withId(R.id.edit_op_mode)).perform(scrollTo())
            onView(withId(R.id.edit_op_mode)).perform(typeText(RadisTest.OP_MODE))
            onView(withId(R.id.edit_op_notes)).perform(scrollTo())
            onView(withId(R.id.edit_op_notes)).perform(typeText(RadisTest.OP_DESC))

            onView(withText(R.string.scheduling)).perform(click())

            onView(withId(R.id.periodicity_choice)).perform(scrollTo())
            onView(withId(R.id.periodicity_choice)).perform(click())
            val strs = activity.getResources().getStringArray(R.array.periodicity_choices).get(1)
            onData(allOf(iz(instanceOf(javaClass<String>())), iz(equalTo(strs)))).perform(click())
            clickOnActionItemConfirm()


            onView(withText(R.string.no_operation_sch)).check(matches(not(isDisplayed())))
            // TODO assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())

            Espresso.pressBack()

            // TODO assertEquals(1, solo!!.getCurrentViews(javaClass<ListView>()).get(0).getCount())
            onView(withText(R.string.no_operation)).check(doesNotExist())

            onView(withId(R.id.account_sum)).check(matches(withText(containsString(991.0.formatSum()))))
        }

        fun pauseTest(t: Long) {
            try {
                Thread.sleep(t)
            } catch (e: Exception) {

            }
        }
    }
}