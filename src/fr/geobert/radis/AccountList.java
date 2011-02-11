package fr.geobert.radis;

import java.util.Currency;
import java.util.Date;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AccountList extends ListActivity {
	private static final int CREATE_ACCOUNT_ID = Menu.FIRST;
	private static final int DELETE_ACCOUNT_ID = Menu.FIRST + 1;
	private static final int EDIT_ACCOUNT_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_ACCOUNT_CREATE = 0;
	private static final int ACTIVITY_ACCOUNT_EDIT = 1;

	private static final int DIALOG_DELETE = 0;

	private CommonDbAdapter mDbHelper;
	private long mAccountToDelete = 0;
	public static AccountList ACTIVITY;
	public static PendingIntent RESTART_INTENT;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Tools.checkDebugMode(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_list);
		mDbHelper = new CommonDbAdapter(this);
		mDbHelper.open();
		fillData();
		setTitle(getString(R.string.app_name) + " - "
				+ getString(R.string.accounts_list));
		registerForContextMenu(getListView());
		ACTIVITY = this;
		RESTART_INTENT = PendingIntent.getActivity(this.getBaseContext(), 0,
				new Intent(getIntent()), getIntent().getFlags());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, CREATE_ACCOUNT_ID, 0, R.string.create_account);
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
		case CREATE_ACCOUNT_ID:
			createAccount();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
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
			Intent i = new Intent(this, AccountEditor.class);
			i.putExtra(Tools.EXTRAS_ACCOUNT_ID, info.id);
			startActivityForResult(i, ACTIVITY_ACCOUNT_EDIT);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void deleteAccount(long id) {
		mDbHelper.deleteAccount(id);
		fillData();
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
				}
				String txt = Operation.SUM_FORMAT.format(Double.valueOf(sum));
				textView.setText(txt
						+ " "
						+ Currency
								.getInstance(
										cursor.getString(cursor
												.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CURRENCY)))
								.getSymbol());

				return true;
			}
			return false;
		}
	}

	// private class InnerSimpleCursorAdapter extends SimpleCursorAdapter {
	// InnerSimpleCursorAdapter(Context context, int layout, Cursor c,
	// String[] from, int[] to) {
	// super(context, layout, c, from, to);
	// }
	//
	// @Override
	// public void setViewText(TextView v, String text) {
	// if (v.getId() == R.id.account_sum) {
	// String d = Operation.SUM_FORMAT
	// .format(Double.parseDouble(text));
	// d = d + " " + mCurrency;
	// super.setViewText(v, d);
	// } else {
	// super.setViewText(v, text);
	// }
	// }
	// }

	private void fillData() {
		Cursor accountsCursor = mDbHelper.fetchAllAccounts();
		startManagingCursor(accountsCursor);

		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { CommonDbAdapter.KEY_ACCOUNT_NAME,
				CommonDbAdapter.KEY_ACCOUNT_CUR_SUM,
				CommonDbAdapter.KEY_ACCOUNT_CURRENCY };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.account_name, R.id.account_sum };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter accounts = new SimpleCursorAdapter(this,
				R.layout.account_row, accountsCursor, from, to);
		accounts.setViewBinder(new InnerViewBinder());
		setListAdapter(accounts);

	}

	private void createAccount() {
		Intent i = new Intent(this, AccountEditor.class);
		startActivityForResult(i, ACTIVITY_ACCOUNT_CREATE);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		SQLiteCursor data = (SQLiteCursor) l.getItemAtPosition(position);
		String accountName = data.getString(data
				.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_NAME));
		Intent i = new Intent(this, OperationList.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, id);
		i.putExtra(CommonDbAdapter.KEY_ACCOUNT_NAME, accountName);
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
		case Tools.DEBUG_DIALOG:
			return Tools.getDebugDialog(this, mDbHelper);
		case DIALOG_DELETE:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							deleteAccount(mAccountToDelete);
							mAccountToDelete = 0;
						}
					});
		}
		return null;
	}
}