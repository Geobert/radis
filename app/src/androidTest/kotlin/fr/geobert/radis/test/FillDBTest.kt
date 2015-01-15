package fr.geobert.radis.test

import android.content.Intent
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.db.DbContentProvider
import android.test.ActivityInstrumentationTestCase2
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.action.ViewActions.typeText
import fr.geobert.radis.tools.Tools
import java.util.Calendar
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.contrib.PickerActions.setDate
import android.support.test.espresso.Espresso

public class FillDBTest : ActivityInstrumentationTestCase2<MainActivity>(javaClass<MainActivity>()) {

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

        Helpers.instrumentationTest = this
        Helpers.activity = getActivity()
    }

    public fun testZFillForScreenshots() {
        Helpers.checkTitleBarDisplayed(R.string.account_creation)
        onView(withId(R.id.edit_account_name)).perform(typeText("Courant"))
        onView(withId(R.id.edit_account_start_sum)).perform(typeText("2000"))
        Helpers.clickOnActionItemConfirm()

        Helpers.clickInDrawer(R.string.create_account)
        Helpers.checkTitleBarDisplayed(R.string.account_creation)
        onView(withId(R.id.edit_account_name)).perform(typeText("Epargne"))
        onView(withId(R.id.edit_account_start_sum)).perform(typeText("1400"))
        Helpers.clickOnActionItemConfirm()

        Helpers.clickInDrawer(R.string.create_account)
        Helpers.checkTitleBarDisplayed(R.string.account_creation)
        onView(withId(R.id.edit_account_name)).perform(typeText("Livret A"))
        onView(withId(R.id.edit_account_start_sum)).perform(typeText("1000"))
        Helpers.clickOnActionItemConfirm()

        Helpers.clickInDrawer(R.string.create_account)
        Helpers.checkTitleBarDisplayed(R.string.account_creation)
        onView(withId(R.id.edit_account_name)).perform(typeText("Autre compte"))
        onView(withId(R.id.edit_account_start_sum)).perform(typeText("1210"))
        Helpers.clickOnActionItemConfirm()

        Helpers.clickInDrawer(R.string.scheduled_ops)
        onView(withId(R.id.create_operation)).perform(click())
        Helpers.checkTitleBarDisplayed(R.string.sch_edition)
        var date = Tools.createClearedCalendar()
        date.set(Calendar.DAY_OF_MONTH, 29)
        date.add(Calendar.MONTH, -2)
        onView(withId(R.id.edit_op_date)).perform(setDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH)))
        Helpers.fillOpForm("Salaire", "+2000.00", "", "Virement", "")
        Helpers.clickOnActionItemConfirm()

        onView(withId(R.id.create_operation)).perform(click())
        date.set(Calendar.DAY_OF_MONTH, 5)
        onView(withId(R.id.edit_op_date)).perform(setDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH)))
        Helpers.fillOpForm("Internet", "35.00", "Maison", "Prelevement", "")
        Helpers.clickOnActionItemConfirm()

        Espresso.pressBack()

        date = Tools.createClearedCalendar()
        date.set(Calendar.DAY_OF_MONTH, 22)
        Helpers.addOp(date, "Boucherie", "45.00", "Alimentaire", "CB", "")
        date.set(Calendar.DAY_OF_MONTH, 26)
        date.add(Calendar.MONTH, -1)
        Helpers.addOp(date, "Mutuelle", "+35.00", "Sante", "Virement", "")
        date.set(Calendar.DAY_OF_MONTH, 22)
        Helpers.addOp(date, "Boulangerie", "8.00", "Alimentaire", "Espece", "")
        date.set(Calendar.DAY_OF_MONTH, 19)
        Helpers.addOp(date, "Disquaire", "17.00", "Loisir", "CB", "")
        date.set(Calendar.DAY_OF_MONTH, 28)
        Helpers.addOp(date, "Restaurant", "43.32", "Sortie", "CB", "")
    }
}