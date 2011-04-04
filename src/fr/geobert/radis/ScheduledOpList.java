package fr.geobert.radis;

import java.util.Date;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.editor.ScheduledOperationEditor;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

public class ScheduledOpList extends ListActivity {
	private CommonDbAdapter mDbHelper;

	private AdapterContextMenuInfo mOpToDelete;

	// activities ids
	private static final int ACTIVITY_SCH_OP_CREATE = 0;
	private static final int ACTIVITY_SCH_OP_EDIT = 1;

	// context menu ids
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;

	// dialog ids
	private static final int DIALOG_DELETE = 0;

	private class InnerViewBinder extends OpViewBinder {

		public InnerViewBinder() {
			super(ScheduledOpList.this, CommonDbAdapter.KEY_OP_SUM,
					CommonDbAdapter.KEY_OP_DATE, R.id.scheduled_icon);
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);
			if (colName.equals(CommonDbAdapter.KEY_SCHEDULED_PERIODICITY_UNIT)) {
				StringBuilder b = new StringBuilder();
				int periodicityUnit = cursor.getInt(columnIndex);
				int periodicity = cursor.getInt(columnIndex - 1);
				long endDate = cursor.getLong(columnIndex - 2);
				b.append(ScheduledOperation.getUnitStr(ScheduledOpList.this,
						periodicityUnit, periodicity));
				b.append(" - ");
				if (endDate > 0) {
					b.append(Formater.DATE_FORMAT.format(new Date(endDate)));
				} else {
					b.append(ScheduledOpList.this
							.getString(R.string.no_end_date));
				}
				((TextView) view).setText(b.toString());
				return true;
			} else {
				return super.setViewValue(view, cursor, columnIndex);
			}
		}
	}

	private void initDbHelper() {
		mDbHelper = CommonDbAdapter.getInstance(this);
		mDbHelper.open();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}
		setTitle(R.string.scheduled_ops);
		setContentView(R.layout.scheduled_list);
		registerForContextMenu(getListView());

		final GestureDetector gestureDetector = new GestureDetector(
				new ListViewSwipeDetector(getListView(), new ListSwipeAction() {
					@Override
					public void run() {
						ScheduledOpList.this.finish();
					}
				}, new ListSwipeAction() {
					@Override
					public void run() {
						if (mRowId > 0) {
							startEditScheduledOperation(mRowId);
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

	private void fillData() {
		Cursor c = mDbHelper.fetchAllScheduledOps();
		startManagingCursor(c);
		String[] from = new String[] { CommonDbAdapter.KEY_OP_DATE,
				CommonDbAdapter.KEY_THIRD_PARTY_NAME,
				CommonDbAdapter.KEY_OP_SUM, CommonDbAdapter.KEY_ACCOUNT_NAME,
				CommonDbAdapter.KEY_SCHEDULED_PERIODICITY_UNIT,
				CommonDbAdapter.KEY_SCHEDULED_PERIODICITY,
				CommonDbAdapter.KEY_SCHEDULED_END_DATE, };

		int[] to = new int[] { R.id.scheduled_date, R.id.scheduled_third_party,
				R.id.scheduled_sum, R.id.scheduled_account,
				R.id.scheduled_infos };
		SimpleCursorAdapter operations = new SimpleCursorAdapter(this,
				R.layout.scheduled_row, c, from, to);
		operations.setViewBinder(new InnerViewBinder());
		setListAdapter(operations);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			try {
				fillData();
			} catch (Exception e) {
				ErrorReporter.getInstance().handleException(e);
				Tools.popError(ScheduledOpList.this, e.getMessage(),
						Tools.createRestartClickListener());
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		initDbHelper();
		fillData();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(state);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE:
			return askDeleteOccurrences();
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
		}
	}

	private AlertDialog askDeleteOccurrences() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.ask_delete_occurrences)
				.setCancelable(false)
				.setPositiveButton(R.string.del_all_occurrences,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteSchOp(true);
							}
						})
				.setNeutralButton(R.string.cancel_delete_occurrences,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteSchOp(false);
							}
						})
				.setNegativeButton(R.string.cancel_sch_deletion,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		return builder.create();
	}

	private void deleteSchOp(final boolean delAllOccurrences) {
		AdapterContextMenuInfo op = mOpToDelete;
		if (delAllOccurrences) {
			Cursor schOp = mDbHelper.fetchOneScheduledOp(op.id);
			startManagingCursor(schOp);
			final long accountId = schOp.getLong(schOp
					.getColumnIndex(CommonDbAdapter.KEY_SCHEDULED_ACCOUNT_ID));
			int nbDeleted = mDbHelper.deleteAllOccurrences(accountId, op.id);
			// update account op sum, current sum and current date
			final double total = nbDeleted
					* schOp.getDouble(schOp
							.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
			Cursor accountCursor = mDbHelper.fetchAccount(accountId);
			startManagingCursor(accountCursor);
			final double curSum = accountCursor.getDouble(accountCursor
					.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
			mDbHelper.updateOpSum(accountId, curSum - total);
			Cursor lastOp = mDbHelper.fetchNLastOps(1, accountId);
			if (null != lastOp) {
				lastOp.moveToFirst();
			}
			startManagingCursor(lastOp);
			mDbHelper.updateCurrentSum(accountId, lastOp.getLong(lastOp
					.getColumnIndex(CommonDbAdapter.KEY_OP_DATE)));
		}
		mDbHelper.deleteScheduledOp(op.id);
		fillData();
		mOpToDelete = null;
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
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case DELETE_OP_ID:
			showDialog(DIALOG_DELETE);
			mOpToDelete = info;
			return true;
		case EDIT_OP_ID:
			startEditScheduledOperation(info.id);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void startEditScheduledOperation(long id) {
		Intent i = new Intent(this, ScheduledOperationEditor.class);
		i.putExtra(Tools.EXTRAS_OP_ID, id);
		startActivityForResult(i, ACTIVITY_SCH_OP_EDIT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.scheduled_op_list_menu, menu);
		inflater.inflate(R.menu.common_menu, menu);
		return true;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (Tools.onKeyLongPress(keyCode, event, this)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.create_scheduled_operation:
			startCreateScheduledOp();
			return true;
		default:
			if (Tools.onDefaultMenuSelected(this, featureId, item)) {
				return true;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void startCreateScheduledOp() {
		Intent i = new Intent(this, ScheduledOperationEditor.class);
		i.putExtra(Tools.EXTRAS_OP_ID, -1l);
		startActivityForResult(i, ACTIVITY_SCH_OP_CREATE);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
	}

}
