package fr.geobert.radis;

import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class OperationList extends ListActivity {
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;

	private static final int ACTIVITY_OP_CREATE = 0;
	private static final int ACTIVITY_OP_EDIT = 1;
	private static final int DIALOG_DELETE = 0;

	private OperationsDbAdapter mDbHelper;
	private Long mAccountId;
	private String mAccountName;
	private Cursor mCurAccount;
	private final int NB_LAST_OPS = 20;
	private MatrixCursor mLastOps = null;
	private GregorianCalendar mLastSelectedDate;
	private ImageView mLoadingIcon;
	private AsyncTask<Void, Void, Double> mUpdateSumTask;
	private Integer mLastSelectedPosition = null;
	private boolean mOnRestore = false;
	private AdapterContextMenuInfo mOpToDelete = null;
	private AutoCompleteTextView mQuickAddThirdParty;
	private EditText mQuickAddAmount;
	private Button mQuickAddButton;
	private QuickAddTextWatcher mQuickAddTextWatcher;
	private CorrectCommaWatcher mCorrectCommaWatcher;

	private class InnerViewBinder implements SimpleCursorAdapter.ViewBinder {
		private Resources res = getResources();

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);

			if (colName.equals(OperationsDbAdapter.KEY_OP_SUM)) {
				TextView textView = ((TextView) view);
				ImageView i = (ImageView) ((LinearLayout) view.getParent()
						.getParent()).findViewById(R.id.op_icon);
				double sum = cursor.getDouble(columnIndex);
				if (sum >= 0.0) {
					textView.setTextColor(res.getColor(R.color.positiveSum));
					i.setImageResource(R.drawable.arrow_up16);
				} else {
					textView.setTextColor(res.getColor(R.color.text_color));
					i.setImageResource(R.drawable.arrow_down16);
				}
				String txt = Operation.SUM_FORMAT.format(Double.valueOf(sum));
				textView.setText(txt);
				return true;
			} else if (colName.equals(OperationsDbAdapter.KEY_OP_DATE)) {
				Date date = new Date(cursor.getLong(columnIndex));
				((TextView) view).setText(Operation.SHORT_DATE_FORMAT
						.format(date));
				return true;
			} else if (colName.equals(OperationsDbAdapter.KEY_TAG_NAME)) {
				TextView textView = ((TextView) view);
				StringBuilder b = new StringBuilder();
				String s = cursor.getString(columnIndex);
				if (null != s) {
					b.append(s);
				} else {
					b.append('−');
				}
				b.append(" / ");
				s = cursor.getString(columnIndex + 1);
				if (null != s) {
					b.append(s);
				} else {
					b.append('−');
				}
				textView.setText(b.toString());
				return true;
			}
			return false;
		}
	}

	private class SelectedCursorAdapter extends SimpleCursorAdapter {
		private int selectedPos = -1;

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
				v.setBackgroundResource(R.drawable.line_selected_gradient);
			} else {
				v.setBackgroundResource(R.drawable.op_line);
			}
			return v;
		}
	}

	private class GetMoreOps extends AsyncTask<Long, Void, Void> {
		private boolean mHasResult = false;

		@Override
		protected void onPostExecute(Void result) {
			((SelectedCursorAdapter) getListAdapter()).notifyDataSetChanged();
			mLoadingIcon.clearAnimation();
			mLoadingIcon.setVisibility(View.INVISIBLE);
			if (!mHasResult) {
				Toast.makeText(OperationList.this, R.string.no_more_ops,
						Toast.LENGTH_LONG).show();
			}
		}

		@Override
		protected void onPreExecute() {
			mLoadingIcon.setVisibility(View.VISIBLE);
			mLoadingIcon.startAnimation(AnimationUtils.loadAnimation(
					OperationList.this, R.anim.rotation));
		}

		@Override
		protected Void doInBackground(Long... params) {
			Cursor result = mDbHelper
					.fetchOpEarlierThan(params[0], NB_LAST_OPS);
			startManagingCursor(result);
			mHasResult = fillLastOps(result);
			return null;
		}

	}

	private void getMoreOps() {
		MatrixCursor c = mLastOps;
		c.moveToLast();
		Long earliestOpDate = c.getLong(c
				.getColumnIndex(OperationsDbAdapter.KEY_OP_DATE));
		new GetMoreOps().execute(earliestOpDate);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.operation_list);
		registerForContextMenu(getListView());

		LayoutInflater inf = getLayoutInflater();
		View footer = inf
				.inflate(R.layout.op_list_footer, getListView(), false);
		mLoadingIcon = (ImageView) footer.findViewById(R.id.loading_icon);
		getListView().addFooterView(footer);

		Bundle extras = getIntent().getExtras();
		mAccountId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID)
				: null;
		mAccountName = extras != null ? extras
				.getString(CommonDbAdapter.KEY_ACCOUNT_NAME) : null;
		setTitle(getString(R.string.app_name) + " - " + mAccountName);
		mDbHelper = new OperationsDbAdapter(this, mAccountId);
		mDbHelper.open();
		mCurAccount = mDbHelper.fetchAccount(mAccountId);
		startManagingCursor(mCurAccount);
		mQuickAddThirdParty = (AutoCompleteTextView) findViewById(R.id.quickadd_third_party);
		mQuickAddThirdParty.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME));
		mQuickAddAmount = (EditText) findViewById(R.id.quickadd_amount);
		mCorrectCommaWatcher = new CorrectCommaWatcher(Operation.SUM_FORMAT
				.getDecimalFormatSymbols().getDecimalSeparator(),
				mQuickAddAmount).setAutoNegate(true);
		mQuickAddAmount.addTextChangedListener(mCorrectCommaWatcher);
		mQuickAddButton = (Button) findViewById(R.id.quickadd_validate);
		mQuickAddButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				quickAddOp();
			}
		});
		OperationList.setQuickAddButEnabled(mQuickAddButton, false);
		mQuickAddTextWatcher = new QuickAddTextWatcher(mQuickAddThirdParty,
				mQuickAddAmount, mQuickAddButton);
		mQuickAddThirdParty.addTextChangedListener(mQuickAddTextWatcher);
		mQuickAddAmount.addTextChangedListener(mQuickAddTextWatcher);
	}

	public static void setQuickAddButEnabled(Button but, boolean b) {
		but.setEnabled(b);
		int drawable;
		if (b) {
			drawable = R.drawable.btn_check_buttonless_on;
		} else {
			drawable = R.drawable.btn_check_buttonless_off;
		}
		but.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
	}

	private void quickAddOp() {
		Operation op = new Operation();
		op.mThirdParty = mQuickAddThirdParty.getText().toString();
		try {
			op.setSumStr(mQuickAddAmount.getText().toString());
			mDbHelper.createOp(op);
			updateSums(0, op.mSum);
			mQuickAddAmount.setText("");
			mQuickAddThirdParty.setText("");
			InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(mQuickAddAmount.getWindowToken(), 0);
			mCorrectCommaWatcher.setAutoNegate(true);
			mQuickAddAmount.clearFocus();
			mQuickAddThirdParty.clearFocus();
			fillData();
			updateSumsAndSelection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateSumsAndSelection() throws Exception {
		double curSum = getAccountCurSum();
		updateFutureSumDisplay(curSum);
		if (mLastSelectedPosition == null) {
			updateSumAtDateDisplay(new GregorianCalendar(), curSum);
		} else {
			int position = mLastSelectedPosition.intValue();
			MatrixCursor data = (MatrixCursor) getListView().getItemAtPosition(
					position);
			updateSumAtSelectedOpDisplay(data, curSum);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		mQuickAddThirdParty.clearFocus();

	}

	@Override
	protected void onResume() {
		super.onResume();
		fillData();
		mCorrectCommaWatcher.setAutoNegate(true);
		try {
			updateSumsAndSelection();
			getListView().setOnItemSelectedListener(
					new AdapterView.OnItemSelectedListener() {
						public void onItemSelected(AdapterView parentView,
								View childView, int position, long id) {
							mLastOps.moveToPosition(position);
							SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
							adapter.setSelectedPosition(position);
							try {
								updateSumAtSelectedOpDisplay(mLastOps,
										getAccountCurSum());
							} catch (Exception e) {
								Tools.popError(OperationList.this,
										e.getMessage(),
										Tools.createRestartClickListener());
							}
						}

						public void onNothingSelected(AdapterView parentView) {
							((SelectedCursorAdapter) getListAdapter())
									.setSelectedPosition(-1);
						}
					});
		} catch (Exception e) {
			Tools.popError(OperationList.this, e.getMessage(),
					Tools.createRestartClickListener());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.operations_list_menu, menu);
		inflater.inflate(R.menu.advanced_actions, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (((AdapterContextMenuInfo) menuInfo).id != -1) {
			super.onCreateContextMenu(menu, v, menuInfo);
			menu.add(0, EDIT_OP_ID, 0, R.string.edit);
			menu.add(0, DELETE_OP_ID, 0, R.string.delete);
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.create_operation:
			createOp();
			return true;
		default:
			if (Tools.onDefaultMenuSelected(this, featureId, item)) {
				return true;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE_OP_ID:
			showDialog(DIALOG_DELETE);
			mOpToDelete = info;
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
			try {
				updateSums(data);
			} catch (Exception e) {
				Tools.popError(OperationList.this, e.getMessage(),
						Tools.createRestartClickListener());
			}
		}
	}

	private boolean fillLastOps(Cursor c) {
		MatrixCursor lo = mLastOps;
		if (c.moveToFirst()) {
			do {
				Object[] values = { new Long(c.getLong(0)), c.getString(1),
						c.getString(2), c.getString(3),
						new Double(c.getDouble(4)), new Long(c.getLong(5)),
						c.getString(6) };
				lo.addRow(values);
			} while (c.moveToNext());
			return true;
		} else {
			return false;
		}
	}

	private void fillData() {
		mLastOps = new MatrixCursor(new String[] {
				OperationsDbAdapter.KEY_OP_ROWID,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
				OperationsDbAdapter.KEY_TAG_NAME,
				OperationsDbAdapter.KEY_MODE_NAME,
				OperationsDbAdapter.KEY_OP_SUM,
				OperationsDbAdapter.KEY_OP_DATE,
				OperationsDbAdapter.KEY_OP_NOTES });
		startManagingCursor(mLastOps);
		Cursor c = mDbHelper.fetchNLastOps(NB_LAST_OPS);
		startManagingCursor(c);
		fillLastOps(c);

		MatrixCursor opsCursor = mLastOps;
		String[] from = new String[] { OperationsDbAdapter.KEY_OP_DATE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
				OperationsDbAdapter.KEY_OP_SUM,
				OperationsDbAdapter.KEY_TAG_NAME,
				OperationsDbAdapter.KEY_MODE_NAME };

		int[] to = new int[] { R.id.op_date, R.id.op_third_party, R.id.op_sum,
				R.id.op_infos };

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

	private void deleteOp(AdapterContextMenuInfo info) throws Exception {
		Cursor c = mDbHelper.fetchOneOp(info.id);
		startManagingCursor(c);
		double sum = c.getDouble(c
				.getColumnIndex(OperationsDbAdapter.KEY_OP_SUM));
		updateSums(sum, 0.0);
		mDbHelper.deleteOp(info.id);
	}

	// generic function for getAccountOpSum and getAccountCurSum
	private double getAccountSum(String col) throws Exception {
		Cursor c = mCurAccount;
		if (c.isClosed()) {
			mCurAccount = mDbHelper.fetchAccount(mAccountId);
			startManagingCursor(mCurAccount);
			c = mCurAccount;
			return c.getDouble(c.getColumnIndex(col));
		} else {
			if (c.requery() && c.moveToFirst()) {
				return c.getDouble(c.getColumnIndexOrThrow(col));
			} else {
				throw new Exception("Error while requery");
			}
		}
	}

	private double getAccountOpSum() throws Exception {
		return getAccountSum(CommonDbAdapter.KEY_ACCOUNT_OP_SUM);
	}

	private double getAccountCurSum() throws Exception {
		return getAccountSum(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM);
	}

	private double computeSumFromCursor(Cursor c) {
		double sum = 0.0;
		if (null != c && !c.isBeforeFirst() && !c.isAfterLast()) {
			boolean hasNext = c.moveToPrevious();
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

	private void updateSums(Intent data) throws Exception {
		Bundle extras = data.getExtras();
		updateSums(extras.getDouble("oldSum"), extras.getDouble("sum"));
	}

	private void updateSums(double oldSum, double sum) throws Exception {
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
			latestOp.mSum = curSum; // to use existing formatter
			t.setText(String.format(getString(R.string.sum_at),
					latestOp.getDateStr(), latestOp.getSumStr()));
		} else {
			t.setText("");
		}
	}

	private void updateSumAtDateDisplay(GregorianCalendar date, double curSum) {
		// TODO maybe no need date param as we use this function only with
		// current time
		if (null == date) {
			date = mLastSelectedDate;
			if (null == date) {
				date = new GregorianCalendar();
			}
		}
		mLastSelectedDate = date;
		updateSumAtSelectedOpDisplay(findLastOpBeforeDate(date), curSum);
	}

	private void selectOpAndAdjustOffset(ListView l, int position) {
		// Get the top position from the first visible element
		int firstIdx = l.getFirstVisiblePosition();
		int offset = 77;
		int firstOffset = offset;
		int count = l.getChildCount();
		count = count == 0 ? 1 : count;
		int relativeFirstIdx = firstIdx % count;
		View firstView = l.getChildAt(relativeFirstIdx);
		if (null != firstView) {
			offset = firstView.getHeight();
			firstOffset = firstView.getBottom() - (relativeFirstIdx * offset)
					- relativeFirstIdx; // getBottom = px according to virtual
			// ListView (not only what we see on
			// screen), - (firstIdx * offset) =
			// remove
			// all the previous items height, -
			// firstIdx
			// = remove the separator height
		}

		int relativePos = position - firstIdx;
		SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
		adapter.setSelectedPosition(position);
		l.setSelectionFromTop(position, mOnRestore ? 0
				: ((relativePos - 1) * offset) + firstOffset + relativePos);
		mOnRestore = false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (id != -1) {
			MatrixCursor data = (MatrixCursor) l.getItemAtPosition(position);
			try {
				updateSumAtSelectedOpDisplay(data, getAccountCurSum());
			} catch (Exception e) {
				Tools.popError(OperationList.this, e.getMessage(),
						Tools.createRestartClickListener());
			}
		} else { // get more ops is clicked
			getMoreOps();
		}
	}

	private void updateSumAtSelectedOpDisplay(Cursor data, double accountCurSum) {
		if (null != data) {
			int position = data.getPosition();
			mLastSelectedPosition = position;
			selectOpAndAdjustOffset(getListView(), position);
			TextView t = (TextView) findViewById(R.id.date_sum);
			t.setText(String.format(
					getString(R.string.sum_at_selection),
					Operation.SUM_FORMAT.format(accountCurSum
							- computeSumFromCursor(data))));
		}
	}

	@Override
	protected void onPause() {
		if (null != mUpdateSumTask) {
			mUpdateSumTask.cancel(true);
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequence("third_party", mQuickAddThirdParty.getText());
		outState.putCharSequence("amount", mQuickAddAmount.getText());
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		mLastSelectedPosition = (Integer) getLastNonConfigurationInstance();
		mQuickAddThirdParty.setText(state.getCharSequence("third_party"));
		mQuickAddAmount.setText(state.getCharSequence("amount"));
		mOnRestore = true;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mLastSelectedPosition;
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
							try {
								deleteOp(mOpToDelete);
								fillData();
								mOpToDelete = null;
							} catch (Exception e) {
								Tools.popError(OperationList.this,
										e.getMessage(),
										Tools.createRestartClickListener());
							}

						}
					});
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
		}
	}
}
