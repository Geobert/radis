package fr.geobert.radis;

import java.util.Date;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.editor.ScheduledOperationEditor;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

public class ScheduledOpList extends ListActivity {
	public static final String CURRENT_ACCOUNT = "accountId";

	private AdapterContextMenuInfo mOpToDelete;

	private Spinner mAccountSpinner;

	private long mCurrentAccount;

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
			super(ScheduledOpList.this, OperationTable.KEY_OP_SUM,
					OperationTable.KEY_OP_DATE, R.id.scheduled_icon, -1);
		}

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			String colName = cursor.getColumnName(columnIndex);
			if (colName
					.equals(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT)) {
				StringBuilder b = new StringBuilder();
				int periodicityUnit = cursor.getInt(columnIndex);
				int periodicity = cursor.getInt(columnIndex - 1);
				long endDate = cursor.getLong(columnIndex - 2);
				b.append(ScheduledOperation.getUnitStr(ScheduledOpList.this,
						periodicityUnit, periodicity));
				b.append(" - ");
				if (endDate > 0) {
					b.append(Formater.getFullDateFormater().format(
							new Date(endDate)));
				} else {
					b.append(ScheduledOpList.this
							.getString(R.string.no_end_date));
				}
				((TextView) view).setText(b.toString());
				return true;
			} else if (colName.equals(mDateColName)) {
				Date date = new Date(cursor.getLong(columnIndex));
				((TextView) view).setText(Formater.getFullDateFormater()
						.format(date));
				return true;
			} else {
				return super.setViewValue(view, cursor, columnIndex);
			}
		}
	}

	public static void callMe(Context ctx, final long currentAccountId) {
		Intent i = new Intent(ctx, ScheduledOpList.class);
		i.putExtra(CURRENT_ACCOUNT, currentAccountId);
		ctx.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.scheduled_ops);
		setContentView(R.layout.scheduled_list);
		registerForContextMenu(getListView());

		final GestureDetector gestureDetector = new GestureDetector(this,
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

		ImageButton btn = (ImageButton) findViewById(R.id.add_sch_op);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startCreateScheduledOp();
			}
		});

		mAccountSpinner = (Spinner) findViewById(R.id.account_spinner);
		mCurrentAccount = getIntent().getLongExtra(CURRENT_ACCOUNT, 0);
	}

	private void fillData() {
//		Cursor c = mCurrentAccount == 0 ? mDbHelper.fetchAllScheduledOps()
//				: mDbHelper.fetchScheduledOpsOfAccount(mCurrentAccount);
//		startManagingCursor(c);
//		String[] from = new String[] { CommonDbAdapter.KEY_OP_DATE,
//				CommonDbAdapter.KEY_THIRD_PARTY_NAME,
//				CommonDbAdapter.KEY_OP_SUM, CommonDbAdapter.KEY_ACCOUNT_NAME,
//				CommonDbAdapter.KEY_SCHEDULED_PERIODICITY_UNIT,
//				CommonDbAdapter.KEY_SCHEDULED_PERIODICITY,
//				CommonDbAdapter.KEY_SCHEDULED_END_DATE, };
//
//		int[] to = new int[] { R.id.scheduled_date, R.id.scheduled_third_party,
//				R.id.scheduled_sum, R.id.scheduled_account,
//				R.id.scheduled_infos };
//		SimpleCursorAdapter operations = new SimpleCursorAdapter(this,
//				R.layout.scheduled_row, c, from, to);
//		operations.setViewBinder(new InnerViewBinder());
//		setListAdapter(operations);
	}

	private void populateAccountSpinner() {
//		Cursor c = mDbHelper.fetchAllAccounts();
//		startManagingCursor(c);
//		if (c != null && c.isFirst()) {
//			ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this,
//					android.R.layout.simple_spinner_item);
//			adapter.add(new Account(0, getString(R.string.all_accounts)));
//			do {
//				adapter.add(new Account(c));
//			} while (c.moveToNext());
//
//			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			mAccountSpinner.setAdapter(adapter);
//			if (mCurrentAccount != 0) {
//				int pos = 0;
//				while (pos < adapter.getCount()) {
//					long id = adapter.getItemId(pos);
//					if (id == mCurrentAccount) {
//						mAccountSpinner.setSelection(pos);
//						break;
//					} else {
//						pos++;
//					}
//				}
//			}
//		}
//		mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
//
//			@Override
//			public void onItemSelected(AdapterView<?> parent, View view,
//					int pos, long id) {
//				Account a = (Account) parent.getItemAtPosition(pos);
//				mCurrentAccount = a.mAccountId;
//				fillData();
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView<?> arg0) {
//				// TODO Auto-generated method stub
//
//			}
//		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//		if (resultCode == RESULT_OK) {
//			if (mDbHelper == null) {
//				initDbHelper();
//			}
//			try {
//				fillData();
//			} catch (Exception e) {
//				ErrorReporter.getInstance().handleException(e);
//				Tools.popError(ScheduledOpList.this, e.getMessage(),
//						Tools.createRestartClickListener(this));
//			}
//		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateAccountSpinner();
		fillData();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		startEditScheduledOperation(id);
	}

//	@Override
//	protected Dialog onCreateDialog(int id) {
//		switch (id) {
//		case DIALOG_DELETE:
//			return askDeleteOccurrences();
//		default:
//			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
//		}
//	}

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
//		AdapterContextMenuInfo op = mOpToDelete;
//		Cursor cursorOp = mDbHelper.fetchOneScheduledOp(op.id);
//		startManagingCursor(cursorOp);
//		final long transId = cursorOp.getLong(cursorOp
//				.getColumnIndex(CommonDbAdapter.KEY_OP_TRANSFERT_ACC_ID));
//		if (delAllOccurrences) {
//			ScheduledOperation.deleteAllOccurences(mDbHelper, op.id);
//		}
//		if (mDbHelper.deleteScheduledOp(op.id)) {
//			fillData();
//			if (transId > 0) {
//				mDbHelper.consolidateSums(transId);
//			}
//		}
//		mOpToDelete = null;
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
		if (Tools.onDefaultMenuSelected(this, featureId, item)) {
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void startCreateScheduledOp() {
		Intent i = new Intent(this, ScheduledOperationEditor.class);
		i.putExtra(Tools.EXTRAS_OP_ID, -1l);
		i.putExtra(Tools.EXTRAS_ACCOUNT_ID, mCurrentAccount);
		startActivityForResult(i, ACTIVITY_SCH_OP_CREATE);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
	}

}
