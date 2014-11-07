package fr.geobert.radis;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.crashlytics.android.Crashlytics;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.OnRefreshReceiver;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.PauseHandler;
import fr.geobert.radis.tools.PrefsManager;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;
import fr.geobert.radis.ui.OperationListFragment;
import fr.geobert.radis.ui.ScheduledOpListFragment;
import fr.geobert.radis.ui.StatisticsListFragment;
import fr.geobert.radis.ui.drawer.NavDrawerItem;
import fr.geobert.radis.ui.drawer.NavDrawerListAdapter;
import fr.geobert.radis.ui.editor.AccountEditor;
import fr.geobert.radis.ui.editor.OperationEditor;
import fr.geobert.radis.ui.editor.ScheduledOperationEditor;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;

public class MainActivity extends BaseActivity implements UpdateDisplayInterface {
    public static final String INTENT_UPDATE_OP_LIST = "fr.geobert.radis.UPDATE_OP_LIST";
    public static final String INTENT_UPDATE_ACC_LIST = "fr.geobert.radis.UPDATE_ACC_LIST";
    private static final String TAG = "MainActivity";
    //    private static final int RESUMING = 1;
    // robotium test set this to true to activate database cleaning on launch
    public static boolean ROBOTIUM_MODE = false;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private boolean mFirstStart = true;
    private SimpleCursorAdapter mAccountAdapter;
    private OnRefreshReceiver mOnRefreshReceiver;
    private IntentFilter mOnInsertionIntentFilter;
    private BaseFragment mActiveFragment;
    private BaseFragment mPrevFragment;
    private int redColor;
    private int greenColor;

    // used for FragmentHandler
    public static final int OP_LIST = 1;
    public static final int SCH_OP_LIST = 2;

    public static final int STATISTICS = 4;

    public static final int CREATE_ACCOUNT = 6;
    public static final int EDIT_ACCOUNT = 7;
    public static final int DELETE_ACCOUNT = 8;

    public static final int PREFERENCES = 10;
    public static final int SAVE_ACCOUNT = 11;
    public static final int RESTORE_ACCOUNT = 12;
    public static final int PROCESS_SCH = 13;
    public static final int RECOMPUTE_ACCOUNT = 14;

    private FragmentHandler handler;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mActiveFragmentId = -1;
    private int mPrevFragmentId;

    private class DrawerClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            displayFragment(i, getCurrentAccountId());
        }
    }

    private class FragmentHandler extends PauseHandler {
        private MainActivity activity;

        FragmentHandler(MainActivity activity) {
            this.activity = activity;
        }

        public void setActivity(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected boolean storeMessage(Message message) {
            return true;
        }

        protected Fragment findOrCreateFragment(Class c, final int fragmentId) {
            Fragment fragment;
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            fragment = fragmentManager.findFragmentByTag(c.getName());
            if (fragment == null) {
                try {
                    fragment = (Fragment) c.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return fragment;
            } else {
                mPrevFragment = mActiveFragment;
                mActiveFragment = (BaseFragment) fragment;
                mPrevFragmentId = mActiveFragmentId;
                mActiveFragmentId = fragmentId;
                mDrawerList.setItemChecked(mActiveFragmentId, true);
                mDrawerLayout.closeDrawer(mDrawerList);
                fragmentManager.popBackStack(c.getName(), 0);
                return null;
            }
        }

        @Override
        protected void processMessage(Message message) {
            Fragment fragment = null;
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            switch (message.what) {
                case OP_LIST:
                    fragment = findOrCreateFragment(OperationListFragment.class, message.what);
                    break;
                case SCH_OP_LIST:
                    fragment = findOrCreateFragment(ScheduledOpListFragment.class, message.what);
                    break;
                case STATISTICS:
                    fragment = findOrCreateFragment(StatisticsListFragment.class, message.what);
                    break;
                case CREATE_ACCOUNT:
                    AccountEditor.callMeForResult(activity, AccountEditor.NO_ACCOUNT);
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case EDIT_ACCOUNT:
                    AccountEditor.callMeForResult(activity, getCurrentAccountId());
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case DELETE_ACCOUNT:
                    OperationListFragment.DeleteAccountConfirmationDialog.
                            newInstance(getCurrentAccountId()).show(fragmentManager, "delAccount");
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case PREFERENCES:
                    Intent i = new Intent(activity, RadisConfiguration.class);
                    activity.startActivity(i);
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case SAVE_ACCOUNT:
                    Tools.AdvancedDialog.newInstance(SAVE_ACCOUNT, activity).show(fragmentManager, "backup");
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case RESTORE_ACCOUNT:
                    Tools.AdvancedDialog.newInstance(RESTORE_ACCOUNT, activity).show(fragmentManager, "restore");
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case PROCESS_SCH:
                    Tools.AdvancedDialog.newInstance(PROCESS_SCH, activity).show(fragmentManager,
                            "process_scheduling");
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                case RECOMPUTE_ACCOUNT:
                    AccountTable.consolidateSums(activity, activity.getCurrentAccountId());
                    MainActivity.refreshAccountList(activity);
                    mDrawerList.setItemChecked(mActiveFragmentId, true);
                    break;
                default:
                    Log.d(TAG, "Undeclared fragment");
            }

            if (fragment != null) {
                mPrevFragment = mActiveFragment;
                mActiveFragment = (BaseFragment) fragment;
                mPrevFragmentId = mActiveFragmentId;
                mActiveFragmentId = message.what;
                mDrawerList.setItemChecked(mActiveFragmentId, true);
                fragmentManager.beginTransaction().
                        setCustomAnimations(R.anim.enter_from_right, R.anim.zoom_exit,
                                R.anim.enter_from_left, R.anim.zoom_exit).
                        replace(R.id.content_frame, fragment, mActiveFragment.getName()).
                        addToBackStack(mActiveFragment.getName()).commit();
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    public static void refreshAccountList(final Context ctx) {
        Intent intent = new Intent(INTENT_UPDATE_ACC_LIST);
        ctx.sendBroadcast(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG)
            Crashlytics.start(this);
        setContentView(R.layout.activity_main);
        handler = new FragmentHandler(this);
        Tools.checkDebugMode(this);
        cleanDatabaseIfTestingMode();

        ActionBar actionbar = getSupportActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowTitleEnabled(false);

        Resources resources = getResources();
        redColor = resources.getColor(R.color.op_alert);
        greenColor = resources.getColor(R.color.positiveSum);

        mOnRefreshReceiver = new OnRefreshReceiver(this);
        mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_REFRESH_NEEDED);

        registerReceiver(mOnRefreshReceiver, mOnInsertionIntentFilter);
        registerReceiver(mOnRefreshReceiver, new IntentFilter(INTENT_UPDATE_ACC_LIST));
        registerReceiver(mOnRefreshReceiver, new IntentFilter(INTENT_UPDATE_OP_LIST));

        initDrawer();
        installRadisTimer();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            finish();
        } else {
            mActiveFragment = mPrevFragment;
            mActiveFragmentId = mPrevFragmentId;
            mDrawerList.setItemChecked(mActiveFragmentId, true);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void setUpDrawerToggle() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                             /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        // NB: required for the drawer indicator to show up!
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void displayFragment(int fragmentId, long id) {
        if (fragmentId != mActiveFragmentId || mActiveFragment == null) {
            Message msg = new Message();
            msg.what = fragmentId;
            msg.obj = id;
            handler.sendMessage(msg);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.setActivity(this);
        handler.resume();
        DBPrefsManager.getInstance(this).fillCache(this, new Runnable() {
            @Override
            public void run() {
                consolidateDbIfNeeded();
                if (mAccountAdapter == null) {
                    initAccountStuff();
                }
                mAccountManager.fetchAllAccounts(MainActivity.this, false, new Runnable() {
                    @Override
                    public void run() {
                        processAccountList(true);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mOnRefreshReceiver) {
            unregisterReceiver(mOnRefreshReceiver);
        }
    }

    public void onAccountEditFinished(int result) {
        if (result == Activity.RESULT_OK) {
            mAccountManager.fetchAllAccounts(MainActivity.this, true, new Runnable() {
                @Override
                public void run() {
                    processAccountList(false);
                }
            });
        } else if (result == Activity.RESULT_CANCELED) {
            AccountManager accMan = mAccountManager;
            Cursor allAccounts = accMan.getAllAccountsCursor();
            if (allAccounts == null || allAccounts.getCount() == 0) {
                finish();
            }
        }
    }

    private void processAccountList(boolean resuming) {
        AccountManager accMan = mAccountManager;
        Cursor allAccounts = accMan.getAllAccountsCursor();
        if (allAccounts == null || allAccounts.getCount() == 0) {
            // no account, open create account
            AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT);
        } else {
            if (mAccountAdapter == null) {
                initAccountStuff();
            } else {
                Cursor old = mAccountAdapter.swapCursor(allAccounts);
                if (old != null) {
                    old.close();
                }
            }
            if (mActiveFragmentId == -1) {
                displayFragment(OP_LIST, -1);
            } else if (resuming) {
                displayFragment(mActiveFragmentId, getCurrentAccountId());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.d(TAG, "onActivityResult : " + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case AccountEditor.ACCOUNT_EDITOR:
                onAccountEditFinished(resultCode);
                break;
            case ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE:
            case ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT:
            case ScheduledOperationEditor.ACTIVITY_SCH_OP_CONVERT:
                // nothing as we call RadisService to insert, and it calls updateDisplay
                break;
            case OperationEditor.OPERATION_EDITOR:
                ((OperationListFragment) mActiveFragment).onOperationEditorResult(resultCode, data);
                mAccountManager.backupCurAccountId();
                updateAccountList();
                break;
        }
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        if (drawerOpen) {
//            mDrawerList.setItemChecked(mActiveFragmentId, true);
//        }
//        return super.onPrepareOptionsMenu(menu);
//    }

    private void consolidateDbIfNeeded() {
        PrefsManager prefs = PrefsManager.getInstance(this);
        Boolean needConsolidate = prefs.getBoolean(RadisService.CONSOLIDATE_DB, false);
        Log.d(TAG, "needConsolidate : " + needConsolidate);
        if (needConsolidate) {
            RadisService.acquireStaticLock(this);
            this.startService(new Intent(this, RadisService.class));
        }
    }

    private void initAccountStuff() {
        String[] from = new String[]{AccountTable.KEY_ACCOUNT_NAME,
                AccountTable.KEY_ACCOUNT_CUR_SUM,
                AccountTable.KEY_ACCOUNT_CUR_SUM_DATE,
                AccountTable.KEY_ACCOUNT_CURRENCY};

        int[] to = new int[]{android.R.id.text1, R.id.account_sum, R.id.account_balance_at};

        mAccountAdapter = new SimpleCursorAdapter(this, R.layout.account_row, null, from, to, 0);
        mAccountAdapter.setViewBinder(new SimpleAccountViewBinder());
        mAccountManager.setSimpleCursorAdapter(mAccountAdapter);
        this.setActionBarListNavCbk(mAccountAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                if (mActiveFragment != null && mActiveFragment.isAdded()) {
                    return mActiveFragment.onAccountChanged(itemId);
                }
                return false;
            }
        });
    }

    public void updateAccountList() {
        mAccountManager.fetchAllAccounts(this, true, new Runnable() {
            @Override
            public void run() {
                onFetchAllAccountCbk();
            }
        });
    }

    private void onFetchAllAccountCbk() {
        processAccountList(false);
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        setUpDrawerToggle();

        ArrayList<NavDrawerItem> navDrawerItems = new ArrayList<NavDrawerItem>();

        navDrawerItems.add(new NavDrawerItem(getString(R.string.operations)));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.op_list), R.drawable.op_list_48));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.scheduled_ops), R.drawable.sched_48));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.statistics), R.drawable.stat_48));

        navDrawerItems.add(new NavDrawerItem(getString(R.string.accounts)));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.create_account), R.drawable.new_account_48)); // TODO another icon
        navDrawerItems.add(new NavDrawerItem(getString(R.string.account_edit), R.drawable.edit_48));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.delete_account), R.drawable.trash_48));

        navDrawerItems.add(new NavDrawerItem(getString(R.string.advanced)));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.preferences), 0)); // TODO icon
        navDrawerItems.add(new NavDrawerItem(getString(R.string.backup_db), 0));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.restore_db), 0));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.process_scheduled_transactions), 0));
        navDrawerItems.add(new NavDrawerItem(getString(R.string.recompute_account_sums), 0));

        mDrawerList.setAdapter(new NavDrawerListAdapter(getApplicationContext(), navDrawerItems));
        mDrawerList.setOnItemClickListener(new DrawerClickListener());
    }

    private void installRadisTimer() {
        if (mFirstStart) {
            Intent i = new Intent(this, InstallRadisServiceReceiver.class);
            i.setAction(Tools.INTENT_RADIS_STARTED);
            sendBroadcast(i);
            RadisService.callMe(this);
            mFirstStart = false;
        }
    }

    private void cleanDatabaseIfTestingMode() {
        // if run by robotium, delete database, can't do it from robotium test, it leads to crash
        // see http://stackoverflow.com/questions/12125656/robotium-testing-failed-because-of-deletedatabase
        if (ROBOTIUM_MODE) {
            DBPrefsManager.getInstance(this).resetAll();
            ContentProviderClient client = getContentResolver()
                    .acquireContentProviderClient("fr.geobert.radis.db");
            DbContentProvider provider = (DbContentProvider) client.getLocalContentProvider();
            provider.deleteDatabase(this);
            client.release();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("activeFragId", mActiveFragmentId);
        outState.putInt("prevFragId", mPrevFragmentId);
        if (mActiveFragment != null) {
            mActiveFragment.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mActiveFragmentId = savedInstanceState.getInt("activeFragId");
        mPrevFragmentId = savedInstanceState.getInt("prevFragId");
        DBPrefsManager.getInstance(this).fillCache(this, new Runnable() {
            @Override
            public void run() {
                if (mActiveFragment != null) {
                    mActiveFragment.onRestoreInstanceState(savedInstanceState);
                }
                if (mAccountManager.getSimpleCursorAdapter() == null) {
                    initAccountStuff();
                }
                if (mAccountAdapter == null || mAccountAdapter.isEmpty()) {
                    updateDisplay(null);
                }
            }
        });
    }

    public void setActionBarListNavCbk(SpinnerAdapter adapter, ActionBar.OnNavigationListener callback) {
        getSupportActionBar().setListNavigationCallbacks(adapter, callback);
    }

    @Override
    public void updateDisplay(final Intent intent) {
        mAccountManager.fetchAllAccounts(this, true, new Runnable() {
            @Override
            public void run() {
                if (mActiveFragment != null) {
                    mActiveFragment.updateDisplay(intent);
                }
            }
        });
    }

    public Long getCurrentAccountId() {
        return mAccountManager.getCurrentAccountId(this);
    }

    // for accounts list
    private class SimpleAccountViewBinder implements SimpleCursorAdapter.ViewBinder {
        private int ACCOUNT_NAME_COL = -1;
        private int ACCOUNT_CUR_SUM;
        private int ACCOUNT_CUR_SUM_DATE;
        private int ACCOUNT_CURRENCY;
        private String currencySymbol = null;

        private SimpleAccountViewBinder() {
        }

        String cleanupSymbol(String s, String c) {
            return s.contains(c) ? c : s;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int i) {
            if (ACCOUNT_NAME_COL == -1) {
                ACCOUNT_NAME_COL = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME);
                ACCOUNT_CUR_SUM = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM);
                ACCOUNT_CUR_SUM_DATE = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE);
                ACCOUNT_CURRENCY = cursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CURRENCY);
            }

            try {
                currencySymbol = Currency.getInstance(cursor.getString(ACCOUNT_CURRENCY)).getSymbol();
                currencySymbol = cleanupSymbol(currencySymbol, "£");
                currencySymbol = cleanupSymbol(currencySymbol, "$");
            } catch (IllegalArgumentException ex) {
                currencySymbol = "";
            }

            boolean res;
            if (i == ACCOUNT_NAME_COL) {
                TextView textView = (TextView) view;
                textView.setText(cursor.getString(i));
                res = true;
            } else if (i == ACCOUNT_CUR_SUM) {
                TextView textView = (TextView) view;
                StringBuilder stringBuilder = new StringBuilder();
                long sum = cursor.getLong(i);
                if (sum < 0) {
                    textView.setTextColor(redColor);
                } else {
                    textView.setTextColor(greenColor);
                }
                stringBuilder.append(Formater.getSumFormater().format(sum / 100.0d));
                stringBuilder.append(' ').append(currencySymbol);
                textView.setText(stringBuilder);
                res = true;
            } else if (i == ACCOUNT_CUR_SUM_DATE) {
                TextView textView = (TextView) view;
                long dateLong = cursor.getLong(i);
                StringBuilder stringBuilder = new StringBuilder();
                if (dateLong > 0) {
                    stringBuilder.append(String.format(
                            getString(R.string.balance_at),
                            Formater.getFullDateFormater().format(
                                    new Date(dateLong))
                    ));
                } else {
                    stringBuilder.append(getString(R.string.current_sum));
                }
                textView.setText(stringBuilder);
                res = true;
            } else {
                res = false;
            }
            return res;
        }
    }
}