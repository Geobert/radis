package fr.geobert.radis

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.Cursor
import android.os.Bundle
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.crashlytics.android.Crashlytics
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.service.InstallRadisServiceReceiver
import fr.geobert.radis.service.OnRefreshReceiver
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.ConfigEditor
import fr.geobert.radis.ui.OperationListFragment
import fr.geobert.radis.ui.ScheduledOpListFragment
import fr.geobert.radis.ui.StatisticsListFragment
import fr.geobert.radis.ui.drawer.NavDrawerItem
import fr.geobert.radis.ui.drawer.NavDrawerListAdapter
import fr.geobert.radis.ui.editor.AccountEditFragment
import fr.geobert.radis.ui.editor.AccountEditor
import fr.geobert.radis.ui.editor.OperationEditor
import fr.geobert.radis.ui.editor.ScheduledOperationEditor
import hirondelle.date4j.DateTime
import io.fabric.sdk.android.Fabric
import java.util.ArrayList
import java.util.Currency
import java.util.Date
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class MainActivity : BaseActivity(), UpdateDisplayInterface {
    //    private static final int RESUMING = 1;

    private val mDrawerLayout by Delegates.lazy { findViewById(R.id.drawer_layout) as DrawerLayout }
    private val mDrawerList by Delegates.lazy { findViewById(R.id.left_drawer) as ListView }
    private val mAccountAdapter: SimpleCursorAdapter by Delegates.lazy {
        val from = array(AccountTable.KEY_ACCOUNT_NAME, AccountTable.KEY_ACCOUNT_CUR_SUM,
                AccountTable.KEY_ACCOUNT_CUR_SUM_DATE, AccountTable.KEY_ACCOUNT_CURRENCY)
        val to = intArray(android.R.id.text1, R.id.account_sum, R.id.account_balance_at)
        SimpleCursorAdapter(this, R.layout.account_row, null, from, to, 0)
    }
    private val mOnRefreshReceiver by Delegates.lazy { OnRefreshReceiver(this) }
    private val redColor: Int by Delegates.lazy { getResources().getColor(R.color.op_alert) }
    private val greenColor: Int by Delegates.lazy { getResources().getColor(R.color.positiveSum) }
    private val handler: FragmentHandler by Delegates.lazy { FragmentHandler(this) }
    public val mAccountSpinner: Spinner by Delegates.lazy { findViewById(R.id.account_spinner) as Spinner }

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the navigation drawer and the action bar app icon.
    private val mDrawerToggle: ActionBarDrawerToggle by Delegates.lazy {
        object : ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar,
                R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */) {

            override fun onDrawerClosed(drawerView: View?) {
                invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View?) {
                invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

        }
    }

    private var mActiveFragment: BaseFragment? = null
    private var mFirstStart = true
    private var mPrevFragment: BaseFragment? = null
    private var mActiveFragmentId = -1
    private var mPrevFragmentId: Int = 0


    protected fun findOrCreateFragment(c: Class<out BaseFragment>, fragmentId: Int): Fragment? {
        var fragment: Fragment?
        val fragmentManager = getSupportFragmentManager()
        fragment = fragmentManager.findFragmentByTag(c.getName())
        when (fragment) {
            null -> {
                try {
                    return updateFragmentRefs(c.newInstance(), fragmentId)
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                return null
            }
            else -> {
                updateFragmentRefs(fragment!!, fragmentId)
                mDrawerLayout.closeDrawer(mDrawerList)
                fragmentManager.popBackStack(c.getName(), 0)
                return null
            }
        }
    }

    protected fun updateFragmentRefs(fragment: Fragment, id: Int): Fragment {
        val f = fragment as BaseFragment
        mPrevFragment = mActiveFragment
        mActiveFragment = f
        mPrevFragmentId = mActiveFragmentId
        mActiveFragmentId = id
        mDrawerList.setItemChecked(mActiveFragmentId, true)
        return fragment
    }

    private inner class FragmentHandler(private var activity: MainActivity) : PauseHandler() {

        override fun processMessage(act: Activity, message: Message) {
            var fragment: Fragment? = null
            val fragmentManager = activity.getSupportFragmentManager()
            when (message.what) {
                OP_LIST -> fragment = findOrCreateFragment(javaClass<OperationListFragment>(), message.what)
                SCH_OP_LIST -> fragment = findOrCreateFragment(javaClass<ScheduledOpListFragment>(), message.what)
                STATISTICS -> fragment = findOrCreateFragment(javaClass<StatisticsListFragment>(), message.what)
                CREATE_ACCOUNT -> {
                    AccountEditor.callMeForResult(activity, AccountEditor.NO_ACCOUNT)
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                EDIT_ACCOUNT -> {
                    AccountEditor.callMeForResult(activity, getCurrentAccountId())
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                DELETE_ACCOUNT -> {
                    OperationListFragment.DeleteAccountConfirmationDialog.newInstance(getCurrentAccountId()).show(fragmentManager, "delAccount")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                PREFERENCES -> {
                    val i = Intent(activity, javaClass<ConfigEditor>())
                    activity.startActivity(i)
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                SAVE_ACCOUNT -> {
                    Tools.AdvancedDialog.newInstance(SAVE_ACCOUNT, activity).show(fragmentManager, "backup")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                RESTORE_ACCOUNT -> {
                    Tools.AdvancedDialog.newInstance(RESTORE_ACCOUNT, activity).show(fragmentManager, "restore")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                PROCESS_SCH -> {
                    Tools.AdvancedDialog.newInstance(PROCESS_SCH, activity).show(fragmentManager, "process_scheduling")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                RECOMPUTE_ACCOUNT -> {
                    AccountTable.consolidateSums(activity, activity.getCurrentAccountId())
                    MainActivity.refreshAccountList(activity)
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                else -> Log.d(TAG, "Undeclared fragment")
            }

            val tmp = fragment
            if (tmp != null) {
                val f = tmp as BaseFragment
                fragmentManager.beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.zoom_exit,
                        R.anim.enter_from_left, R.anim.zoom_exit).replace(R.id.content_frame, f,
                        f.getName()).addToBackStack(f.getName()).commit()
            }
            mDrawerLayout.closeDrawer(mDrawerList)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        initShortDate(this)

        if (!BuildConfig.DEBUG)
            Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_main)
        Tools.checkDebugMode(this)

        mToolbar.setTitle("")
        mAccountSpinner.setAdapter(mAccountAdapter)

        registerReceiver(mOnRefreshReceiver, IntentFilter(Tools.INTENT_REFRESH_NEEDED))
        registerReceiver(mOnRefreshReceiver, IntentFilter(INTENT_UPDATE_ACC_LIST))
        //registerReceiver(mOnRefreshReceiver, IntentFilter(INTENT_UPDATE_OP_LIST))

        initDrawer()
        installRadisTimer()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super<BaseActivity>.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            finish()
        } else {
            mActiveFragment = mPrevFragment
            mActiveFragmentId = mPrevFragmentId
            mDrawerList.setItemChecked(mActiveFragmentId, true)
            super<BaseActivity>.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle.onOptionsItemSelected(item) || super<BaseActivity>.onOptionsItemSelected(item)
    }

    private fun setUpDrawerToggle() {
        // Defer code dependent on restoration of previous instance state.
        // NB: required for the drawer indicator to show up!
        mDrawerLayout.setDrawerListener(mDrawerToggle)
        mDrawerLayout.post(object : Runnable {
            override fun run() {
                mDrawerToggle.syncState()
            }
        })
    }

    public fun displayFragment(fragmentId: Int, id: Long) {
        if (fragmentId != mActiveFragmentId || mActiveFragment == null) {
            val msg = Message()
            msg.what = fragmentId
            msg.obj = id
            handler.sendMessage(msg)
        }
    }

    override fun onPause() {
        super<BaseActivity>.onPause()
        handler.pause()
    }

    // nothing to do here, required by Android
    override fun onResume() {
        super<BaseActivity>.onResume()
        Log.d(TAG, "onResume: mactiveFrag:$mActiveFragment")
    }

    override fun onResumeFragments() {
        super<BaseActivity>.onResumeFragments()
        Log.d(TAG, "onResumeFragments:$mActiveFragment")
        mActiveFragment?.setupIcon()
        DBPrefsManager.getInstance(this).fillCache(this, {
            Log.d(TAG, "pref cache ok")
            consolidateDbIfNeeded()
            initAccountStuff()
            handler.resume(this)
            mAccountManager.fetchAllAccounts(false, {
                Log.d(TAG, "all accounts fetched")
                processAccountList(true)
            })
        })
    }

    override fun onDestroy() {
        super<BaseActivity>.onDestroy()
        unregisterReceiver(mOnRefreshReceiver)
    }

    public fun onAccountEditFinished(result: Int) {
        if (result == Activity.RESULT_OK) {
            mAccountManager.fetchAllAccounts(true, {
                mAccountManager.refreshConfig(this, mAccountManager.getCurrentAccountId(this)) // need to be done before setQuickAddVisibility
                val f = mActiveFragment
                if (mActiveFragmentId == OP_LIST && f is OperationListFragment) {
                    f.refreshQuickAdd()
                }
                processAccountList(false)
            })
        } else if (result == Activity.RESULT_CANCELED) {
            val accMan = mAccountManager
            val allAccounts = accMan.allAccountsCursor
            if (allAccounts == null || allAccounts.getCount() == 0) {
                finish()
            }
        }
    }

    private fun processAccountList(resuming: Boolean) {
        val accMan = mAccountManager
        val allAccounts = accMan.allAccountsCursor
        if (allAccounts == null || allAccounts.getCount() == 0) {
            // no account, open create account
            AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT)
        } else {
            //            if (mAccountAdapter == null) {
            //                initAccountStuff()
            //                mAccountManager.setAllAccountsCursor(allAccounts)
            //            } else {
            val old = mAccountAdapter.swapCursor(allAccounts)
            old?.close()
            if (mActiveFragmentId == -1) {
                displayFragment(OP_LIST, (-1).toLong())
            } else {
                displayFragment(mActiveFragmentId, getCurrentAccountId())
            }
            //            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Log.d(TAG, "onActivityResult : " + requestCode);
        super<BaseActivity>.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AccountEditor.ACCOUNT_EDITOR -> onAccountEditFinished(resultCode)
            ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE, ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT,
            ScheduledOperationEditor.ACTIVITY_SCH_OP_CONVERT -> {
                if (mActiveFragment == null) {
                    findOrCreateFragment(if (mActiveFragmentId == OP_LIST) javaClass<OperationListFragment>() else
                        javaClass<ScheduledOpListFragment>(), mActiveFragmentId)
                }
                mActiveFragment?.onOperationEditorResult(requestCode, resultCode, data)
            }
            OperationEditor.OPERATION_EDITOR, OperationEditor.OPERATION_CREATOR -> {
                if (mActiveFragment == null) {
                    findOrCreateFragment(javaClass<OperationListFragment>(), OP_LIST)
                }
                mActiveFragment?.onOperationEditorResult(requestCode, resultCode, data)
                mAccountManager.backupCurAccountId()
                updateAccountList()
            }
            else -> {
            }
        }
    }

    private fun consolidateDbIfNeeded() {
        val prefs = DBPrefsManager.getInstance(this)
        val needConsolidate = prefs.getBoolean(fr.geobert.radis.service.RadisService.CONSOLIDATE_DB, false)
        if (needConsolidate) {
            fr.geobert.radis.service.RadisService.acquireStaticLock(this)
            this.startService(Intent(this, javaClass<fr.geobert.radis.service.RadisService>()))
        }
    }

    private fun initAccountStuff() {
        mAccountAdapter.setViewBinder(SimpleAccountViewBinder())
        mAccountManager.setSimpleCursorAdapter(mAccountAdapter)
        this.setActionBarListNavCbk(object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<out Adapter>?) {
            }

            override fun onItemSelected(p0: AdapterView<out Adapter>?, p1: View?, p2: Int, itemId: Long) {
                val frag = mActiveFragment
                if (frag != null && frag.isAdded()) {
                    frag.onAccountChanged(itemId)
                }
            }
        })
    }

    public fun updateAccountList() {
        mAccountManager.setSimpleCursorAdapter(mAccountAdapter)
        mAccountManager.fetchAllAccounts(true, { onFetchAllAccountCbk() })
    }

    private fun onFetchAllAccountCbk() {
        processAccountList(false)
    }

    private fun initDrawer() {
        setUpDrawerToggle()

        val navDrawerItems = ArrayList<NavDrawerItem>()

        navDrawerItems.add(NavDrawerItem(getString(R.string.operations)))
        navDrawerItems.add(NavDrawerItem(getString(R.string.op_list), R.drawable.op_list_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.scheduled_ops), R.drawable.sched_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.statistics), R.drawable.stat_48))

        navDrawerItems.add(NavDrawerItem(getString(R.string.accounts)))
        navDrawerItems.add(NavDrawerItem(getString(R.string.create_account), R.drawable.new_account_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.account_edit), R.drawable.edit_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.delete_account), R.drawable.trash_48))

        navDrawerItems.add(NavDrawerItem(getString(R.string.advanced)))
        navDrawerItems.add(NavDrawerItem(getString(R.string.preferences), 0)) // TODO icon
        navDrawerItems.add(NavDrawerItem(getString(R.string.backup_db), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.restore_db), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.process_scheduled_transactions), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.recompute_account_sums), 0))

        mDrawerList.setAdapter(NavDrawerListAdapter(getApplicationContext(), navDrawerItems))
        mDrawerList.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                displayFragment(i, getCurrentAccountId())
            }
        })
    }

    private fun installRadisTimer() {
        if (mFirstStart) {
            val i = Intent(this, javaClass<InstallRadisServiceReceiver>())
            i.setAction(Tools.INTENT_RADIS_STARTED)
            sendBroadcast(i) // install radis timer
            fr.geobert.radis.service.RadisService.callMe(this) // call service once
            mFirstStart = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseActivity>.onSaveInstanceState(outState)
        outState.putInt("activeFragId", mActiveFragmentId)
        outState.putInt("prevFragId", mPrevFragmentId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super<BaseActivity>.onRestoreInstanceState(savedInstanceState)
        mActiveFragmentId = savedInstanceState.getInt("activeFragId")
        mPrevFragmentId = savedInstanceState.getInt("prevFragId")
        DBPrefsManager.getInstance(this).fillCache(this, {
            //            mActiveFragment?.onRestoreInstanceState(savedInstanceState)
            initAccountStuff()
            if (mAccountAdapter.isEmpty()) {
                updateDisplay(null)
            }
        })
    }

    public fun setActionBarListNavCbk(callback: AdapterView.OnItemSelectedListener) {
        mAccountSpinner.setOnItemSelectedListener(callback)
    }

    override fun updateDisplay(intent: Intent?) {
        mAccountManager.fetchAllAccounts(true, {
            val f = mActiveFragment
            if (f != null && f.isAdded()) {
                f.updateDisplay(intent)
            }
        })
    }

    public fun getCurrentAccountId(): Long {
        return mAccountManager.getCurrentAccountId(this)
    }

    // for accounts list
    private inner class SimpleAccountViewBinder() : android.support.v4.widget.SimpleCursorAdapter.ViewBinder {
        private var ACCOUNT_NAME_COL = -1
        private var ACCOUNT_CUR_SUM: Int = 0
        private var ACCOUNT_CUR_SUM_DATE: Int = 0
        private var ACCOUNT_CURRENCY: Int = 0
        private var currencySymbol: String? = null

        fun cleanupSymbol(s: String, c: String): String {
            return if (s.contains(c)) c else s
        }

        override fun setViewValue(view: View, cursor: Cursor, i: Int): Boolean {
            if (ACCOUNT_NAME_COL == -1) {
                ACCOUNT_NAME_COL = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME)
                ACCOUNT_CUR_SUM = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM)
                ACCOUNT_CUR_SUM_DATE = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE)
                ACCOUNT_CURRENCY = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CURRENCY)
            }

            try {
                val c = Currency.getInstance(cursor.getString(ACCOUNT_CURRENCY)).getSymbol()
                currencySymbol = cleanupSymbol(c, "£")
                currencySymbol = cleanupSymbol(c, "$")
            } catch (ex: IllegalArgumentException) {
                currencySymbol = ""
            }

            val res: Boolean
            if (i == ACCOUNT_NAME_COL) {
                val textView = view as TextView
                textView.setText(cursor.getString(i))
                res = true
            } else if (i == ACCOUNT_CUR_SUM) {
                val textView = view as TextView
                val stringBuilder = StringBuilder()
                val sum = cursor.getLong(i)
                if (sum < 0) {
                    textView.setTextColor(redColor)
                } else {
                    textView.setTextColor(greenColor)
                }
                stringBuilder.append((sum.toDouble() / 100.0).formatSum())
                stringBuilder.append(' ').append(currencySymbol)
                textView.setText(stringBuilder)
                res = true
            } else if (i == ACCOUNT_CUR_SUM_DATE) {
                val textView = view as TextView
                val dateLong = cursor.getLong(i)
                val stringBuilder = StringBuilder()
                if (dateLong > 0) {
                    stringBuilder.append(getString(R.string.balance_at).format(Date(dateLong).formatDate()))
                } else {
                    stringBuilder.append(getString(R.string.current_sum))
                }
                textView.setText(stringBuilder)
                res = true
            } else {
                res = false
            }
            return res
        }
    }

    private val TAG = "MainActivity"

    companion object {
        public val INTENT_UPDATE_OP_LIST: String = "fr.geobert.radis.UPDATE_OP_LIST"
        public val INTENT_UPDATE_ACC_LIST: String = "fr.geobert.radis.UPDATE_ACC_LIST"

        // used for FragmentHandler
        public val OP_LIST: Int = 1
        public val SCH_OP_LIST: Int = 2
        public val STATISTICS: Int = 3

        public val CREATE_ACCOUNT: Int = 5
        public val EDIT_ACCOUNT: Int = 6
        public val DELETE_ACCOUNT: Int = 7

        public val PREFERENCES: Int = 9
        public val SAVE_ACCOUNT: Int = 10
        public val RESTORE_ACCOUNT: Int = 11
        public val PROCESS_SCH: Int = 12
        public val RECOMPUTE_ACCOUNT: Int = 13

        platformStatic public fun refreshAccountList(ctx: Context) {
            val intent = Intent(INTENT_UPDATE_ACC_LIST)
            ctx.sendBroadcast(intent)
        }
    }
}
