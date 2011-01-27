package fr.geobert.Radis;

import java.text.DecimalFormat;
import java.util.GregorianCalendar;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class OperationList extends ListActivity {
	private static final int CREATE_OP_ID = Menu.FIRST;
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_OP_CREATE = 0;
	private static final int ACTIVITY_OP_EDIT = 1;

	private OperationsDbAdapter mDbHelper;
	private Long mAccountId;
	private String mAccountName;
	private Cursor mCurAccount;
	private DecimalFormat mDecimalFormat;
	private final int NB_LAST_OPS = 20;
	private final int OFFSET = 60;
	private Cursor mLastOps = null;
	private GregorianCalendar mLastSelectedDate;

	private class InnerViewBinder implements SimpleCursorAdapter.ViewBinder {
		private Resources res = getResources();

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);

			if (colName.equals(OperationsDbAdapter.KEY_OP_SUM)) {
				TextView textView = ((TextView) view);
				double sum = cursor.getDouble(columnIndex);
				if (sum >= 0.0) {
					textView.setTextColor(res.getColor(R.color.positiveSum));
				} else {
					textView.setTextColor(res.getColor(R.color.blackSum));
				}
				String txt = mDecimalFormat.format(Double.valueOf(sum));
				textView.setText(txt);
				return true;
			} else if (colName.equals(OperationsDbAdapter.KEY_OP_DATE)) {
				Operation op = new Operation();
				long date = cursor.getLong(columnIndex);
				op.setDate(date);
				((TextView) view).setText(op.getShortDateStr());
				return true;
			}
			return false;
		}
	}

	private class SelectedCursorAdapter extends SimpleCursorAdapter {
		private int selectedPos = -1;
		private int[] colors = new int[] { R.drawable.odd_op_line,
				R.drawable.even_op_line };

		SelectedCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
		}

		public void setSelectedPosition(int pos) {
			selectedPos = pos;
			// inform the view of this change
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);
			if (selectedPos == position) {
				v.setBackgroundResource(R.color.op_selected);
			} else {
				v.setBackgroundResource(colors[position % colors.length]);
			}
			return v;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mDecimalFormat = new DecimalFormat();
		mDecimalFormat.setMaximumFractionDigits(2);
		mDecimalFormat.setMinimumFractionDigits(2);
		setContentView(R.layout.operation_list);
		registerForContextMenu(getListView());

		// LayoutInflater inf = getLayoutInflater();
		// View footer = inf
		// .inflate(R.layout.op_list_footer, getListView(), false);
		// getListView().addFooterView(footer);
		Bundle extras = getIntent().getExtras();
		mAccountId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID)
				: null;
		mAccountName = extras != null ? extras
				.getString(AccountsDbAdapter.KEY_ACCOUNT_NAME) : null;
		setTitle(getString(R.string.app_name) + " - " + mAccountName);
		mDbHelper = new OperationsDbAdapter(this, mAccountId);
		mDbHelper.open();
		mCurAccount = mDbHelper.fetchAccount(mAccountId);
		startManagingCursor(mCurAccount);
		fillData();
		double curSum = mCurAccount.getDouble(mCurAccount
				.getColumnIndex(AccountsDbAdapter.KEY_ACCOUNT_CUR_SUM));
		updateFutureSumDisplay(curSum);
		updateSumAtDateDisplay(new GregorianCalendar(), curSum);
		getListView().setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(AdapterView parentView,
							View childView, int position, long id) {
						mLastOps.moveToPosition(position);
						SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
						adapter.setSelectedPosition(position);
						updateSumAtSelectedOpDisplay(mLastOps,
								getAccountCurSum());
					}

					public void onNothingSelected(AdapterView parentView) {
						((SelectedCursorAdapter) getListAdapter())
								.setSelectedPosition(-1);
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, CREATE_OP_ID, 0, R.string.new_op);
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
			deleteOp(info);
			fillData();
			return true;
		case EDIT_OP_ID:
			Intent i = new Intent(this, OperationEditor.class);
			i.putExtra(Tools.EXTRAS_OP_ID, info.id);
			i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
			startActivityForResult(i, ACTIVITY_OP_EDIT);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			updateSums(data);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (id != -1) {
			SQLiteCursor data = (SQLiteCursor) l.getItemAtPosition(position);
			SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
			adapter.setSelectedPosition(position);
			getListView().setSelectionFromTop(position, position * OFFSET);
			//setSelection(position);
			updateSumAtSelectedOpDisplay(data, getAccountCurSum());
		}
	}

	private void fillData() {
		if (mLastOps == null) {
			mLastOps = mDbHelper.fetchNLastOps(NB_LAST_OPS);
			startManagingCursor(mLastOps);
		} else {
			mLastOps.requery();
			mLastOps.moveToFirst();
		}
		Cursor opsCursor = mLastOps;
		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { OperationsDbAdapter.KEY_OP_DATE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
				OperationsDbAdapter.KEY_OP_SUM };

		// and an array of the fields we want to bind those fields to (in this
		// case just text1)
		int[] to = new int[] { R.id.op_date, R.id.op_third_party, R.id.op_sum };

		// Now create a simple cursor adapter and set it to display
		SelectedCursorAdapter operations = new SelectedCursorAdapter(this,
				R.layout.operation_row, opsCursor, from, to);
		operations.setViewBinder(new InnerViewBinder());
		setListAdapter(operations);
	}

	private void createOp() {
		Intent i = new Intent(this, OperationEditor.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
		i.putExtra(Tools.EXTRAS_OP_ID, -1l);
		startActivityForResult(i, ACTIVITY_OP_CREATE);
	}

	private void deleteOp(AdapterContextMenuInfo info) {
		Cursor c = mDbHelper.fetchOneOp(info.id);
		startManagingCursor(c);
		double sum = c.getDouble(c
				.getColumnIndex(OperationsDbAdapter.KEY_OP_SUM));
		updateSums(sum, 0.0);
		mDbHelper.deleteOp(info.id);
	}

	private double getAccountOpSum() {
		Cursor c = mCurAccount;
		c.requery();
		c.moveToFirst();
		return c.getDouble(c
				.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_OP_SUM));
	}

	private double getAccountCurSum() {
		Cursor c = mCurAccount;
		c.requery();
		c.moveToFirst();
		return c.getDouble(c
				.getColumnIndex(AccountsDbAdapter.KEY_ACCOUNT_CUR_SUM));
	}

	private double computeSumFromCursor(Cursor c) {
		double sum = 0.0;
		if (null != c && !c.isBeforeFirst() && !c.isAfterLast()) {
			boolean hasNext = true;
			while (hasNext) {
				double s = c.getDouble(c
						.getColumnIndex(OperationsDbAdapter.KEY_OP_SUM));
				sum = sum + s;
				hasNext = c.moveToPrevious();
			}
		}
		return sum;
	}

	private Cursor findLastOpBeforeDate(GregorianCalendar date) {
		Cursor ops = mLastOps;
		ops.requery();
		if (ops.moveToFirst()) {
			long dateLong = date.getTimeInMillis();
			do {
				long opDate = ops.getLong(ops
						.getColumnIndex(OperationsDbAdapter.KEY_OP_DATE));
				if (opDate <= dateLong) {
					break;
				}
			} while (ops.moveToNext());
		}
		return ops;
	}

	private void updateSums(Intent data) {
		Bundle extras = data.getExtras();
		updateSums(extras.getDouble("oldSum"), extras.getDouble("sum"));
	}

	private void updateSums(double oldSum, double sum) {
		double opSum = getAccountOpSum();
		opSum = opSum - oldSum + sum;
		if (mDbHelper.updateOpSum(mAccountId, opSum)) {
			double curSum = mDbHelper.updateCurrentSum(mAccountId);
			updateFutureSumDisplay(curSum);
			updateSumAtDateDisplay(null, curSum);
		}
	}

	private void updateFutureSumDisplay(double curSum) {
		Cursor c = mLastOps;
		c.requery();
		c.moveToFirst();
		TextView t = (TextView) findViewById(R.id.future_sum);
		if (c.isFirst()) {
			Operation latestOp = new Operation(c);
			latestOp.setSum(curSum); // to use existing formatter
			t.setText(String.format(getString(R.string.sum_at), latestOp
					.getDateStr(), latestOp.getSumStr()));
		} else {
			t.setText("");
		}
	}

	private void updateSumAtDateDisplay(GregorianCalendar date, double curSum) {
		if (null == date) {
			date = mLastSelectedDate;
			if (null == date) {
				date = new GregorianCalendar();
			}
		}
		mLastSelectedDate = date;
		Cursor c = findLastOpBeforeDate(date);
		SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
		int position = c.getPosition();
		adapter.setSelectedPosition(position);
		getListView().setSelectionFromTop(position, position * OFFSET);
		updateSumAtSelectedOpDisplay(c, getAccountCurSum());
	}

	private void updateSumAtSelectedOpDisplay(Cursor selectedOp, double curSum) {
		double sum = 0.0;
		if (selectedOp.moveToPrevious()) {
			sum = computeSumFromCursor(selectedOp);
		}
		Operation f = new Operation(); // to use formatters
		f.setSum(curSum - sum);
		TextView t = (TextView) findViewById(R.id.date_sum);
		t.setText(String.format(getString(R.string.sum_at_selection), f
				.getSumStr()));
	}

}
