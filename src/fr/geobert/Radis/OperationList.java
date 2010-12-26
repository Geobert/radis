package fr.geobert.Radis;

import java.text.DecimalFormat;
import java.util.Currency;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class OperationList extends ListActivity {
	private static final int CREATE_OP_ID = Menu.FIRST;
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_OP_CREATE = 0;
	private static final int ACTIVITY_OP_EDIT = 1;
	private static final String EVEN_COLOR = "#FFFFFF";
	private static final String ODD_COLOR = "##D6D6FF";

	private OperationsDbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.operation_list);

		LayoutInflater inf = getLayoutInflater();
		View header = inf
				.inflate(R.layout.op_list_header, getListView(), false);

		getListView().addHeaderView(header);
		View footer = inf
				.inflate(R.layout.op_list_footer, getListView(), false);
		getListView().addFooterView(footer);
		//try {
			Bundle extras = getIntent().getExtras();
			Long accountId = extras != null ? extras
					.getLong(AccountsDbAdapter.KEY_ACCOUNT_ROWID) : null;

			mDbHelper = new OperationsDbAdapter(this, accountId);
			mDbHelper.open();
//		} catch (Exception e) {
	//		int i = 0;
//		}

		// fillData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, CREATE_OP_ID, 0, R.string.create_account);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_OP_ID, 0, R.string.edit);
		menu.add(0, DELETE_OP_ID, 0, R.string.delete);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case CREATE_OP_ID:
			createOp();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE_OP_ID:

			mDbHelper.deleteAccount(info.id);
			fillData();
			return true;
		case EDIT_OP_ID:
			Intent i = new Intent(this, AccountEditor.class);
			i.putExtra(AccountsDbAdapter.KEY_ACCOUNT_ROWID, info.id);
			startActivityForResult(i, ACTIVITY_OP_EDIT);
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

	private void createOp() {
		Intent i = new Intent(this, OperationEditor.class);
		startActivityForResult(i, ACTIVITY_OP_CREATE);
	}
}
