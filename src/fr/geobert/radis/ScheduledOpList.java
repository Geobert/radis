package fr.geobert.radis;

import java.util.Date;

import org.acra.ErrorReporter;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ScheduledOpList extends ListActivity {
	private OperationsDbAdapter mDbHelper;

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
			super(ScheduledOpList.this, OperationsDbAdapter.KEY_OP_SUM,
					OperationsDbAdapter.KEY_OP_DATE, R.id.scheduled_icon);
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);
			if (colName
					.equals(OperationsDbAdapter.KEY_SCHEDULED_PERIODICITY_UNIT)) {
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}
		setTitle(R.string.scheduled_ops);
		setContentView(R.layout.scheduled_list);
		registerForContextMenu(getListView());
		mDbHelper = new OperationsDbAdapter(this);
		mDbHelper.open();
		fillData();

	}

	private void fillData() {
		Cursor c = mDbHelper.fetchAllScheduledOps();
		startManagingCursor(c);
		String[] from = new String[] { OperationsDbAdapter.KEY_OP_DATE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
				OperationsDbAdapter.KEY_OP_SUM,
				OperationsDbAdapter.KEY_ACCOUNT_NAME,
				OperationsDbAdapter.KEY_SCHEDULED_PERIODICITY_UNIT,
				OperationsDbAdapter.KEY_SCHEDULED_PERIODICITY,
				OperationsDbAdapter.KEY_SCHEDULED_END_DATE, };

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
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
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
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							try {
								deleteSchOp(mOpToDelete);
								fillData();
								mOpToDelete = null;
							} catch (Exception e) {
								Tools.popError(ScheduledOpList.this,
										e.getMessage(),
										Tools.createRestartClickListener());
							}

						}
					});
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
		}
	}

	private void deleteSchOp(AdapterContextMenuInfo info) {
		mDbHelper.deleteScheduledOp(info.id);
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
