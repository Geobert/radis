package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.editor.OperationEditor;
import fr.geobert.radis.editor.ScheduledOperationEditor;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.ProjectionDateController;
import fr.geobert.radis.tools.QuickAddController;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public class OperationList extends ListActivity implements
		UpdateDisplayInterface {
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;
	private static final int CONVERT_OP_ID = Menu.FIRST + 3;
	private static final int EDIT_SCH_OP_ID = Menu.FIRST + 4;

	private static final int ACTIVITY_OP_CREATE = 0;
	private static final int ACTIVITY_OP_EDIT = 1;
	private static final int CREATE_SCH_OP = 2;

	private static final int DIALOG_DELETE = 0;
	private static final int DIALOG_PROJECTION = 1;
	private static final int DIALOG_DELETE_OCCURENCE = 2;

	private CommonDbAdapter mDbHelper;
	private Long mAccountId;
	private Cursor mCurAccount;
	private MatrixCursor mLastOps = null;
	private ImageView mLoadingIcon;
	private AsyncTask<Void, Void, Long> mUpdateSumTask;
	private Integer mLastSelectedPosition = null;
	private boolean mOnRestore = false;
	private long mOpToDelete = -1;
	private OnInsertionReceiver mOnInsertionReceiver;
	private IntentFilter mOnInsertionIntentFilter;
	private QuickAddController mQuickAddController;
	private Button mProjectionBtn;
	private long mProjectionDate;
	private int mNbGetMoreOps;
	private int mLastSelectionFromTop; // last pos from top of ListView
	private boolean receiverIsRegistered;
	private SelectedCursorAdapter mOpListCursorAdapter;

	private static class OpRowHolder {
		public TextView separator;
		public ImageView scheduledImg;
		public StringBuilder tagBuilder = new StringBuilder();
	}

	// used in InnerViewBinder
	private static GregorianCalendar date1 = new GregorianCalendar();
	private static GregorianCalendar date2 = new GregorianCalendar();

	private class InnerViewBinder extends OpViewBinder {
		/**
		 * State of ListView item that has never been determined.
		 */
		private static final int STATE_UNKNOWN = 0;

		/**
		 * State of a ListView item that is sectioned. A sectioned item must
		 * display the separator.
		 */
		private static final int STATE_SECTIONED_CELL = 1;

		/**
		 * State of a ListView item that is not sectioned and therefore does not
		 * display the separator.
		 */
		private static final int STATE_REGULAR_CELL = 2;

		private int[] mCellStates = null;

		public InnerViewBinder(Cursor c) {
			super(OperationList.this, CommonDbAdapter.KEY_OP_SUM,
					CommonDbAdapter.KEY_OP_DATE, R.id.op_icon);
			initCache(c);
		}

		public void initCache(Cursor cursor) {
			Log.d("Radis", "initCache");
			mCellStates = cursor == null ? null : new int[cursor.getCount()];
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);
			if (colName.equals(CommonDbAdapter.KEY_TAG_NAME)) {
				final OpRowHolder h = (OpRowHolder) ((View) view.getParent()
						.getParent()).getTag();
				TextView textView = ((TextView) view);
				StringBuilder b = h.tagBuilder;
				b.setLength(0);
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

				ImageView i = h.scheduledImg;
				if (cursor.getLong(cursor
						.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)) > 0) {
					i.setVisibility(View.VISIBLE);
				} else {
					i.setVisibility(View.INVISIBLE);
				}

				boolean needSeparator = false;
				final int position = cursor.getPosition();
				assert (mCellStates != null);
				date1.setTimeInMillis(cursor.getLong(cursor
						.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
				switch (mCellStates[position]) {
				case STATE_SECTIONED_CELL:
					needSeparator = true;
					break;

				case STATE_REGULAR_CELL:
					needSeparator = false;
					break;

				case STATE_UNKNOWN:
				default:
					// A separator is needed if it's the first itemview of the
					// ListView or if the group of the current cell is different
					// from the previous itemview.
					if (position == 0) {
						needSeparator = true;
					} else {
						cursor.moveToPosition(position - 1);
						date2.setTimeInMillis(cursor.getLong(cursor
								.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
						if (date1.get(GregorianCalendar.MONTH) != date2
								.get(GregorianCalendar.MONTH)) {
							needSeparator = true;
						}
						cursor.moveToPosition(position);
					}
				}
				TextView separator = h.separator;
				if (needSeparator) {
					separator.setText(DateFormat.format("MMMM", date1));
					separator.setVisibility(View.VISIBLE);
				} else {
					separator.setVisibility(View.GONE);
				}
				return true;
			} else {
				return super.setViewValue(view, cursor, columnIndex);
			}
		}

		public void increaseCache(MatrixCursor c) {
			Log.d("Radis", "increaseCache");
			int[] tmp = mCellStates;
			initCache(c);
			for (int i = 0; i < tmp.length; ++i) {
				mCellStates[i] = tmp[i];
			}
		}
	}

	private class SelectedCursorAdapter extends SimpleCursorAdapter {
		private int selectedPos = -1;

		SelectedCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			InnerViewBinder viewBinder = new InnerViewBinder(c);
			setViewBinder(viewBinder);
		}

		public void setSelectedPosition(int pos) {
			selectedPos = pos;
			// inform the view of this change
			notifyDataSetChanged();
		}

		@Override
		public void changeCursor(Cursor c) {
			super.changeCursor(c);
			((InnerViewBinder) getViewBinder()).initCache(c);
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

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			OpRowHolder h = new OpRowHolder();
			h.separator = (TextView) v.findViewById(R.id.separator);
			h.scheduledImg = (ImageView) v.findViewById(R.id.op_sch_icon);
			v.setTag(h);
			return v;
		}
	}

	private class GetMoreOps extends AsyncTask<Long, Void, Void> {
		private boolean mHasResult = false;
		private boolean mIsRestoring;
		private int mEffectiveRetrieval;
		private MatrixCursor mAccumulator;
		private Cursor mLops;

		public GetMoreOps(final boolean isRestoring) {
			mIsRestoring = isRestoring;
			mEffectiveRetrieval = 0;
			mAccumulator = new MatrixCursor(new String[] {
					CommonDbAdapter.KEY_OP_ROWID,
					CommonDbAdapter.KEY_THIRD_PARTY_NAME,
					CommonDbAdapter.KEY_TAG_NAME,
					CommonDbAdapter.KEY_MODE_NAME, CommonDbAdapter.KEY_OP_SUM,
					CommonDbAdapter.KEY_OP_DATE, CommonDbAdapter.KEY_OP_NOTES,
					CommonDbAdapter.KEY_OP_SCHEDULED_ID,
					CommonDbAdapter.KEY_OP_TRANSFERT_ACC_ID });
		}

		@Override
		protected void onPostExecute(Void result) {
			fillMatrixCursor(mLastOps, mAccumulator);
			((InnerViewBinder) ((SelectedCursorAdapter) getListAdapter())
					.getViewBinder()).increaseCache(mLastOps);
			((SelectedCursorAdapter) getListAdapter()).notifyDataSetChanged();
			mAccumulator.close();
			mLoadingIcon.clearAnimation();
			mLoadingIcon.setVisibility(View.INVISIBLE);
			if (!mHasResult) {
				if (!mIsRestoring) {
					Toast.makeText(OperationList.this, R.string.no_more_ops,
							Toast.LENGTH_LONG).show();
				} else {
					OperationList.this.mNbGetMoreOps = this.mEffectiveRetrieval;
				}
			} else {
				if (mIsRestoring) {
					updateSumsAndSelection();
				} else {
					OperationList.this.mNbGetMoreOps++;
				}
			}
			Log.d("Radis",
					"post getMoreOps mLastOps.getCount() : "
							+ mLastOps.getCount());
		}

		@Override
		protected void onPreExecute() {
			mLoadingIcon.setVisibility(View.VISIBLE);
			mLoadingIcon.startAnimation(AnimationUtils.loadAnimation(
					OperationList.this, R.anim.rotation));
			mLops = mLastOps;
		}

		private void getOps() {
			Cursor c = mLops;
			boolean moveToLast = c.moveToLast();
			if (!moveToLast || c.isBeforeFirst() || c.isAfterLast()) {
				mNbGetMoreOps = 0;
			} else {
				try {
					long earliestOpDate = c.getLong(c
							.getColumnIndex(CommonDbAdapter.KEY_OP_DATE));
					Log.d("Radis", "earliestOpDate : "
							+ Formater.getFullDateFormater(OperationList.this)
									.format(earliestOpDate));
					c = mDbHelper.fetchOpEarlierThan(earliestOpDate, 1,
							mAccountId);
					startManagingCursor(c);
					if (c.moveToFirst() && c.isFirst()) {
						GregorianCalendar opDate = new GregorianCalendar();
						opDate.setTimeInMillis(c.getLong(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
						c.close();
						Log.d("Radis",
								"opDate : "
										+ Formater.getFullDateFormater(
												OperationList.this).format(
												opDate.getTimeInMillis()));
						Cursor result = mDbHelper.fetchOpOfMonth(opDate,
								earliestOpDate, mAccountId);
						startManagingCursor(result);
						mHasResult = fillMatrixCursor(mAccumulator, result);
						mLops = mAccumulator;
					} else {
						mHasResult = false;
					}
				} catch (CursorIndexOutOfBoundsException e) {
					mNbGetMoreOps = 0;
					ErrorReporter.getInstance().putCustomData(
							"mLops.getCount()",
							Integer.toBinaryString(c.getCount()));
					ErrorReporter.getInstance().putCustomData(
							"c.isBeforeFirst()",
							String.valueOf(c.isBeforeFirst()));
					ErrorReporter.getInstance().putCustomData(
							"c.isAfterLast()", String.valueOf(c.isAfterLast()));
					ErrorReporter.getInstance().putCustomData("moveToLast",
							String.valueOf(moveToLast));
					ErrorReporter.getInstance().handleSilentException(e);

				}
			}
		}

		@Override
		protected Void doInBackground(Long... params) {
			if (mIsRestoring) {
				int i = mNbGetMoreOps;
				while (i > 0) {
					getOps();
					if (mHasResult) {
						mEffectiveRetrieval++;
					}
					--i;
				}
			} else {
				getOps();
			}
			return null;
		}
	}

	private void getMoreOps(final boolean isRestoring) {
		new GetMoreOps(isRestoring).execute();
	}

	protected void initDbHelper() {
		mDbHelper = CommonDbAdapter.getInstance(this);
		mCurAccount = mDbHelper.fetchAccount(mAccountId);
		startManagingCursor(mCurAccount);
		if (mCurAccount.moveToFirst()) {
			mProjectionDate = mCurAccount.getLong(mCurAccount
					.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE));
		}
		mQuickAddController.setDbHelper(mDbHelper);
	}

	private void initReferences() {
		mQuickAddController = new QuickAddController(this, this);
		mQuickAddController.setDbHelper(mDbHelper);
		mQuickAddController.setAccount(mAccountId);
		receiverIsRegistered = false;
		mOnInsertionReceiver = new OnInsertionReceiver(this);
		mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);
		mProjectionBtn = (Button) findViewById(R.id.future_sum);
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
		mProjectionBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OperationList.this.showDialog(DIALOG_PROJECTION);
			}
		});
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

		initReferences();
		mNbGetMoreOps = 1;
		mLastSelectedPosition = null;
		mLastSelectionFromTop = 0;
	}

	@Override
	protected void onStart() {
		super.onStart();
		mQuickAddController.clearFocus();

	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean hideQuickAdd = DBPrefsManager.getInstance(this).getBoolean(
				RadisConfiguration.KEY_HIDE_OPS_QUICK_ADD);
		int visibility = View.VISIBLE;
		if (hideQuickAdd) {
			visibility = View.GONE;
		}

		mQuickAddController.setVisibility(visibility);

		initDbHelper();
		initViewBehavior();
		if (!receiverIsRegistered) {
			receiverIsRegistered = true;
			registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
		}
		fillData();

		if (mNbGetMoreOps > 0) {
			getMoreOps(true);
		}
		mQuickAddController.setAutoNegate(true);
		updateSumsAndSelection();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.operations_list_menu, menu);
		inflater.inflate(R.menu.common_menu, menu);
		menu.findItem(R.id.recompute_account).setVisible(true);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		if (info.id != -1) {
			super.onCreateContextMenu(menu, v, menuInfo);
			menu.add(0, EDIT_OP_ID, 0, R.string.edit);
			Cursor c = mDbHelper.fetchOneOp(info.id, mAccountId);
			startManagingCursor(c);
			if (!c.isBeforeFirst() && !c.isAfterLast()) {
				if (c.getLong(c
						.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)) > 0) {
					menu.add(0, EDIT_SCH_OP_ID, 0, R.string.edit_scheduling);
				} else {
					menu.add(0, CONVERT_OP_ID, 0,
							R.string.convert_into_scheduling);
				}
				menu.add(0, DELETE_OP_ID, 0, R.string.delete);
			}
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
		case R.id.recompute_account:
			mDbHelper.consolidateSums(mAccountId);
			updateSumsDisplay(true);
			return true;
		default:
			if (Tools.onDefaultMenuSelected(this, featureId, item)) {
				return true;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void startScheduledOpsList() {
		ScheduledOpList.callMe(this, mAccountId);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE_OP_ID: {
			Cursor c = mDbHelper.fetchOneOp(info.id, mAccountId);
			startManagingCursor(c);
			if (!c.isBeforeFirst()
					&& !c.isAfterLast()
					&& c.getLong(c
							.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)) > 0) {
				showDialog(DIALOG_DELETE_OCCURENCE);
			} else {
				showDialog(DIALOG_DELETE);
			}
			mOpToDelete = info.id;
		}
			return true;
		case EDIT_OP_ID:
			startOperationEdit(info.id);
			return true;
		case CONVERT_OP_ID:
			startScheduledOpEditor(info.id, true);
			return true;
		case EDIT_SCH_OP_ID: {
			Cursor c = mDbHelper.fetchOneOp(info.id, mAccountId);
			startManagingCursor(c);
			startScheduledOpEditor(c.getLong(c
					.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID)),
					false);
		}
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void afterAddOp(Intent data) {
		long newRowId = data.getLongExtra("newRowId", -1);
		Log.d("Radis", "afterAddOp newRowId: " + newRowId);
		if (newRowId > -1) {
			ListAdapter adap = getListAdapter();
			int position = 0;
			for (; position < adap.getCount(); ++position) {
				if (newRowId == adap.getItemId(position)) {
					break;
				}
			}
			if (null == mLastSelectedPosition) {
				Log.d("Radis", "mLastSelectedPosition INIT");
				mLastSelectedPosition = 0;
			}
			Log.d("Radis", "mLastSelectedPosition: " + mLastSelectedPosition);
			Log.d("Radis", "position: " + position);
			Log.d("Radis", "mLastOps.getCount(): " + mLastOps.getCount());
			if (position <= mLastSelectedPosition
					&& mLastSelectedPosition < mLastOps.getCount() - 1) {
				Log.d("Radis", "mLastSelectedPosition is incremeted");
				mLastSelectedPosition++;
			}
		}
		if (mLastSelectedPosition >= mLastOps.getCount()) {
			mLastSelectedPosition = mLastOps.getCount() - 1;
		}
		if (data.getBooleanExtra("sumUpdateNeeded", false)) {
			Bundle extras = data.getExtras();
			updateSumsAfterOpEdit(extras.getLong("oldSum"),
					extras.getLong("sum"), extras.getLong("opDate"));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// mDbHelper.open();
		if (requestCode == ACTIVITY_OP_CREATE
				|| requestCode == ACTIVITY_OP_EDIT) {
			if (resultCode == RESULT_OK) {
				updateSumsDisplay(requestCode == ACTIVITY_OP_CREATE);
				afterAddOp(data);
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

	private boolean fillMatrixCursor(MatrixCursor m, Cursor c) {
		if (!c.isClosed() && c.moveToFirst() && c.isFirst()) {
			do {
				Object[] values = {
						Long.valueOf(c.getLong(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_ROWID))),
						c.getString(c
								.getColumnIndex(CommonDbAdapter.KEY_THIRD_PARTY_NAME)),
						c.getString(c
								.getColumnIndex(CommonDbAdapter.KEY_TAG_NAME)),
						c.getString(c
								.getColumnIndex(CommonDbAdapter.KEY_MODE_NAME)),
						Long.valueOf(c.getLong(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_SUM))),
						Long.valueOf(c.getLong(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_DATE))),
						c.getString(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_NOTES)),
						Long.valueOf(c.getLong(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID))),
						Long.valueOf(c.getLong(c
								.getColumnIndex(CommonDbAdapter.KEY_OP_TRANSFERT_ACC_ID))) };
				m.addRow(values);
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
				CommonDbAdapter.KEY_OP_SCHEDULED_ID,
				CommonDbAdapter.KEY_OP_TRANSFERT_ACC_ID });
		startManagingCursor(mLastOps);
		Cursor lastOp = mDbHelper.fetchLastOp(mAccountId);
		GregorianCalendar latest = new GregorianCalendar();
		Tools.clearTimeOfCalendar(latest);
		if (lastOp != null) {
			if (lastOp.moveToFirst()) {
				latest.setTimeInMillis(lastOp.getLong(lastOp
						.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
				GregorianCalendar currentMonth = new GregorianCalendar();
				Tools.clearTimeOfCalendar(currentMonth);
				currentMonth.set(Calendar.DAY_OF_MONTH,
						currentMonth.getActualMinimum(Calendar.DAY_OF_MONTH));
				Cursor c = mDbHelper.fetchOpBetweenDate(currentMonth, latest,
						mAccountId);
				startManagingCursor(c);
				fillMatrixCursor(mLastOps, c);
			}
			lastOp.close();
		}
		String[] from = new String[] { CommonDbAdapter.KEY_OP_DATE,
				CommonDbAdapter.KEY_THIRD_PARTY_NAME,
				CommonDbAdapter.KEY_OP_SUM, CommonDbAdapter.KEY_TAG_NAME,
				CommonDbAdapter.KEY_MODE_NAME };

		int[] to = new int[] { R.id.op_date, R.id.op_third_party, R.id.op_sum,
				R.id.op_infos };

		// Now create a simple cursor adapter and set it to display
		if (mOpListCursorAdapter == null) {
			mOpListCursorAdapter = new SelectedCursorAdapter(this,
					R.layout.operation_row, mLastOps, from, to);
			setListAdapter(mOpListCursorAdapter);
		} else {
			mOpListCursorAdapter.changeCursor(mLastOps);
		}
	}

	private void startCreateOp() {
		Intent i = new Intent(this, OperationEditor.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
		i.putExtra(Tools.EXTRAS_OP_ID, -1l);
		startActivityForResult(i, ACTIVITY_OP_CREATE);
	}

	private void deleteOp(long opToDelId) {
		Cursor c = mDbHelper.fetchOneOp(opToDelId, mAccountId);
		startManagingCursor(c);
		long sum = c.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
		updateLastSelectionIdxFromTop();
		if (mDbHelper.deleteOp(opToDelId, mAccountId)) {
			if (c.getPosition() < mLastSelectedPosition) {
				mLastSelectedPosition--;
			}
			fillData();
			updateSumsAfterOpEdit(sum, 0L, 0);
		} else {
			updateSumsDisplay(true);
			// fillData() is called inside updateSumsDisplay
		}
	}

	private void deleteOpAndFollowing(long opToDelId) {
		Cursor c = mDbHelper.fetchOneOp(opToDelId, mAccountId);
		startManagingCursor(c);
		long sum = c.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
		updateLastSelectionIdxFromTop();
		final long schOpId = c.getLong(c
				.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID));
		final long date = c.getLong(c
				.getColumnIndex(CommonDbAdapter.KEY_OP_DATE));
		int nbDeleted = mDbHelper.deleteAllFutureOccurrences(mAccountId,
				schOpId, date);
		if (nbDeleted > 0) {
			if (c.getPosition() < mLastSelectedPosition) {
				mLastSelectedPosition--;
			}
			fillData();
			updateSumsAfterOpEdit(sum * nbDeleted, 0L, 0);
		} else {
			updateSumsDisplay(true);
			// fillData() is called inside updateSumsDisplay
		}
	}

	private void deleteAllOccurences(long opToDelId) {
		Cursor c = mDbHelper.fetchOneOp(opToDelId, mAccountId);
		startManagingCursor(c);
		final long sum = c
				.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
		updateLastSelectionIdxFromTop();
		final long schOpId = c.getLong(c
				.getColumnIndex(CommonDbAdapter.KEY_OP_SCHEDULED_ID));
		if (mDbHelper.deleteScheduledOp(schOpId)) {
			final int nbDeleted = mDbHelper.deleteAllOccurrences(mAccountId,
					schOpId);
			if (nbDeleted > 0) {
				if (c.getPosition() < mLastSelectedPosition) {
					mLastSelectedPosition--;
				}
				fillData();
				updateSumsAfterOpEdit(sum * nbDeleted, 0L, 0);
			} else {
				updateSumsDisplay(true);
				// fillData() is called inside updateSumsDisplay
			}
		}
	}

	// generic function for getAccountOpSum and getAccountCurSum
	private long getAccountSum(String col) {
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

	private long getAccountCurSum() {
		return getAccountSum(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM);
	}

	private long computeSumFromCursor(Cursor op) {
		long sum = 0L;
		final int dateIdx = op.getColumnIndex(CommonDbAdapter.KEY_OP_DATE);
		final int opSumIdx = op.getColumnIndex(CommonDbAdapter.KEY_OP_SUM);
		long opDate = op.getLong(dateIdx);
		if (null != op && !op.isBeforeFirst() && !op.isAfterLast()) {
			if (opDate <= mProjectionDate) {
				boolean hasPrev = op.moveToPrevious();
				// Log.d("Radis",
				// "computeSumFromCursor op is <= projDate, hasPrev : " +
				// hasPrev);

				if (hasPrev) {
					opDate = op.getLong(dateIdx);
					// Log.d("Radis", "computeSumFromCursor opDate : " +
					// Tools.getDateStr(opDate)
					// + " / projDate : " + Tools.getDateStr(mProjectionDate));
					while (hasPrev && opDate <= mProjectionDate) {
						long s = op.getLong(opSumIdx);
						// Log.d("Radis", "computeSumFromCursor add " + s);
						sum = sum + s;
						hasPrev = op.moveToPrevious();
						if (hasPrev) {
							opDate = op.getLong(dateIdx);
						}
					}
					sum = -sum;
				}
			} else {
				sum = op.getLong(opSumIdx);
				boolean hasNext = op.moveToNext();
				// Log.d("Radis",
				// "computeSumFromCursor op is > projDate, hasNext : " +
				// hasNext);
				if (hasNext) {
					opDate = op.getLong(dateIdx);
					// Log.d("Radis", "computeSumFromCursor opDate : " +
					// Tools.getDateStr(opDate)
					// + " / projDate : " + Tools.getDateStr(mProjectionDate));
					while (hasNext && opDate > mProjectionDate) {
						long s = op.getLong(opSumIdx);
						// Log.d("Radis", "computeSumFromCursor add " + s);
						sum = sum + s;
						hasNext = op.moveToNext();
						if (hasNext) {
							opDate = op.getLong(dateIdx);
						}
					}
				}
			}
		}
		Log.d("Radis", "computeSumFromCursor after sum = " + sum);
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

	private void selectOpAndAdjustOffset(ListView l, int position) {
		// Get the top position from the first visible element
		final int firstIdx = l.getFirstVisiblePosition();
		final int lastIdx = l.getLastVisiblePosition();

		int offset = 77;
		int firstOffset = offset;
		int count = l.getChildCount();
		count = count == 0 ? 1 : count;
		int relativeFirstIdx = firstIdx % count;
		View firstView = l.getChildAt(relativeFirstIdx);
		if (null != firstView) {
			offset = firstView.getHeight();
			firstOffset = firstView.getBottom() - (relativeFirstIdx * offset)
					- relativeFirstIdx;
			// getBottom = px according to virtual ListView (not only what we
			// see on
			// screen), - (firstIdx * offset) = remove all the previous items
			// height, -
			// firstIdx = remove the separator height
		}

		int relativePos = position - firstIdx;
		SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
		adapter.setSelectedPosition(position);

		// check if the selected pos is visible on screen
		int posFromTop;
		if ((position >= firstIdx) && (position < lastIdx)) {
			posFromTop = mOnRestore ? mLastSelectionFromTop
					: ((relativePos - 1) * offset) + firstOffset + relativePos;
		} else {
			posFromTop = mLastSelectionFromTop != 0 ? mLastSelectionFromTop
					: (int) (Tools.SCREEN_HEIGHT * 0.3);
		}
		l.setSelectionFromTop(position, posFromTop);
		mOnRestore = false;
		Log.d("Radis",
				"selectOpAndAdjustOffset setting mLastSelectedPosition: "
						+ position);
		mLastSelectedPosition = Integer.valueOf(position);
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
		Log.d("Radis", "onListItemClick ");
		super.onListItemClick(l, v, position, id);
		if (id != -1) {
			mLastOps.moveToPosition(position);
			SelectedCursorAdapter adapter = (SelectedCursorAdapter) getListAdapter();
			adapter.setSelectedPosition(position);
			MatrixCursor data = (MatrixCursor) getListView().getItemAtPosition(
					position);
			updateSumAtSelectedOpDisplay(data, getAccountCurSum());
		} else { // get more ops is clicked
			getMoreOps(false);
		}
	}

	@Override
	protected void onPause() {
		if (receiverIsRegistered) {
			receiverIsRegistered = false;
			unregisterReceiver(mOnInsertionReceiver);
		}
		if (null != mUpdateSumTask) {
			mUpdateSumTask.cancel(true);
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mQuickAddController.onSaveInstanceState(outState);
		outState.putLong("accountId", mAccountId);
		outState.putInt("mNbGetMoreOps", mNbGetMoreOps);
		updateLastSelectionIdxFromTop();
		outState.putInt("mLastSelectionFromTop", mLastSelectionFromTop);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		mLastSelectedPosition = (Integer) getLastNonConfigurationInstance();
		Log.d("Radis", "onRestoreInstanceState setting mLastSelectedPosition: "
				+ mLastSelectedPosition);
		mQuickAddController.onRestoreInstanceState(state);
		mAccountId = state.getLong("accountId");
		mOnRestore = true;
		mNbGetMoreOps = state.getInt("mNbGetMoreOps");
		mLastSelectionFromTop = state.getInt("mLastSelectionFromTop");
		initDbHelper();
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

	private Dialog createOccurenceDeleteDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.delete_recurring_op)
				.setCancelable(true)
				.setPositiveButton(R.string.del_only_current,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								deleteOp(mOpToDelete);
							}
						})
				.setNeutralButton(R.string.del_all_following,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								deleteOpAndFollowing(mOpToDelete);
							}
						})
				.setNegativeButton(R.string.del_all_occurrences,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteAllOccurences(mOpToDelete);
							}
						});
		return builder.create();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							deleteOp(mOpToDelete);
						}
					});
		case DIALOG_DELETE_OCCURENCE:
			return createOccurenceDeleteDialog();
		case DIALOG_PROJECTION:
			return ProjectionDateController.getDialog(this, mDbHelper);
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_PROJECTION) {
			ProjectionDateController.onPrepareDialog(mCurAccount);
		}
	}

	private void updateLastSelectionIdxFromTop() {
		ListView l = getListView();
		final int firstIdx = l.getFirstVisiblePosition();
		int pos = ((SelectedCursorAdapter) getListAdapter()).selectedPos;
		View v = l.getChildAt(pos - firstIdx);
		if (v != null) {
			mLastSelectionFromTop = v.getTop();
		}
	}

	// used by UpdateDisplayInterface
	@Override
	public void updateDisplay(Intent intent) {
		if (null != intent) {
			// intent is from insert service where modified accountIds were
			// saved
			// if one of them is the current accountId, updateDisplay, if not,
			// currently displayed account do not need to be refreshed
			Object[] accountIds = (Object[]) intent
					.getSerializableExtra("accountIds");
			for (int i = 0; i < accountIds.length; ++i) {
				if (((Long) accountIds[i]).equals(mAccountId)) {
					updateSumsDisplay(true);
					break;
				}
			}
		} else {
			updateSumsDisplay(true);
		}
	}

	private void updateSumsDisplay(boolean restoreOps) {
		mCurAccount.requery();
		mCurAccount.moveToFirst();
		mProjectionDate = mCurAccount.getLong(mCurAccount
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE));
		fillData();
		if (restoreOps && mNbGetMoreOps > 0) {
			getMoreOps(true);
		}
		updateSumsAndSelection();
	}

	// update db projection sum and display
	private void updateSumsAfterOpEdit(long oldSum, long sum, long date) {
		Log.d("Radis", "updateSumsAfterOpEdit oldSum : " + oldSum + " / sum : "
				+ sum);
		mDbHelper.updateProjection(mAccountId, -oldSum + sum, date);
		updateSumsAndSelection();
	}

	// update projection's sum
	private void updateFutureSumDisplay() {
		Button t = (Button) findViewById(R.id.future_sum);
		String format = getString(R.string.sum_at);
		if (mLastOps.moveToFirst()) {
			Cursor account = mCurAccount;
			account.requery();
			account.moveToFirst();
			t.setVisibility(View.VISIBLE);
			t.setText(String.format(
					format,
					Formater.getFullDateFormater(this)
							.format(account.getLong(account
									.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE))),
					Formater.getSumFormater()
							.format(account.getLong(account
									.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM)) / 100.0d)));
		} else {
			t.setVisibility(View.INVISIBLE);
		}
	}

	// used to display the left sum at current op (data)
	// and scroll to the current op (selectOpAndAdjustOffset)
	private void updateSumAtSelectedOpDisplay(Cursor data, long accountCurSum) {
		TextView t = (TextView) findViewById(R.id.date_sum);
		if (null != data && !data.isBeforeFirst() && !data.isAfterLast()) {
			int position = data.getPosition();
			mLastSelectedPosition = position;
			selectOpAndAdjustOffset(getListView(), position);
			long sum = accountCurSum + computeSumFromCursor(data);
			t.setText(String.format(getString(R.string.sum_at_selection),
					Formater.getSumFormater().format(sum / 100.0d)));
		} else {
			t.setText(String.format(getString(R.string.sum_at_selection),
					Formater.getSumFormater().format((accountCurSum / 100.0d))));
		}
	}

	private void updateSumsAndSelection() {
		final long curSum = getAccountCurSum();
		Cursor c = mLastOps;
		c.moveToFirst();
		updateFutureSumDisplay();
		Log.d("Radis", "updateSumsAndSelection mLastSelectedPosition : "
				+ mLastSelectedPosition);
		if (mLastSelectedPosition == null) {
			GregorianCalendar today = new GregorianCalendar();
			Tools.clearTimeOfCalendar(today);
			updateSumAtSelectedOpDisplay(findLastOpBeforeDate(today), curSum);
		} else {
			int position = mLastSelectedPosition.intValue();
			if (position < getListView().getCount()) { // attempt to fix
														// IndexOutOfBoundsException
														// (issue 106)
				// if (position < mLastOps.getCount()) {
				MatrixCursor data = (MatrixCursor) getListView()
						.getItemAtPosition(position);
				updateSumAtSelectedOpDisplay(data, curSum);
			}
		}
	}

}
