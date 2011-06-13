package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.acra.ErrorReporter;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.editor.OperationEditor;
import fr.geobert.radis.editor.ScheduledOperationEditor;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.QuickAddInterface;
import fr.geobert.radis.tools.RadisListActivity;
import fr.geobert.radis.tools.Tools;

public class OperationList extends ListActivity implements RadisListActivity, QuickAddInterface {
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;
	private static final int CONVERT_OP_ID = Menu.FIRST + 3;
	private static final int EDIT_SCH_OP_ID = Menu.FIRST + 4;

	private static final int ACTIVITY_OP_CREATE = 0;
	private static final int ACTIVITY_OP_EDIT = 1;
	private static final int CREATE_SCH_OP = 2;

	private static final int DIALOG_DELETE = 0;

	private CommonDbAdapter mDbHelper;
	private Long mAccountId;
	private Cursor mCurAccount;
	private MatrixCursor mLastOps = null;
	private GregorianCalendar mLastSelectedDate;
	private ImageView mLoadingIcon;
	private AsyncTask<Void, Void, Long> mUpdateSumTask;
	private Integer mLastSelectedPosition = null;
	private boolean mOnRestore = false;
	private AdapterContextMenuInfo mOpToDelete = null;
	private OnInsertionReceiver mOnInsertionReceiver;
	private IntentFilter mOnInsertionIntentFilter;
	private QuickAddController mQuickAddController;

	private class InnerViewBinder extends OpViewBinder {

		public InnerViewBinder() {
			super(OperationList.this, CommonDbAdapter.KEY_OP_SUM,
					CommonDbAdapter.KEY_OP_DATE, R.id.op_icon);
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);
			if (colName.equals(CommonDbAdapter.KEY_TAG_NAME)) {
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

				ImageView i = (ImageView) ((LinearLayout) view.getParent()
						.getParent()).findViewById(R.id.op_sch_icon);
				if (cursor.getLong(cursor
						.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)) > 0) {
					i.setVisibility(View.VISIBLE);
				} else {
					i.setVisibility(View.INVISIBLE);
				}
				return true;
			} else {
				return super.setViewValue(view, cursor, columnIndex);
			}
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
			Cursor c = mDbHelper.fetchOpEarlierThan(params[0], 1);
			startManagingCursor(c);
			if (c.moveToFirst()) {
				GregorianCalendar opDate = new GregorianCalendar();
				opDate.setTimeInMillis(c.getLong(c
						.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
				c.close();
				Cursor result = mDbHelper.fetchOpOfMonth(opDate
						.get(Calendar.MONTH));
				startManagingCursor(result);
				mHasResult = fillLastOps(result);
			} else {
				mHasResult = false;
			}
			return null;
		}
	}

	private void getMoreOps() {
		MatrixCursor c = mLastOps;
		c.moveToLast();
		long earliestOpDate = c.getLong(c
				.getColumnIndex(CommonDbAdapter.KEY_OP_DATE));
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(earliestOpDate);
		new GetMoreOps().execute(earliestOpDate);
	}

	protected void initDbHelper() {
		mDbHelper = CommonDbAdapter.getInstance(this, mAccountId);
		assert null != mDbHelper;
		mDbHelper.open();
		mCurAccount = mDbHelper.fetchAccount(mAccountId);
		startManagingCursor(mCurAccount);
		mQuickAddController.setDbHelper(mDbHelper);	
	}

	private void initReferences() {
		mQuickAddController = new QuickAddController(this, this);
		mQuickAddController.setDbHelper(mDbHelper);
		mQuickAddController.setAccount(mAccountId);
		mOnInsertionReceiver = new OnInsertionReceiver(this);
		mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);
	}

	private void initViewBehavior() {
		
		mQuickAddController.initViewBehavior();
		final GestureDetector gestureDetector = new GestureDetector(
				new ListViewSwipeDetector(getListView(), new ListSwipeAction() {
					@Override
					public void run() {
						OperationList.this.finish();
					}
				}, new ListSwipeAction() {
					@Override
					public void run() {
						if (mRowId > 0) {
							startOperationEdit(mRowId);
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
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}
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

		initReferences();

	}

	private void updateSumsAndSelection() throws Exception {
		long curSum = getAccountCurSum();
		Cursor c = mLastOps;
		c.requery();
		c.moveToFirst();
		updateFutureSumDisplay(curSum, c);
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
		mQuickAddController.clearFocus();

	}

	@Override
	protected void onResume() {
		super.onResume();
		initDbHelper();
		initViewBehavior();
		registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
		fillData();
		mQuickAddController.setAutoNegate(true);
		try {
			updateSumsAndSelection();
			getListView().setOnItemSelectedListener(
					new AdapterView.OnItemSelectedListener() {
						public void onItemSelected(AdapterView<?> parentView,
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

						public void onNothingSelected(AdapterView<?> parentView) {
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
		inflater.inflate(R.menu.common_menu, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		if (info.id != -1) {
			super.onCreateContextMenu(menu, v, menuInfo);
			menu.add(0, EDIT_OP_ID, 0, R.string.edit);
			Cursor c = mDbHelper.fetchOneOp(info.id);
			startManagingCursor(c);
			if (c.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)) > 0) {
				menu.add(0, EDIT_SCH_OP_ID, 0, R.string.edit_scheduling);
			} else {
				menu.add(0, CONVERT_OP_ID, 0, R.string.convert_into_scheduling);
			}
			menu.add(0, DELETE_OP_ID, 0, R.string.delete);
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.create_operation:
			startCreateOp();
			return true;
		case R.id.go_to_sch_op:
			startScheduledOpsList();
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
			startOperationEdit(info.id);
			return true;
		case CONVERT_OP_ID:
			startScheduledOpEditor(info.id, true);
			return true;
		case EDIT_SCH_OP_ID:
			Cursor c = mDbHelper.fetchOneOp(info.id);
			startManagingCursor(c);
			startScheduledOpEditor(c.getLong(c
					.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)),
					false);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// mDbHelper.open();
		if (requestCode == ACTIVITY_OP_CREATE
				|| requestCode == ACTIVITY_OP_EDIT) {
			if (resultCode == RESULT_OK) {
				try {
					fillData();
					if (data.getBooleanExtra("sumUpdateNeeded", false)) {
						updateSums(data);
					}
				} catch (Exception e) {
					ErrorReporter.getInstance().handleException(e);
					Tools.popError(OperationList.this, e.getMessage(),
							Tools.createRestartClickListener());
				}
			}
		} else if (requestCode == CREATE_SCH_OP) {
			if (resultCode == RESULT_OK) {
				final long schOpId = data.getLongExtra("schOperationId", 0);
				final long opId = data.getLongExtra("opIdSource", 0);
				if (opId > 0 && schOpId > 0) {
					mDbHelper.updateOp(opId, schOpId);
				} else {
					// TODO error
				}
			}
		}
	}

	private boolean fillLastOps(Cursor c) {
		MatrixCursor lo = mLastOps;
		if (c.moveToFirst()) {
			do {
				Object[] values = { new Long(c.getLong(0)), c.getString(1),
						c.getString(2), c.getString(3), new Long(c.getLong(4)),
						new Long(c.getLong(5)), c.getString(7),
						new Long(c.getLong(8)) };
				lo.addRow(values);
			} while (c.moveToNext());
			return true;
		} else {
			return false;
		}
	}

	private void fillData() {
		mLastOps = new MatrixCursor(new String[] {
				CommonDbAdapter.KEY_OP_ROWID,
				CommonDbAdapter.KEY_THIRD_PARTY_NAME,
				CommonDbAdapter.KEY_TAG_NAME, CommonDbAdapter.KEY_MODE_NAME,
				CommonDbAdapter.KEY_OP_SUM, CommonDbAdapter.KEY_OP_DATE,
				CommonDbAdapter.KEY_OP_NOTES,
				CommonDbAdapter.KEY_OP_SCHEDULED_ID });
		startManagingCursor(mLastOps);
		Cursor lastOp = mDbHelper.fetchLastOp(mAccountId);
		GregorianCalendar latest = new GregorianCalendar();
		if (lastOp != null && lastOp.moveToFirst()) {
			latest.setTimeInMillis(lastOp.getLong(lastOp
					.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
			GregorianCalendar today = new GregorianCalendar();
			Cursor c;
			if (today.before(latest)) {
				c = mDbHelper.fetchOpBetweenMonthes(
						today.get(Calendar.MONTH), latest.get(Calendar.MONTH));
			} else {
				c = mDbHelper.fetchOpBetweenMonthes(
						latest.get(Calendar.MONTH), today.get(Calendar.MONTH));
			}
			startManagingCursor(c);
			fillLastOps(c);
		}
		if (lastOp != null) {
			lastOp.close();
		}
		MatrixCursor opsCursor = mLastOps;
		String[] from = new String[] { CommonDbAdapter.KEY_OP_DATE,
				CommonDbAdapter.KEY_THIRD_PARTY_NAME,
				CommonDbAdapter.KEY_OP_SUM, CommonDbAdapter.KEY_TAG_NAME,
				CommonDbAdapter.KEY_MODE_NAME };

		int[] to = new int[] { R.id.op_date, R.id.op_third_party, R.id.op_sum,
				R.id.op_infos };

		// Now create a simple cursor adapter and set it to display
		SelectedCursorAdapter operations = new SelectedCursorAdapter(this,
				R.layout.operation_row, opsCursor, from, to);
		operations.setViewBinder(new InnerViewBinder());
		setListAdapter(operations);
	}

	private void startCreateOp() {
		Intent i = new Intent(this, OperationEditor.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
		i.putExtra(Tools.EXTRAS_OP_ID, -1l);
		startActivityForResult(i, ACTIVITY_OP_CREATE);
	}

	private void deleteOp(AdapterContextMenuInfo info) throws Exception {
		Cursor c = mDbHelper.fetchOneOp(info.id);
		startManagingCursor(c);
		long sum = c.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
		updateSums(sum, 0L);
		mDbHelper.deleteOp(info.id);
	}

	// generic function for getAccountOpSum and getAccountCurSum
	private long getAccountSum(String col) throws Exception {
		Cursor c = mCurAccount;
		if ((!c.isClosed()) && c.requery() && c.moveToFirst()) {
			return c.getLong(c.getColumnIndexOrThrow(col));
		} else {
			mCurAccount = mDbHelper.fetchAccount(mAccountId);
			startManagingCursor(mCurAccount);
			c = mCurAccount;
			return c.getLong(c.getColumnIndex(col));
		}
	}

	private long getAccountOpSum() throws Exception {
		return getAccountSum(CommonDbAdapter.KEY_ACCOUNT_OP_SUM);
	}

	private long getAccountCurSum() throws Exception {
		return getAccountSum(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM);
	}

	private long computeSumFromCursor(Cursor c) {
		long sum = 0L;
		int i = 0;

		if (null != c && !c.isBeforeFirst() && !c.isAfterLast()) {
			boolean hasNext = c.moveToPrevious();

			while (hasNext) {
				long s = c
						.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
				sum = sum + s;
				hasNext = c.moveToPrevious();
				i++;
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
						.getColumnIndex(CommonDbAdapter.KEY_OP_DATE));
				if (opDate <= dateLong) {
					break;
				}
			} while (ops.moveToNext());
		}
		return ops;
	}

	private void updateSums(Intent data) throws Exception {
		Bundle extras = data.getExtras();
		updateSums(extras.getLong("oldSum"), extras.getLong("sum"));
	}

	private void updateSums(long oldSum, long sum) throws Exception {
		long opSum = getAccountOpSum();
		opSum = opSum - oldSum + sum;
		if (mDbHelper.updateOpSum(mAccountId, opSum)) {
			Cursor c = mLastOps;
			c.requery();
			c.moveToFirst();
			long curSum = mDbHelper.updateCurrentSum(mAccountId, c.getLong(c
					.getColumnIndexOrThrow(CommonDbAdapter.KEY_OP_DATE)));
			updateFutureSumDisplay(curSum, c);
			updateSumAtDateDisplay(null, curSum);
		}
	}

	private void updateFutureSumDisplay(long curSum, Cursor c) {
		TextView t = (TextView) findViewById(R.id.future_sum);
		if (c.isFirst()) {
			Operation latestOp = new Operation(c); // to use existing formatter
			latestOp.mSum = curSum;
			t.setText(String.format(getString(R.string.sum_at),
					latestOp.getDateStr(), latestOp.getSumStr()));
		} else {
			t.setText("");
		}
	}

	private void updateSumAtDateDisplay(GregorianCalendar date, long curSum) {
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

	private void startScheduledOpEditor(final long id, final boolean conversion) {
		Intent i = new Intent(this, ScheduledOperationEditor.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
		if (conversion) {
			i.putExtra("operationId", id);
		} else {
			i.putExtra(Tools.EXTRAS_OP_ID, id);
		}
		startActivityForResult(i, CREATE_SCH_OP);
	}

	private void startOperationEdit(long id) {
		Intent i = new Intent(this, OperationEditor.class);
		i.putExtra(Tools.EXTRAS_OP_ID, id);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
		startActivityForResult(i, ACTIVITY_OP_EDIT);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (id != -1) {
			MatrixCursor data = (MatrixCursor) getListView().getItemAtPosition(
					position);
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

	private void updateSumAtSelectedOpDisplay(Cursor data, long accountCurSum) {
		if (null != data) {
			int position = data.getPosition();
			mLastSelectedPosition = position;
			selectOpAndAdjustOffset(getListView(), position);
			TextView t = (TextView) findViewById(R.id.date_sum);
			t.setText(String
					.format(getString(R.string.sum_at_selection),
							Formater.SUM_FORMAT
									.format((accountCurSum - computeSumFromCursor(data)) / 100.0d)));
		}
	}

	@Override
	protected void onPause() {
		unregisterReceiver(mOnInsertionReceiver);
		if (null != mUpdateSumTask) {
			mUpdateSumTask.cancel(true);
		}
		// mDbHelper.close();
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mQuickAddController.onSaveInstanceState(outState);
		outState.putLong("accountId", mAccountId);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		mLastSelectedPosition = (Integer) getLastNonConfigurationInstance();
		mQuickAddController.onRestoreInstanceState(state);
		mAccountId = state.getLong("accountId");
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

	@Override
	public void updateDisplay(Intent intent) {
		Object[] accountIds = (Object[]) intent
				.getSerializableExtra("accountIds");
		for (int i = 0; i < accountIds.length; ++i) {
			if (((Long) accountIds[i]).equals(mAccountId)) {
				updateSumsDisplay();
				break;
			}
		}
	}

	@Override
	public void updateSumsDisplay() {
		fillData();
		try {
			updateSumsAndSelection();
		} catch (Exception e) {
			ErrorReporter.getInstance().handleException(e);
			e.printStackTrace();
		}
	}
}
