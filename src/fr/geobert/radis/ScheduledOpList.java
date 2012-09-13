package fr.geobert.radis;

import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.editor.ScheduledOperationEditor;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

public class ScheduledOpList extends BaseActivity implements
		LoaderCallbacks<Cursor> {
	public static final String CURRENT_ACCOUNT = "accountId";
	private static final String TAG = "ScheduleOpList";

	private AdapterContextMenuInfo mOpToDelete;

	private Spinner mAccountSpinner;

	private long mCurrentAccount;

	private ListView mListView;

	private SimpleCursorAdapter mAdapter;

	private CursorLoader mLoader;
	private TextView mTotalLbl;

	// activities ids
	private static final int ACTIVITY_SCH_OP_CREATE = 0;
	private static final int ACTIVITY_SCH_OP_EDIT = 1;

	// context menu ids
	private static final int DELETE_OP_ID = Menu.FIRST + 1;
	private static final int EDIT_OP_ID = Menu.FIRST + 2;

	// dialog ids
	private static final int DIALOG_DELETE = 0;

	private static final int GET_ALL_SCH_OPS = 900;
	private static final int GET_SCH_OPS_OF_ACCOUNT = 910;

	private class InnerViewBinder extends OpViewBinder {

		public InnerViewBinder() {
			super(ScheduledOpList.this, OperationTable.KEY_OP_SUM,
					OperationTable.KEY_OP_DATE, R.id.scheduled_icon,
					mCurrentAccount);
		}

		public void setCurAccount(long accountId) {
			mCurAccountId = accountId;
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
			} else if (colName.equals(InfoTables.KEY_THIRD_PARTY_NAME)) {
				if (mCurrentAccount != 0
						&& mCurrentAccount == cursor
								.getLong(cursor
										.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))) {
					((TextView) view)
							.setText(cursor.getString(cursor
									.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)));
					return true;
				} else {
					return super.setViewValue(view, cursor, columnIndex);
				}
			} else if (colName.equals(AccountTable.KEY_ACCOUNT_NAME)) {
				if (mCurrentAccount != 0
						&& mCurrentAccount == cursor
								.getLong(cursor
										.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))) {
					((TextView) view).setText(cursor.getString(cursor
							.getColumnIndex(InfoTables.KEY_THIRD_PARTY_NAME)));
					return true;
				} else {
					return super.setViewValue(view, cursor, columnIndex);
				}
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
		mListView = (ListView) findViewById(android.R.id.list);
		registerForContextMenu(mListView);
		mTotalLbl = (TextView) findViewById(R.id.sch_op_sum_total);
		final GestureDetector gestureDetector = new GestureDetector(this,
				new ListViewSwipeDetector(mListView, new ListSwipeAction() {
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
		mListView.setOnTouchListener(gestureListener);

		ImageButton btn = (ImageButton) findViewById(R.id.add_sch_op);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startCreateScheduledOp();
			}
		});

		mAccountSpinner = (Spinner) findViewById(R.id.account_spinner);
		mCurrentAccount = getIntent().getLongExtra(CURRENT_ACCOUNT, 0);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		String[] from = new String[] { OperationTable.KEY_OP_DATE,
				InfoTables.KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_SUM,
				AccountTable.KEY_ACCOUNT_NAME,
				ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT,
				ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY,
				ScheduledOperationTable.KEY_SCHEDULED_END_DATE, };

		int[] to = new int[] { R.id.scheduled_date, R.id.scheduled_third_party,
				R.id.scheduled_sum, R.id.scheduled_account,
				R.id.scheduled_infos };
		mAdapter = new SimpleCursorAdapter(
				this,
				R.layout.scheduled_row,
				null,
				from,
				to,
				android.support.v4.widget.SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mAdapter.setViewBinder(new InnerViewBinder());
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long id) {
				startEditScheduledOperation(id);
			}
		});
	}

	private void fetchAllSchOps() {
		showProgress();
		if (mLoader == null) {
			getSupportLoaderManager().initLoader(GET_ALL_SCH_OPS, null, this);
		} else {
			getSupportLoaderManager()
					.restartLoader(GET_ALL_SCH_OPS, null, this);
		}
	}

	private void fetchSchOpsOfAccount() {
		showProgress();
		if (mLoader == null) {
			getSupportLoaderManager().initLoader(GET_SCH_OPS_OF_ACCOUNT, null,
					this);
		} else {
			getSupportLoaderManager().restartLoader(GET_SCH_OPS_OF_ACCOUNT,
					null, this);
		}
	}

	private void populateAccountSpinner(Cursor c) {
		if (c != null && c.moveToFirst()) {
			ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this,
					android.R.layout.simple_spinner_item);
			adapter.add(new Account(0, getString(R.string.all_accounts)));
			do {
				adapter.add(new Account(c));
			} while (c.moveToNext());

			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mAccountSpinner.setAdapter(adapter);
			if (mCurrentAccount != 0) {
				int pos = 0;
				while (pos < adapter.getCount()) {
					long id = adapter.getItemId(pos);
					if (id == mCurrentAccount) {
						mAccountSpinner.setSelection(pos);
						break;
					} else {
						pos++;
					}
				}
			}
		}
		mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				Account a = (Account) parent.getItemAtPosition(pos);
				mCurrentAccount = a.mAccountId;
				((InnerViewBinder) mAdapter.getViewBinder())
						.setCurAccount(mCurrentAccount);
				if (mCurrentAccount != 0) {
					fetchSchOpsOfAccount();
				} else {
					fetchAllSchOps();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// if (resultCode == RESULT_OK) {
		// switch (mLoader.getId()) {
		// case GET_ALL_SCH_OPS:
		// fetchAllSchOps();
		// break;
		// case GET_SCH_OPS_OF_ACCOUNT:
		// fetchSchOpsOfAccount();
		// break;
		// default:
		// break;
		// }
		// }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateAccountSpinner(AccountList.getAllAccounts(this));
		if (mCurrentAccount == 0) {
			fetchAllSchOps();
		} else {
			fetchSchOpsOfAccount();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE:
			return askDeleteOccurrences();
		default:
			return Tools.onDefaultCreateDialog(this, id);
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
		Cursor cursorOp = ScheduledOperationTable.fetchOneScheduledOp(this,
				op.id);
		final long transId = cursorOp.getLong(cursorOp
				.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
		if (delAllOccurrences) {
			ScheduledOperationTable.deleteAllOccurences(this, op.id);
		}
		if (ScheduledOperationTable.deleteScheduledOp(this, op.id)) {
			int req = -1;
			if (mCurrentAccount == 0) {
				req = GET_ALL_SCH_OPS;
			} else {
				req = GET_SCH_OPS_OF_ACCOUNT;
			}
			if (mLoader != null) {
				getSupportLoaderManager().restartLoader(req, null, this);
			} else {
				getSupportLoaderManager().initLoader(req, null, this);
			}
			if (transId > 0) {
				AccountTable.consolidateSums(this, transId);
			}
		}
		Log.d(TAG, "REFRESH AFTER DEL SCH OP");
		AccountList.refreshDisplay(this);
		OperationList
				.refreshDisplay(
						this,
						cursorOp.getLong(cursorOp
								.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID)));
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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case GET_ALL_SCH_OPS:
			mLoader = new CursorLoader(this,
					DbContentProvider.SCHEDULED_JOINED_OP_URI,
					ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, null,
					null, ScheduledOperationTable.SCHEDULED_OP_ORDERING);
			break;
		case GET_SCH_OPS_OF_ACCOUNT:
			mLoader = new CursorLoader(this,
					DbContentProvider.SCHEDULED_JOINED_OP_URI,
					ScheduledOperationTable.SCHEDULED_OP_COLS_QUERY, "sch."
							+ ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID
							+ " = ? OR sch."
							+ OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = ?",
					new String[] { Long.toString(mCurrentAccount),
							Long.toString(mCurrentAccount) },
					ScheduledOperationTable.SCHEDULED_OP_ORDERING);
			break;
		default:
			break;
		}
		return mLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		hideProgress();
		mAdapter.changeCursor(data);
		if (mCurrentAccount != 0) {
			computeTotal(data);
			mTotalLbl.setVisibility(View.VISIBLE);
		} else {
			mTotalLbl.setVisibility(View.GONE);
		}
	}

	private void computeTotal(Cursor data) {
		long credit = 0;
		long debit = 0;
		if (data != null && data.getCount() > 0 && data.moveToFirst()) {
			do {
				long s = data.getLong(data
						.getColumnIndex(OperationTable.KEY_OP_SUM));
				final long transId = data.getLong(data
						.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID));
				if (transId == mCurrentAccount) {
					s = -s;
				}
				if (s >= 0) {
					credit += s;
				} else {
					debit += s;
				}
			} while (data.moveToNext());
		}
		mTotalLbl.setText(String.format(getString(R.string.sched_op_total_sum),
				Formater.getSumFormater().format(credit / 100.0d), Formater
						.getSumFormater().format(debit / 100.0d), Formater
						.getSumFormater().format((credit + debit) / 100.0d)));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

}
