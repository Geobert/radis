package fr.geobert.radis;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.editor.OperationEditor;
import fr.geobert.radis.editor.ScheduledOperationEditor;
import fr.geobert.radis.service.OnInsertionReceiver;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.ProjectionDateController;
import fr.geobert.radis.tools.QuickAddController;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public class OperationList extends BaseActivity implements
		UpdateDisplayInterface, LoaderCallbacks<Cursor> {
	private static final String TAG = "OperationList";
	public static final String INTENT_UPDATE_OP_LIST = "fr.geobert.radis.UPDATE_OP_LIST";
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

	public static final String OLD_SUM = "oldSum";
	public static final String SUM = "sum";
	public static final String OP_DATE = "opDate";
	public static final String NEW_ROWID = "newRowId";
	public static final String TRANSFERT_ID = "transfertId";
	public static final String UPDATE_SUM_NEEDED = "sumUpdateNeeded";
	public static final String OLD_TRANSFERT_ID = "oldTransfertId";

	private static final int GET_OPS = 500;
	private static final int GET_ACCOUNT = 510;

	private boolean mAdjustListScroll = false;
	private Long mAccountId;
	private Cursor mCurAccount;
	private ImageView mLoadingIcon;
	private AsyncTask<Void, Void, Long> mUpdateSumTask;
	private Integer mLastSelectedPosition = null;
	private boolean mOnRestore = false;
	private OnInsertionReceiver mOnInsertionReceiver;
	private IntentFilter mOnInsertionIntentFilter;
	private QuickAddController mQuickAddController;
	private Button mProjectionBtn;
	private long mProjectionDate;
	private int mLastSelectionFromTop; // last pos from top of ListView
	private boolean mReceiverIsRegistered;
	private SelectedCursorAdapter mOpListCursorAdapter;
	private ListView mListView;
	private AdapterContextMenuInfo mCurrentSelectedOp;
	private GregorianCalendar startOpDate; // start date of ops to get
	private GregorianCalendar endOpDate; // end date of ops to get

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
			super(OperationList.this, OperationTable.KEY_OP_SUM,
					OperationTable.KEY_OP_DATE, R.id.op_icon, mAccountId);
			initCache(c);
		}

		public void initCache(Cursor cursor) {
			mCellStates = cursor == null ? null : new int[cursor.getCount()];
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);
			if (colName.equals(InfoTables.KEY_TAG_NAME)) {
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
						.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)) > 0) {
					i.setVisibility(View.VISIBLE);
				} else {
					i.setVisibility(View.GONE);
				}

				boolean needSeparator = false;
				final int position = cursor.getPosition();
				assert (mCellStates != null);
				date1.setTimeInMillis(cursor.getLong(cursor
						.getColumnIndex(OperationTable.KEY_OP_DATE)));
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
								.getColumnIndex(OperationTable.KEY_OP_DATE)));
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
			} else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
				TextView textView = ((TextView) view);
				final long transfertId = cursor
						.getLong(cursor
								.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
				if (transfertId > 0 && transfertId == mAccountId) {
					textView.setText(cursor.getString(cursor
							.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)));
					return true;
				} else {
					String name = cursor.getString(cursor
							.getColumnIndex(colName));
					if (name != null) {
						textView.setText(name);
						return true;
					} else {
						name = cursor
								.getString(cursor
										.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME));
						if (name != null) {
							textView.setText(name);
							return true;
						} else {
							return false;
						}
					}
				}
			} else {
				return super.setViewValue(view, cursor, columnIndex);
			}
		}

		public void increaseCache(Cursor c) {
			int[] tmp = mCellStates;
			initCache(c);
			for (int i = 0; i < tmp.length; ++i) {
				mCellStates[i] = tmp[i];
			}
		}
	}

	private class SelectedCursorAdapter extends SimpleCursorAdapter {
		private int selectedPos = -1;

		SelectedCursorAdapter(Context context, int layout, String[] from,
				int[] to) {
			super(context, layout, null, from, to,
					CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
			InnerViewBinder viewBinder = new InnerViewBinder(null);
			setViewBinder(viewBinder);
		}

		public void setSelectedPosition(int pos) {
			selectedPos = pos;
			// inform the view of this change
			notifyDataSetChanged();
		}

		@Override
		public Cursor swapCursor(Cursor c) {
			Cursor old = super.swapCursor(c);
			((InnerViewBinder) getViewBinder()).initCache(c);
			return old;
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

	public static void refreshDisplay(Context ctx, long accountId) {
		Intent i = new Intent(OperationList.INTENT_UPDATE_OP_LIST);
		i.putExtra("accountIds", new Long[] { Long.valueOf(accountId) });
		ctx.sendOrderedBroadcast(i, null);
	}

	private void startLoadingAnim() {
		mLoadingIcon.setVisibility(View.VISIBLE);
		mLoadingIcon.startAnimation(AnimationUtils.loadAnimation(this,
				R.anim.rotation));
	}

	private void getMoreOps(final boolean isRestoring) {
		startLoadingAnim();
		startOpDate.add(Calendar.MONTH, -1);
		getSupportLoaderManager().restartLoader(GET_OPS, null, this);
	}

	private void initReferences() {
		mQuickAddController = new QuickAddController(this, this);
		mQuickAddController.setAccount(mAccountId);
		mReceiverIsRegistered = false;
		mOnInsertionReceiver = new OnInsertionReceiver(this);
		mOnInsertionIntentFilter = new IntentFilter(Tools.INTENT_OP_INSERTED);
		registerReceiver(mOnInsertionReceiver, new IntentFilter(
				INTENT_UPDATE_OP_LIST));
		mProjectionBtn = (Button) findViewById(R.id.future_sum);
		mProjectionBtn.setVisibility(View.INVISIBLE);
	}

	private void initViewBehavior() {
		mQuickAddController.initViewBehavior();
		final GestureDetector gestureDetector = new GestureDetector(this,
				new ListViewSwipeDetector(mListView, new ListSwipeAction() {
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
		mListView.setOnTouchListener(gestureListener);
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
		mAdjustListScroll = true;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.operation_list);

		mListView = (ListView) findViewById(android.R.id.list);
		registerForContextMenu(mListView);
		mListView.setEmptyView(findViewById(android.R.id.empty));

		LayoutInflater inf = getLayoutInflater();
		View footer = inf.inflate(R.layout.op_list_footer, mListView, false);
		mLoadingIcon = (ImageView) footer.findViewById(R.id.loading_icon);
		mListView.addFooterView(footer);

		Bundle extras = getIntent().getExtras();
		mAccountId = extras != null ? extras.getLong(Tools.EXTRAS_ACCOUNT_ID)
				: null;

		initReferences();

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				onListItemClick(position, id);
			}
		});

		mLastSelectedPosition = null;
		mLastSelectionFromTop = 0;

		fillData();
	}

	private void initQuickAdd() {
		boolean hideQuickAdd = DBPrefsManager.getInstance(this).getBoolean(
				RadisConfiguration.KEY_HIDE_OPS_QUICK_ADD);
		int visibility = View.VISIBLE;
		if (hideQuickAdd) {
			visibility = View.GONE;
		}
		mQuickAddController.setVisibility(visibility);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initQuickAdd();
		initViewBehavior();
		if (!mReceiverIsRegistered) {
			mReceiverIsRegistered = true;
			registerReceiver(mOnInsertionReceiver, mOnInsertionIntentFilter);
		}

		mQuickAddController.setAutoNegate(true);
		Log.d(TAG, "onResume");
		mQuickAddController.clearFocus();
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
			Cursor c = (Cursor) mListView.getItemAtPosition(info.position);
			if (c != null) {
				if (!c.isBeforeFirst() && !c.isAfterLast()) {
					if (c.getLong(c
							.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)) > 0) {
						menu.add(0, EDIT_SCH_OP_ID, 0, R.string.edit_scheduling);
					} else {
						menu.add(0, CONVERT_OP_ID, 0,
								R.string.convert_into_scheduling);
					}
					menu.add(0, DELETE_OP_ID, 0, R.string.delete);
				}
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
			AccountTable.consolidateSums(this, mAccountId);
			updateSumsDisplay();
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
		mCurrentSelectedOp = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE_OP_ID: {
			Cursor c = (Cursor) mListView
					.getItemAtPosition(mCurrentSelectedOp.position);
			if (!c.isBeforeFirst()
					&& !c.isAfterLast()
					&& c.getLong(c
							.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)) > 0) {
				showDialog(DIALOG_DELETE_OCCURENCE);
			} else {
				showDialog(DIALOG_DELETE);
			}
		}
			return true;
		case EDIT_OP_ID:
			startOperationEdit(mCurrentSelectedOp.id);
			return true;
		case CONVERT_OP_ID:
			startScheduledOpEditor(mCurrentSelectedOp.id, true);
			return true;
		case EDIT_SCH_OP_ID: {
			Cursor c = (Cursor) mListView
					.getItemAtPosition(mCurrentSelectedOp.position);
			startScheduledOpEditor(c.getLong(c
					.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)), false);
		}
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void afterAddOp(Intent data) {
		long newRowId = data.getLongExtra(NEW_ROWID, -1);
		Log.d(TAG, "afterAddOp newRowId: " + newRowId);
		mAdjustListScroll = true;
		final int listCount = mListView.getCount();
		if (null == mLastSelectedPosition) {
			Log.d(TAG, "mLastSelectedPosition INIT");
			mLastSelectedPosition = 0;
		}
		if (newRowId > -1) {
			ListAdapter adap = mListView.getAdapter();
			int position = 0;
			for (; position < adap.getCount(); ++position) {
				if (newRowId == adap.getItemId(position)) {
					break;
				}
			}

			Log.d(TAG, "mLastSelectedPosition: " + mLastSelectedPosition);
			Log.d(TAG, "position: " + position);
			if (position <= mLastSelectedPosition
					&& mLastSelectedPosition < listCount - 1) {
				Log.d(TAG, "mLastSelectedPosition is incremeted");
				mLastSelectedPosition++;
			}
		}
		if (mLastSelectedPosition >= listCount) {
			mLastSelectedPosition = listCount - 1;
		}

		final long oldSum = data.getLongExtra(OLD_SUM, 0);
		final long sum = data.getLongExtra(SUM, 0);
		final long date = data.getLongExtra(OP_DATE, 0);
		final long transId = data.getLongExtra(TRANSFERT_ID, 0);
		final long oldTransId = data.getLongExtra(OLD_TRANSFERT_ID, 0);
		final long sumToAdd = -oldSum + sum;

		if (data.getBooleanExtra(UPDATE_SUM_NEEDED, false)) {
			mOnRestore = true;
			showProgress();
			initDateRange();
			updateSumsAfterOpEdit(sumToAdd, date, mAccountId);
		}
		if (transId > 0) {
			long s;
			if (oldTransId > 0) {
				s = sum;
			} else {
				s = sumToAdd;
			}
			AccountTable.updateProjection(this, transId, -s, date);
		}
		if (oldTransId > 0) {
			AccountTable.consolidateSums(this, oldTransId);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ACTIVITY_OP_CREATE
				|| requestCode == ACTIVITY_OP_EDIT) {
			if (resultCode == RESULT_OK) {
				updateSumsDisplay();// requestCode == ACTIVITY_OP_CREATE);
				afterAddOp(data);
			}
		} else if (requestCode == CREATE_SCH_OP) {
			if (resultCode == RESULT_OK) {
				final long schOpId = data.getLongExtra("schOperationId", 0);
				final long opId = data.getLongExtra("opIdSource", 0);
				if (opId > 0 && schOpId > 0) {
					OperationTable.updateOp(this, opId, schOpId);
				} else {
					// TODO error
				}
			}
		}
	}

	private void initDateRange() {
		Cursor lastOp = OperationTable.fetchLastOp(this, mAccountId);
		endOpDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(endOpDate);
		startOpDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(startOpDate);
		startOpDate.add(Calendar.MONTH, -1);
		if (lastOp != null) {
			if (lastOp.moveToFirst()) {
				long date = lastOp.getLong(lastOp
						.getColumnIndex(OperationTable.KEY_OP_DATE));
				Log.d(TAG, "initDateRange, date of lastOp : " + Formater.getFullDateFormater().format(new Date(date)));
				Log.d(TAG, "initDateRange, endOpDate : " + Formater.getFullDateFormater().format(new Date(endOpDate.getTimeInMillis())));
				if (date > endOpDate.getTimeInMillis()) {
					Log.d(TAG, "initDateRange, change endDate");
					endOpDate.setTimeInMillis(date);
				}
				Log.d(TAG, "initDateRange, startOpDate : " + Formater.getFullDateFormater().format(new Date(startOpDate.getTimeInMillis())));
				if (date < startOpDate.getTimeInMillis()) {
					Log.d(TAG, "initDateRange, change startDate");
					startOpDate.setTimeInMillis(date);
				}
			}
			lastOp.close();
		}
		startOpDate.set(Calendar.DAY_OF_MONTH,
				startOpDate.getActualMinimum(Calendar.DAY_OF_MONTH));
	}

	private void fillData() {
		initDateRange();

		String[] from = new String[] { OperationTable.KEY_OP_DATE,
				InfoTables.KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_SUM,
				InfoTables.KEY_TAG_NAME, InfoTables.KEY_MODE_NAME };

		int[] to = new int[] { R.id.op_date, R.id.op_third_party, R.id.op_sum,
				R.id.op_infos };

		showProgress();
		if (mOpListCursorAdapter == null) {
			mOpListCursorAdapter = new SelectedCursorAdapter(this,
					R.layout.operation_row, from, to);
			mListView.setAdapter(mOpListCursorAdapter);
			getSupportLoaderManager().initLoader(GET_OPS, null, this);
			getSupportLoaderManager().initLoader(GET_ACCOUNT, null, this);
		} else {
			getSupportLoaderManager().restartLoader(GET_OPS, null, this);
		}
	}

	private void startCreateOp() {
		Intent i = new Intent(this, OperationEditor.class);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mAccountId);
		i.putExtra(Tools.EXTRAS_OP_ID, -1l);
		startActivityForResult(i, ACTIVITY_OP_CREATE);
	}

	private void deleteOp(long pos) {
		Cursor c = (Cursor) mListView
				.getItemAtPosition(mCurrentSelectedOp.position);
		final long sum = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_SUM));
		final long date = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_DATE));
		final long transId = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
		updateLastSelectionIdxFromTop();

		if (OperationTable.deleteOp(this,
				c.getLong(c.getColumnIndex(OperationTable.KEY_OP_ROWID)),
				mAccountId)) {
			if (c.getPosition() < mLastSelectedPosition) {
				mLastSelectedPosition--;
			}
			Cursor lastOp = OperationTable.fetchLastOp(this, mAccountId);
			if (lastOp != null && lastOp.moveToFirst() && date >= lastOp.getLong(lastOp
				.getColumnIndex(OperationTable.KEY_OP_DATE))) {
				mLastSelectedPosition = null;
			}

			updateSumsAfterOpEdit(-sum, date, mAccountId);
			if (transId > 0) {
				AccountTable.consolidateSums(this, transId);
			}
		} else {
			updateSumsDisplay();
		}
	}

	private void deleteOpAndFollowing(long pos) {
		Cursor c = (Cursor) mListView
				.getItemAtPosition(mCurrentSelectedOp.position);
		final long sum = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_SUM));
		final long transId = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
		updateLastSelectionIdxFromTop();

		final long schOpId = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID));
		final long date = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_DATE));
		int nbDeleted = OperationTable.deleteAllFutureOccurrences(this,
				mAccountId, schOpId, date);
		Log.d(TAG, "deleteOpAndFollowing, nbDeleted : " + nbDeleted + " / pos " + c.getPosition() + "/ last pos " + mLastSelectedPosition);
		if (nbDeleted > 0) {
			if (c.getPosition() < mLastSelectedPosition) {
				mLastSelectedPosition--;
			}
			Cursor lastOp = OperationTable.fetchLastOp(this, mAccountId);
			if (lastOp != null && lastOp.moveToFirst() && date >= lastOp.getLong(lastOp
				.getColumnIndex(OperationTable.KEY_OP_DATE))) {
				mLastSelectedPosition = null;
			}
			updateSumsAfterOpEdit(-(sum * nbDeleted), date, mAccountId);
			if (transId > 0) {
				AccountTable.consolidateSums(this, transId);
			}
		} else {
			updateSumsDisplay();
		}
	}

	private void deleteAllOccurences(long pos) {
		Cursor c = (Cursor) mListView
				.getItemAtPosition(mCurrentSelectedOp.position);
		final long sum = c.getLong(c.getColumnIndex(OperationTable.KEY_OP_SUM));
		final long date = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_DATE));
		final long transId = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
		updateLastSelectionIdxFromTop();

		final long schOpId = c.getLong(c
				.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID));
		if (ScheduledOperationTable.deleteScheduledOp(this, schOpId)) {
			final int nbDeleted = OperationTable.deleteAllOccurrences(this,
					mAccountId, schOpId);
			Log.d(TAG, "deleteAllOccurences, nbDeleted : " + nbDeleted);
			if (nbDeleted > 0) {
				if (c.getPosition() < mLastSelectedPosition) {
					mLastSelectedPosition--;
				}
				Cursor lastOp = OperationTable.fetchLastOp(this, mAccountId);
				if (lastOp != null && lastOp.moveToFirst() && date >= lastOp.getLong(lastOp
					.getColumnIndex(OperationTable.KEY_OP_DATE))) {
					mLastSelectedPosition = null;
				}

				updateSumsAfterOpEdit(-(sum * nbDeleted), date, mAccountId);
				if (transId > 0) {
					AccountTable.consolidateSums(this, transId);
				}
			} else {
				updateSumsDisplay();
			}
		}
	}

	private long getAccountCurSum() {
		return mCurAccount.getLong(mCurAccount
				.getColumnIndexOrThrow(AccountTable.KEY_ACCOUNT_CUR_SUM));
	}

	private long computeSumFromCursor(Cursor op) {
//		Log.d(TAG,
//				"computeSumFromCursor : "
//						+ op.getString(op
//								.getColumnIndex(InfoTables.KEY_THIRD_PARTY_NAME))
//						+ " / id : "
//						+ op.getLong(op
//								.getColumnIndex(OperationTable.KEY_OP_ROWID)));
		long sum = 0L;
		final int dateIdx = op.getColumnIndex(OperationTable.KEY_OP_DATE);
		final int opSumIdx = op.getColumnIndex(OperationTable.KEY_OP_SUM);
		final int transIdx = op
				.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID);
		long opDate = op.getLong(dateIdx);
		Log.d(TAG,
				"computeSumFromCursor mProjectionDate : "
						+ Formater.getFullDateFormater().format(
								new Date(mProjectionDate)) + "/opDate : " + Tools.getDateStr(opDate));
		if (null != op && !op.isBeforeFirst() && !op.isAfterLast()) {
			if (opDate <= mProjectionDate || mProjectionDate == 0) {
				boolean hasPrev = op.moveToPrevious();
				Log.d(TAG, "computeSumFromCursor op is <= projDate, hasPrev : "
						+ hasPrev);

				if (hasPrev) {
					opDate = op.getLong(dateIdx);
					Log.d(TAG,
							"computeSumFromCursor opDate : "
									+ Tools.getDateStr(opDate)
									+ " / projDate : "
									+ Tools.getDateStr(mProjectionDate));
					while (hasPrev && opDate <= mProjectionDate) {
						long s = op.getLong(opSumIdx);
						if (op.getLong(transIdx) == mAccountId) {
							Log.d(TAG, "computeSumFromCursor invert sum");
							s = -s;
						}
						sum = sum + s;
						Log.d(TAG, "cur sum : " + sum + "/date : " + Tools.getDateStr(opDate));
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
				Log.d(TAG, "computeSumFromCursor op is > projDate, hasNext : "
						+ hasNext);
				if (hasNext) {
					opDate = op.getLong(dateIdx);
					Log.d(TAG,
							"computeSumFromCursor opDate : "
									+ Tools.getDateStr(opDate)
									+ " / projDate : "
									+ Tools.getDateStr(mProjectionDate));
					while (hasNext && opDate > mProjectionDate) {
						long s = op.getLong(opSumIdx);
						if (op.getLong(transIdx) == mAccountId) {
							s = -s;
						}
						sum = sum + s;
						hasNext = op.moveToNext();
						if (hasNext) {
							opDate = op.getLong(dateIdx);
						}
					}
				}
			}
		}
		Log.d(TAG, "computeSumFromCursor after sum = " + sum);
		return sum;
	}

	private Cursor findLastOpBeforeDate(GregorianCalendar date) {
		Cursor ops = getListAdapter().getCursor();
		if (ops.moveToFirst()) {
			long dateLong = date.getTimeInMillis();
			do {
				long opDate = ops.getLong(ops
						.getColumnIndex(OperationTable.KEY_OP_DATE));
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

		if (mAdjustListScroll) {
			mAdjustListScroll = false;
			int relativePos = position - firstIdx;
			SelectedCursorAdapter adapter = getListAdapter();
			adapter.setSelectedPosition(position);

			// check if the selected pos is visible on screen
			int posFromTop;
			if ((position >= firstIdx) && (position < lastIdx)) {
				posFromTop = mOnRestore ? mLastSelectionFromTop
						: ((relativePos - 1) * offset) + firstOffset
								+ relativePos;
			} else {
				posFromTop = mLastSelectionFromTop != 0 ? mLastSelectionFromTop
						: (int) (Tools.SCREEN_HEIGHT * 0.3);
			}
			l.setSelectionFromTop(position, posFromTop);
		}
		Log.d(TAG, "selectOpAndAdjustOffset setting mLastSelectedPosition: "
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

	private SelectedCursorAdapter getListAdapter() {
		return (SelectedCursorAdapter) ((HeaderViewListAdapter) mListView
				.getAdapter()).getWrappedAdapter();
	}

	protected void onListItemClick(int position, long id) {
		Log.d(TAG, "onListItemClick : " + position + "/ id : " + id);
		if (id != -1) {
			mOpListCursorAdapter.setSelectedPosition(position);
			Cursor data = (Cursor) mOpListCursorAdapter.getItem(position);
			mAdjustListScroll = true;
			updateSumAtSelectedOpDisplay(data, getAccountCurSum());
		} else { // get more ops is clicked
			getMoreOps(false);
		}
	}

	@Override
	protected void onPause() {
		// if (mReceiverIsRegistered) {
		// mReceiverIsRegistered = false;
		// unregisterReceiver(mOnInsertionReceiver);
		// }
		if (null != mUpdateSumTask) {
			mUpdateSumTask.cancel(true);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "unregisterReceiver");
		unregisterReceiver(mOnInsertionReceiver);
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mQuickAddController.onSaveInstanceState(outState);
		outState.putSerializable("mLastSelectedPosition", mLastSelectedPosition);
		outState.putLong("accountId", mAccountId);
		updateLastSelectionIdxFromTop();
		outState.putInt("mLastSelectionFromTop", mLastSelectionFromTop);
		outState.putBoolean("mAdjustListScroll", mAdjustListScroll);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		mLastSelectedPosition = (Integer) state.getSerializable("mLastSelectedPosition");
		Log.d(TAG, "onRestoreInstanceState setting mLastSelectedPosition: "
				+ mLastSelectedPosition);
		mQuickAddController.onRestoreInstanceState(state);
		mAccountId = state.getLong("accountId");
		mOnRestore = true;
		mLastSelectionFromTop = state.getInt("mLastSelectionFromTop");
		mAdjustListScroll = state.getBoolean("mAdjustListScroll");
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
								deleteOp(mCurrentSelectedOp.position);
							}
						})
				.setNeutralButton(R.string.del_all_following,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								deleteOpAndFollowing(mCurrentSelectedOp.position);
							}
						})
				.setNegativeButton(R.string.del_all_occurrences,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteAllOccurences(mCurrentSelectedOp.position);
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
							deleteOp(mCurrentSelectedOp.position);
						}
					});
		case DIALOG_DELETE_OCCURENCE:
			return createOccurenceDeleteDialog();
		case DIALOG_PROJECTION:
			return ProjectionDateController.getDialog(this);
		default:
			return Tools.onDefaultCreateDialog(this, id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_PROJECTION) {
			ProjectionDateController.onPrepareDialog(mCurAccount);
		}
	}

	private void updateLastSelectionIdxFromTop() {
		ListView l = mListView;
		final int firstIdx = l.getFirstVisiblePosition();
		int pos = getListAdapter().selectedPos;
		View v = l.getChildAt(pos - firstIdx);
		if (v != null) {
			mLastSelectionFromTop = v.getTop();
		}
		mAdjustListScroll = true;
	}

	// used by UpdateDisplayInterface
	@Override
	public void updateDisplay(Intent intent) {
		Log.d(TAG, "updateDisplay, intent : " + intent + "/mAccountId "
				+ mAccountId);
		if (null != intent) {
			// intent is from insert service where modified accountIds were
			// saved
			// if one of them is the current accountId, updateDisplay, if not,
			// currently displayed account do not need to be refreshed
			Object[] accountIds = (Object[]) intent
					.getSerializableExtra("accountIds");
			Log.d(TAG, "updateDisplay, accountIds  " + accountIds);
			if (null != accountIds) {
				Log.d(TAG, "updateDisplay, accountIds.length  "
						+ accountIds.length);
				for (int i = 0; i < accountIds.length; ++i) {
					if (((Long) accountIds[i]).equals(mAccountId)) {
						Log.d(TAG, "updateDisplay do update");
						showProgress();
						updateSumsDisplay();
						getSupportLoaderManager().restartLoader(GET_OPS, null,
								this);
						break;
					}
				}
			}
		} else {
			mOnRestore = true;
			showProgress();
			updateSumsDisplay();
			getSupportLoaderManager().restartLoader(GET_OPS, null, this);
		}
	}

	private void updateSumsDisplay() {
		LoaderManager lm = getSupportLoaderManager();
		lm.destroyLoader(GET_ACCOUNT);
		lm.initLoader(GET_ACCOUNT, null, this);
		AccountList.refreshDisplay(this);
		// lm.restartLoader(GET_OPS, null, this);
	}

	// update db projection sum and display
	private void updateSumsAfterOpEdit(long sumToAdd, long date, long accountId) {
		getSupportLoaderManager().restartLoader(GET_OPS, null, this);
		AccountTable.updateProjection(this, accountId, sumToAdd, date);
		updateSumsDisplay();
	}

	// update projection's sum
	private void updateFutureSumDisplay() {
		Button t = (Button) findViewById(R.id.future_sum);
		String format = getString(R.string.sum_at);
		if (mOpListCursorAdapter.getCount() > 0) {
			t.setVisibility(View.VISIBLE);
			String date = Formater
					.getFullDateFormater()
					.format(mCurAccount.getLong(mCurAccount
							.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE)));
			t.setText(String.format(format, date, Formater.getSumFormater()
					.format(getAccountCurSum() / 100.0d)));
		} else {
			t.setVisibility(View.INVISIBLE);
		}
	}

	// used to display the left sum at current op (data)
	// and scroll to the current op (selectOpAndAdjustOffset)
	private void updateSumAtSelectedOpDisplay(Cursor data, long accountCurSum) {
		TextView t = (TextView) findViewById(R.id.date_sum);
		long sum = accountCurSum;
		if (null != data && !data.isBeforeFirst() && !data.isAfterLast()) {
			int position = data.getPosition();
			mLastSelectedPosition = position;
			selectOpAndAdjustOffset(mListView, position);
			sum = accountCurSum + computeSumFromCursor(data);
			Log.d(TAG, "updateSumAtSelectedOpDisplay valid cursor, new sum : "
					+ sum + " / data count : " + data.getCount());
		}
		t.setText(String.format(getString(R.string.sum_at_selection), Formater
				.getSumFormater().format(sum / 100.0d)));
	}

	private void updateSumsAndSelection() {
		if (mCurAccount != null && getListAdapter().getCursor() != null) {
			final long curSum = getAccountCurSum();
			Log.d(TAG, "updateSumsAndSelection, curSum : " + curSum
					+ " — mLastSelectedPosition, " + mLastSelectedPosition);
			updateFutureSumDisplay();
			if (mLastSelectedPosition == null) {
				GregorianCalendar today = new GregorianCalendar();
				Tools.clearTimeOfCalendar(today);
				updateSumAtSelectedOpDisplay(findLastOpBeforeDate(today),
						curSum);
			} else {
				int position = mLastSelectedPosition.intValue();
				if (position < mListView.getCount()) { // attempt to fix
					// IndexOutOfBoundsException
					// (issue 106)
					// if (position < mLastOps.getCount()) {
					Cursor data = (Cursor) mListView
							.getItemAtPosition(position);
					updateSumAtSelectedOpDisplay(data, curSum);
				} else {
					updateSumAtSelectedOpDisplay(null, curSum);
				}
			}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		CursorLoader loader = null;
		switch (id) {
		case GET_OPS:
			loader = OperationTable.getOpsBetweenDateLoader(this, startOpDate,
					endOpDate, mAccountId);
			break;
		case GET_ACCOUNT:
			loader = AccountTable.getAccountLoader(this, mAccountId);
		default:
			break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
		case GET_OPS:
			Log.d(TAG, "onLoadFinished GET OPS");
			// if there are no results
			// if (data.getCount() == 0) {
			// // let the user know
			// mListView.setEmptyView(findViewById(android.R.id.empty));
			// } else {
			// // otherwise clear it, so it won't flash in between cursor loads
			// mListView.setEmptyView(null);
			// }
			Cursor old = mOpListCursorAdapter.swapCursor(data);
			hideProgress();
			mLoadingIcon.clearAnimation();
			mLoadingIcon.setVisibility(View.INVISIBLE);
			if (old != null) {
				if (!mOnRestore) {
					if (old.getCount() == data.getCount()) {
						Toast.makeText(OperationList.this,
								R.string.no_more_ops, Toast.LENGTH_LONG).show();
					} else {
						((InnerViewBinder) getListAdapter().getViewBinder())
								.increaseCache(data);
					}
				}
				old.close();
			}
			updateSumsAndSelection();
			mOnRestore = false;
			mQuickAddController.clearFocus();
			break;
		case GET_ACCOUNT:
			Log.d(TAG, "onLoadFinished GET ACCOUNT");
			if (mCurAccount != null) {
				mCurAccount.close();
			}
			mCurAccount = data;
			if (mCurAccount.moveToFirst()) {
				AccountTable.initProjectionDate(data);
				mProjectionDate = mCurAccount.getLong(mCurAccount
						.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE));
			}
			updateSumsAndSelection();
			break;
		default:
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case GET_OPS:
			Cursor old = mOpListCursorAdapter.swapCursor(null);
			if (old != null) {
				old.close();
			}
			break;
		case GET_ACCOUNT:
			if (null != mCurAccount) {
				mCurAccount.close();
			}
			mCurAccount = null;
		default:
			break;
		}
	}

}
