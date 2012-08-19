package fr.geobert.radis;

import java.text.ParseException;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.editor.AccountEditor;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.QuickAddController;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public class AccountList extends FragmentActivity implements
		UpdateDisplayInterface, LoaderCallbacks<Cursor> {
	private static final int DELETE_ACCOUNT_ID = Menu.FIRST + 1;
	private static final int EDIT_ACCOUNT_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_ACCOUNT_CREATE = 0;
	private static final int ACTIVITY_ACCOUNT_EDIT = 1;

	private static final int DIALOG_DELETE = 0;
	private static final int GET_ACCOUNTS = 200;

	private long mAccountToDelete = 0;
	private Button mScheduledListBtn;
	private Button mAddAccountBtn;

	private boolean mFirstStart = true;
	private OnInsertionReceiver mOnInsertionReceiver;
	private IntentFilter mOnInsertionIntentFilter;
	private SimpleCursorAdapter mAccountsAdapter;
	private QuickAddController mQuickAddController;
	private TextView mQuickAddText;
	private InnerViewBinder mViewBinder;
	private ListView mListView;
	private ProgressDialog mProgress;

	private class SimpleAccountCursorAdapter extends SimpleCursorAdapter {
		SimpleAccountCursorAdapter(Context context, int layout, String[] from,
				int[] to) {
			super(context, layout, null, from, to,
					SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			try {
				AccountTable
						.updateAccountProjectionDate(
								AccountList.this,
								cursor.getLong(cursor
										.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)));
			} catch (ParseException e) {
				// nothing
			}
		}
	}

	public static void callMe(Context ctx) {
		Intent intent = new Intent(ctx, AccountList.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ctx.startActivity(intent);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Tools.checkDebugMode(this);
		super.onCreate(savedInstanceState);
		mProgress = ProgressDialog.show(this, "", getString(R.string.loading));
		InfoTables.fillCaches(this);
		setContentView(R.layout.account_list);
		mListView = (ListView) findViewById(android.R.id.list);
		mAccountsAdapter = null;
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mScheduledListBtn = (Button) findViewById(R.id.startScheduledListBtn);
		mScheduledListBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startScheduledOpsList();
			}
		});

		mAddAccountBtn = (Button) findViewById(R.id.startAccountEditorBtn);
		mAddAccountBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createAccount();
			}
		});
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(),
					0).versionName;
		} catch (NameNotFoundException e) {
		}

		setTitle(getString(R.string.app_name) + " " + versionName + " - "
				+ getString(R.string.accounts_list));
		registerForContextMenu(mListView);

		mOnInsertionReceiver = new OnInsertionReceiver(this);
		mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);

		final GestureDetector gestureDetector = new GestureDetector(this,
				new ListViewSwipeDetector(mListView, new ListSwipeAction() {
					@Override
					public void run() {
						if (mRowId > 0) {
							startAccountEdit(mRowId);
						}
					}
				}, new ListSwipeAction() {
					@Override
					public void run() {
						if (mRowId > 0) {
							openOperationsList(mPosition, mRowId);

						}
					}
				}));
		View.OnTouchListener gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		mListView.setOnTouchListener(gestureListener);

		mQuickAddController = new QuickAddController(this, this);
		mQuickAddText = (TextView) findViewById(R.id.quickadd_target_text);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				openOperationsList(position, id);
			}
		});

		if (mFirstStart) {
			Intent i = new Intent(this, InstallRadisServiceReceiver.class);
			i.setAction(Tools.INTENT_RADIS_STARTED);
			sendBroadcast(i);
			mFirstStart = false;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mQuickAddController.clearFocus();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mQuickAddController.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mQuickAddController.onRestoreInstanceState(state);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.common_menu, menu);
		menu.findItem(R.id.recompute_account).setVisible(false);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ACCOUNT_ID, 0, R.string.edit);
		menu.add(0, DELETE_ACCOUNT_ID, 0, R.string.delete);

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		default:
			if (Tools.onDefaultMenuSelected(this, featureId, item)) {
				return true;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void startScheduledOpsList() {
		ScheduledOpList.callMe(this, 0);
	}

	private void startAccountEdit(long id) {
		Intent i = new Intent(this, AccountEditor.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, id);
		startActivityForResult(i, ACTIVITY_ACCOUNT_EDIT);
		AccountList.this.overridePendingTransition(R.anim.enter_from_left, 0);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE_ACCOUNT_ID:
			mAccountToDelete = info.id;
			showDialog(DIALOG_DELETE);
			return true;
		case EDIT_ACCOUNT_ID:
			startAccountEdit(info.id);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void deleteAccount(long id) {
		AccountTable.deleteAccount(this, id);
		mScheduledListBtn.setEnabled(mListView.getAdapter().getCount() > 0);
		if (null != mViewBinder && id == mViewBinder.accountId) {
			DBPrefsManager.getInstance(this).clearAccountRelated();
			mViewBinder.accountId = 0;
		}
	}

	private class InnerViewBinder implements SimpleCursorAdapter.ViewBinder {
		private Resources res = getResources();
		public long accountId;

		public InnerViewBinder(long accountId) {
			this.accountId = accountId;
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);

			if (colName.equals(AccountTable.KEY_ACCOUNT_NAME)) {
				if (cursor.getLong(cursor
						.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)) == accountId) {
					mQuickAddText
							.setText(AccountList.this.getString(
									R.string.quickadd_target,
									cursor.getString(cursor
											.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME))));
				}
			} else if (colName.equals(AccountTable.KEY_ACCOUNT_CUR_SUM)) {
				TextView textView = ((TextView) view);
				long sum = cursor.getLong(columnIndex);
				if (sum < 0) {
					textView.setTextColor(res.getColor(R.color.op_alert));
				} else {
					textView.setTextColor(res.getColor(R.color.positiveSum));
				}
				String txt = Formater.getSumFormater().format(sum / 100.0d);
				try {
					textView.setText(txt
							+ " "
							+ Currency
									.getInstance(
											cursor.getString(cursor
													.getColumnIndex(AccountTable.KEY_ACCOUNT_CURRENCY)))
									.getSymbol());
				} catch (IllegalArgumentException e) {
					// TODO : clean this code after resolving issue 130
					Currency currency = Currency.getInstance(Locale
							.getDefault());
					textView.setText(currency.getSymbol());
					AccountTable
							.updateAccountCurrency(
									AccountList.this,
									cursor.getLong(cursor
											.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)),
									currency.getCurrencyCode());
				}
				return true;
			} else if (colName.equals(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE)) {
				TextView textView = ((TextView) view);
				long dateLong = cursor.getLong(cursor.getColumnIndex(colName));
				if (dateLong > 0) {
					textView.setText(String.format(
							getString(R.string.balance_at),
							Formater.getFullDateFormater().format(
									new Date(dateLong))));
				} else {
					textView.setText("");
				}
				return true;
			}
			return false;
		}
	}

	private void fillData() {
		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { AccountTable.KEY_ACCOUNT_NAME,
				AccountTable.KEY_ACCOUNT_CUR_SUM,
				AccountTable.KEY_ACCOUNT_CUR_SUM_DATE,
				AccountTable.KEY_ACCOUNT_CURRENCY };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.account_name, R.id.account_sum,
				R.id.account_balance_at };

		if (mAccountsAdapter == null) {
			updateDisplay(null);

			// Now create a simple cursor adapter and set it to display
			mAccountsAdapter = new SimpleAccountCursorAdapter(this,
					R.layout.account_row, from, to);
			mViewBinder = new InnerViewBinder(DBPrefsManager.getInstance(this)
					.getLong(RadisConfiguration.KEY_DEFAULT_ACCOUNT, 0));
			mAccountsAdapter.setViewBinder(mViewBinder);
			mListView.setAdapter(mAccountsAdapter);
		}
	}

	private void createAccount() {
		Intent i = new Intent(this, AccountEditor.class);
		startActivityForResult(i, ACTIVITY_ACCOUNT_CREATE);
		AccountList.this.overridePendingTransition(R.anim.enter_from_left, 0);
	}

	private void openOperationsList(int position, long accountId) {
		Intent i = new Intent(this, OperationList.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, accountId);
		startActivity(i);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (Tools.onKeyLongPress(keyCode, event, this)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							deleteAccount(mAccountToDelete);
							mAccountToDelete = 0;
						}
					}, R.string.account_delete_confirmation);
		default:
			return Tools.onDefaultCreateDialog(this, id);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mOnInsertionReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		DBPrefsManager.getInstance(this).fillCache(this, new Runnable() {

			@Override
			public void run() {
				onPrefsInit();
			}
		});

		registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
	}

	protected void onPrefsInit() {
		boolean hideQuickAdd = DBPrefsManager.getInstance(this).getBoolean(
				RadisConfiguration.KEY_HIDE_ACCOUNT_QUICK_ADD, false);
		int visibility = View.VISIBLE;
		if (hideQuickAdd) {
			visibility = View.GONE;
		}
		mQuickAddController.initViewBehavior();
		mQuickAddText.setVisibility(visibility);
		mQuickAddController.setVisibility(visibility);
		fillData();
	}

	private void updateTargetTextView(Cursor accountsCursor) {
		if (accountsCursor.getCount() > 0) {
			long accountId = DBPrefsManager.getInstance(this).getLong(
					RadisConfiguration.KEY_DEFAULT_ACCOUNT, 0);
			mQuickAddController.setAccount(accountId);
			if (accountId == 0) {
				mQuickAddText.setText(R.string.quickadd_target_to_configure);
			}
		} else {
			mQuickAddText.setText(R.string.quickadd_target_no_account);
			mQuickAddController.setAccount(0);
		}
	}

	@Override
	public void updateDisplay(Intent intent) {
		mProgress.show();
		getSupportLoaderManager().initLoader(GET_ACCOUNTS, null, this);
		if (mListView.getAdapter() != null) {
			mScheduledListBtn.setEnabled(mListView.getAdapter().getCount() > 0);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return AccountTable.getAllAccountsLoader(this);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
		case GET_ACCOUNTS:
			mProgress.dismiss();
			Cursor old = mAccountsAdapter.swapCursor(data);
			if (old != null) {
				old.close();
			}
			// if there are no results
			if (data.getCount() == 0) {
				// let the user know
				mListView.setEmptyView(findViewById(android.R.id.empty));
			} else {
				// otherwise clear it, so it won't flash in between cursor loads
				mListView.setEmptyView(null);
			}
			mScheduledListBtn.setEnabled(mAccountsAdapter.getCount() > 0);
			updateTargetTextView(data);
			break;

		default:
			break;
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case GET_ACCOUNTS:
			Cursor old = mAccountsAdapter.swapCursor(null);
			if (old != null) {
				old.close();
			}
			break;
		default:
			break;
		}
	}
}