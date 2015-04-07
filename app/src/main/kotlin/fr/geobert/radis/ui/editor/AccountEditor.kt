package fr.geobert.radis.ui.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.ui.ConfigFragment
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Currency
import java.util.Locale
import kotlin.properties.Delegates

public class AccountEditor : BaseActivity(), EditorToolbarTrait {
    private val mViewPager by Delegates.lazy { findViewById(R.id.pager) as ViewPager }

    private val mPagerAdapter = object : FragmentPagerAdapter(getSupportFragmentManager()) {
        private val fragmentsList: Array<Fragment?> = arrayOfNulls(getCount())
        override fun getItem(position: Int): Fragment? {
            val f = fragmentsList.get(position)
            return if (null == f) {
                fragmentsList.set(position, when (position) {
                    0 -> AccountEditFragment()
                    else -> ConfigFragment()
                })
                fragmentsList.get(position)
            } else {
                f
            }
        }

        override fun getCount(): Int = 2

        override fun getPageTitle(position: Int): CharSequence =
                when (position) {
                    0 -> getString(R.string.account)
                    else -> getString(R.string.preferences)
                }
    }

    fun getAccountFrag() = mPagerAdapter.getItem(0) as AccountEditFragment
    fun getConfigFrag() = mPagerAdapter.getItem(1) as ConfigFragment

    fun getAccount(): Account = getAccountFrag().mAccount

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)

        setContentView(R.layout.multipane_editor)
        initToolbar(this)
        mViewPager.setAdapter(mPagerAdapter)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.confirm -> {
                onOkClicked()
                return true
            }
            else -> return super<BaseActivity>.onOptionsItemSelected(item)
        }
    }

    private fun onOkClicked() {
        val errMsg = StringBuilder()
        if (getAccountFrag().isFormValid(errMsg)) {
            setResult(Activity.RESULT_OK)
            getAccountFrag().saveState()
            finish()
            this.overridePendingTransition(R.anim.enter_from_right, 0)
        } else {
            Tools.popError(this, errMsg.toString(), null)
        }
    }


    companion object {
        public fun callMeForResult(context: ActionBarActivity, accountId: Long) {
            val intent = Intent(context, javaClass<AccountEditor>())
            intent.putExtra(AccountEditFragment.PARAM_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, AccountEditFragment.ACCOUNT_EDITOR)
        }
    }
}
