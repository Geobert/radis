package fr.geobert.radis.ui.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarActivity
import android.util.Log
import android.view.MenuItem
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.ui.ConfigFragment
import kotlin.properties.Delegates

public class AccountEditor : BaseActivity(), EditorToolbarTrait {
    public var mRowId: Long = 0
        private set

    private val mViewPager by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.pager) as ViewPager }

    private val mPagerAdapter = object : FragmentStatePagerAdapter(supportFragmentManager) {
        private val fragmentsList: Array<Fragment?> = arrayOfNulls(count)
        override fun getItem(position: Int): Fragment? {
            val f = fragmentsList.get(position)
            return if (null == f) {
                fragmentsList.set(position, when (position) {
                    0 -> AccountEditFragment()
                    else -> ConfigFragment() : Fragment
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
    fun isNewAccount() = NO_ACCOUNT == mRowId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multipane_editor)
        initToolbar(this)

        val extra = intent.extras
        mRowId = if (extra != null) {
            extra.getLong(PARAM_ACCOUNT_ID)
        } else {
            NO_ACCOUNT
        }

        if (isNewAccount()) {
            setTitle(R.string.account_creation)
        } else {
            setTitle(R.string.account_edit_title)
        }

        mViewPager.adapter = mPagerAdapter
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mRowId = savedInstanceState.getLong("mRowId")
        //        getAccountFrag().onRestoreInstanceState(savedInstanceState) // not managed by Android
        //        getConfigFrag().onRestoreInstanceState(savedInstanceState)  // not managed by Android
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putLong("mRowId", mRowId)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.confirm -> {
                onOkClicked()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onOkClicked() {
        val errMsg = StringBuilder()
        if (getAccountFrag().isFormValid(errMsg)) {
            setResult(Activity.RESULT_OK)
            val account = getAccountFrag().fillAccount()
            val config = getConfigFrag().fillConfig()
            if (isNewAccount()) {
                val id = AccountTable.createAccount(this, account)
                PreferenceTable.createAccountPrefs(this, config, id)
            } else {
                AccountTable.updateAccount(this, account)
                PreferenceTable.updateAccountPrefs(this, config, mRowId)
            }
            Log.d("AccountEditor", "onOkClicked")
            finish()
            this.overridePendingTransition(R.anim.enter_from_right, 0)
        } else {
            Tools.popError(this, errMsg.toString(), null)
        }
    }

    companion object {
        public fun callMeForResult(context: ActionBarActivity, accountId: Long, firstAccount:Boolean = false) {
            val intent = Intent(context, AccountEditor::class.java)
            intent.putExtra(PARAM_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, if (firstAccount) ACCOUNT_CREATOR else ACCOUNT_EDITOR)
        }

        public val NO_ACCOUNT: Long = 0
        public val ACCOUNT_EDITOR: Int = 1000
        public val ACCOUNT_CREATOR: Int = 1100
        public val PARAM_ACCOUNT_ID: String = "account_id"
        public val GET_ACCOUNT: Int = 400
        public val GET_ACCOUNT_CONFIG: Int = 410
    }
}
