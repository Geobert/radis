package fr.geobert.radis;

import java.util.Currency;
import java.util.Date;

import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.editor.AccountEditor;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AccountList extends ListActivity implements RadisListActivity {
	private static final int DELETE_ACCOUNT_ID = Menu.FIRST + 1;
	private static final int EDIT_ACCOUNT_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_ACCOUNT_CREATE = 0;
	private static final int ACTIVITY_ACCOUNT_EDIT = 1;

	private static final int DIALOG_DELETE = 0;

	private CommonDbAdapter mDbHelper;
	private long mAccountToDelete = 0;
	private View mScheduledListBtn;
	public static AccountList ACTIVITY;
	public static PendingIntent RESTART_INTENT;

	private boolean mFirstStart = true;
	private OnInsertionReceiver mOnInsertionReceiver;
	private IntentFilter mOnInsertionIntentFilter;
	private Cursor mAccountsCursor;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (!Formater.isInit()) {
			Formater.init();
		}
		Tools.checkDebugMode(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_list);
		mDbHelper = new CommonDbAdapter(this);
		mDbHelper.open();
		mScheduledListBtn = findViewById(R.id.startScheduledListBtn);
		mScheduledListBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startScheduledOpsList();
			}
		});
		
		setTitle(getString(R.string.app_name) + " - "
				+ getString(R.string.accounts_list));
		registerForContextMenu(getListView());
		ACTIVITY = this;
		RESTART_INTENT = PendingIntent.getActivity(this.getBaseContext(), 0,
				new Intent(getIntent()), getIntent().getFlags());
		mOnInsertionReceiver = new OnInsertionReceiver(this);
		mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);
		
		final GestureDetector gestureDetector = new GestureDetector(
				new ListViewSwipeDetector(getListView(), new ListSwipeAction() {
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
		getListView().setOnTouchListener(gestureListener);
		
		if (mFirstStart) {
			Intent i = new Intent(this, InstallRadisServiceReceiver.class);
			i.setAction(Tools.INTENT_RADIS_STARTED);
			sendBroadcast(i);
			mFirstStart = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.account_list_menu, menu);
		inflater.inflate(R.menu.common_menu, menu);
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
		case R.id.create_account:
			createAccount();
			return true;
		default:
			if (Tools.onDefaultMenuSelected(this, featureId, item)) {
				return true;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void startScheduledOpsList() {
		Intent i = new Intent(this, ScheduledOpList.class);
		startActivity(i);
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
		mDbHelper.deleteAccount(id);
		mAccountsCursor.requery();
		mScheduledListBtn.setEnabled(getListAdapter().getCount() > 0);
	}

	private class InnerViewBinder implements SimpleCursorAdapter.ViewBinder {
		private Resources res = getResources();

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);

			if (colName.equals(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM)) {
				TextView textView = ((TextView) view);
				double sum = cursor.getDouble(columnIndex);
				if (sum < 0.0) {
					textView.setTextColor(res.getColor(R.color.op_alert));
				} else {
					textView.setTextColor(res.getColor(R.color.positiveSum));
				}
				String txt = Formater.SUM_FORMAT.format(Double.valueOf(sum));
				textView.setText(txt
						+ " "
						+ Currency
								.getInstance(
										cursor.getString(cursor
												.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CURRENCY)))
								.getSymbol());

				return true;
			} else if (colName.equals(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE)) {
				TextView textView = ((TextView) view);
				long dateLong = cursor.getLong(cursor.getColumnIndex(colName));
				if (dateLong > 0) {
					textView.setText(String.format(
							getString(R.string.balance_at),
							Formater.DATE_FORMAT.format(new Date(dateLong))));
				} else {
					textView.setText("");
				}
				return true;
			}
			return false;
		}
	}

	private void fillData() {
		mAccountsCursor = mDbHelper.fetchAllAccounts();
		startManagingCursor(mAccountsCursor);
		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { CommonDbAdapter.KEY_ACCOUNT_NAME,
				CommonDbAdapter.KEY_ACCOUNT_CUR_SUM,
				CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE,
				CommonDbAdapter.KEY_ACCOUNT_CURRENCY };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.account_name, R.id.account_sum,
				R.id.account_balance_at };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter accountsAdapter = new SimpleCursorAdapter(this,
				R.layout.account_row, mAccountsCursor, from, to);
		accountsAdapter.setViewBinder(new InnerViewBinder());
		setListAdapter(accountsAdapter);
		mScheduledListBtn.setEnabled(accountsAdapter.getCount() > 0);
	}

	private void createAccount() {
		Intent i = new Intent(this, AccountEditor.class);
		startActivityForResult(i, ACTIVITY_ACCOUNT_CREATE);
	}

	private void openOperationsList(int position, long accountId) {
		SQLiteCursor data = (SQLiteCursor) getListView().getItemAtPosition(
				position);
		String accountName = data.getString(data
				.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_NAME));
		Intent i = new Intent(this, OperationList.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, accountId);
		i.putExtra(CommonDbAdapter.KEY_ACCOUNT_NAME, accountName);
		startActivity(i);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		openOperationsList(position, id);
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
					});
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
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
		fillData();
		registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
	}

	@Override
	public void updateDisplay(Intent intent) {
		mAccountsCursor.requery();
		mScheduledListBtn.setEnabled(getListAdapter().getCount() > 0);
	}
}