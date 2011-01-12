package fr.geobert.Radis;

import java.text.DecimalFormat;
import java.util.Currency;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class AccountList extends ListActivity {
	private static final int CREATE_ACCOUNT_ID = Menu.FIRST;
	private static final int DELETE_ACCOUNT_ID = Menu.FIRST + 1;
	private static final int EDIT_ACCOUNT_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_ACCOUNT_CREATE = 0;
	private static final int ACTIVITY_ACCOUNT_EDIT = 1;

	private AccountsDbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_list);
		mDbHelper = new AccountsDbAdapter(this);
		mDbHelper.open();
		fillData();
		setTitle(getString(R.string.app_name) + " - " + getString(R.string.accounts_list));
		registerForContextMenu(getListView());
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

			mDbHelper.deleteAccount(info.id);
			fillData();
			return true;
		case EDIT_ACCOUNT_ID:
			Intent i = new Intent(this, AccountEditor.class);
			i.putExtra(Tools.EXTRAS_ACCOUNT_ID, info.id);
			startActivityForResult(i, ACTIVITY_ACCOUNT_EDIT);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private class InnerSimpleCursorAdapter extends SimpleCursorAdapter {
		InnerSimpleCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
		}

		@Override
		public void setViewText(TextView v, String text) {
			if (v.getId() == R.id.account_currency) {
				Currency c = Currency.getInstance(text);
				super.setViewText(v, c.getSymbol());
			} else if (v.getId() == R.id.account_sum) {
				String d = new DecimalFormat("0.00").format(Double
						.parseDouble(text));
				super.setViewText(v, d);
			} else {
				super.setViewText(v, text);
			}
		}
	}

	private void fillData() {
		Cursor accountsCursor = mDbHelper.fetchAllAccounts();
		startManagingCursor(accountsCursor);

		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { AccountsDbAdapter.KEY_ACCOUNT_NAME,
				AccountsDbAdapter.KEY_ACCOUNT_CUR_SUM,
				AccountsDbAdapter.KEY_ACCOUNT_CURRENCY };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.account_name, R.id.account_sum,
				R.id.account_currency };

		// Now create a simple cursor adapter and set it to display
		InnerSimpleCursorAdapter accounts = new InnerSimpleCursorAdapter(this,
				R.layout.account_row, accountsCursor, from, to);
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
		String accountName = data.getString(data.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_NAME));
		Intent i = new Intent(this, OperationList.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, id);
		i.putExtra(AccountsDbAdapter.KEY_ACCOUNT_NAME, accountName);
		startActivity(i);
	}
}