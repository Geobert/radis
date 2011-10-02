package fr.geobert.radis.service;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.PowerManager;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Tools;

public class RadisService extends IntentService {
	public static final String LOCK_NAME_STATIC = "fr.geobert.radis.StaticLock";
	private static PowerManager.WakeLock lockStatic = null;
	private CommonDbAdapter mDbHelper;

	public RadisService() {
		super("RadisService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			mDbHelper = CommonDbAdapter.getInstance(getApplicationContext());
			assert null != mDbHelper;
			mDbHelper.open();
			processScheduledOps();
			// mDbHelper.close();
		} finally {
			getLock(this).release();
			// mDbHelper.close();
		}
		stopSelf();
	}

	private void keepGreatestDate(HashMap<Long, Long> greatestDatePerAccount,
			final long accountId, final long curOpDate) {
		Long date = greatestDatePerAccount.get(accountId);
		if (null == date) {
			date = Long.valueOf(0);
		}
		if (curOpDate > date) {
			greatestDatePerAccount.put(accountId, curOpDate);
		}
	}

	private void processScheduledOps() {
		Cursor c = mDbHelper.fetchAllScheduledOps();
		if (c.isFirst()) {
			GregorianCalendar today = new GregorianCalendar();
			Tools.clearTimeOfCalendar(today);
			final long todayInMillis = today.getTimeInMillis();

			GregorianCalendar insertionDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(insertionDate);
			int insertionDayOfMonth = DBPrefsManager
					.getInstance(this)
					.getInt(RadisConfiguration.KEY_INSERTION_DATE,
							Integer.parseInt(RadisConfiguration.DEFAULT_INSERTION_DATE))
					.intValue();
			final int maxDayOfCurMonth = today
					.getActualMaximum(Calendar.DAY_OF_MONTH);
			insertionDayOfMonth = insertionDayOfMonth > maxDayOfCurMonth ? maxDayOfCurMonth
					: insertionDayOfMonth;
			insertionDate.set(Calendar.DAY_OF_MONTH, insertionDayOfMonth);

			Tools.clearTimeOfCalendar(insertionDate);
			long insertionDateInMillis = insertionDate.getTimeInMillis();
			final int insertionMonthLimit = insertionDate.get(Calendar.MONTH) + 1;

			long lastInsertDate = DBPrefsManager.getInstance(this).getLong(
					"LAST_INSERT_DATE", 0);
			// Log.d("Radis", "lastInsertDate: " + lastInsertDate);
			// Log.d("Radis", "insertionDateInMillis: " +
			// insertionDateInMillis);
			if (lastInsertDate > insertionDateInMillis) {
				insertionDate.add(Calendar.MONTH, 1);
				insertionDateInMillis = insertionDate.getTimeInMillis();
			}

			HashMap<Long, Long> sumsPerAccount = new LinkedHashMap<Long, Long>();
			HashMap<Long, Long> greatestDatePerAccount = new LinkedHashMap<Long, Long>();

			do {
				final long opRowId = c.getLong(c.getColumnIndex("_id"));
				ScheduledOperation op = new ScheduledOperation(c);
				final Long accountId = Long.valueOf(op.mAccountId);
				long sum = 0;
				boolean needUpdate = false;
				Cursor accountCursor = mDbHelper.fetchAccount(accountId);
				if (null == accountCursor || !accountCursor.moveToFirst()) {
					c.moveToNext();
					continue;
				}
				
				// insert all scheduled of the past until current month
				while (op.getMonth() <= today.get(Calendar.MONTH)
						&& !op.isObsolete()) {
					long opSum = insertSchOp(op, opRowId);
					if (opSum != 0) {
						sum = sum + opSum;
						needUpdate = true;
					}
				}
				if (todayInMillis >= insertionDateInMillis) {
					while (op.getMonth() <= insertionMonthLimit
							&& !op.isObsolete()) {
						keepGreatestDate(greatestDatePerAccount, accountId,
								op.getDate());
						long opSum = insertSchOp(op, opRowId);
						if (opSum != 0) {
							sum = sum + opSum;
							needUpdate = true;
						}
					}
				}
				mDbHelper.updateScheduledOp(opRowId, op, false);
				if (needUpdate) {
					Long curSum = sumsPerAccount.get(accountId);
					if (curSum == null) {
						curSum = Long.valueOf(0);
					}
					sumsPerAccount.put(accountId, curSum + sum);
					keepGreatestDate(greatestDatePerAccount, accountId,
							op.getDate());
				}
			} while (c.moveToNext());
			boolean needUpdate = false;
			Long[] accountIds = sumsPerAccount.keySet().toArray(
					new Long[sumsPerAccount.size()]);
			for (HashMap.Entry<Long, Long> e : sumsPerAccount.entrySet()) {
				needUpdate = true;
				updateAccountSum(e.getValue().longValue(), e.getKey()
						.longValue(), greatestDatePerAccount.get(e.getKey()),
						mDbHelper);
			}
			if (needUpdate) {
				Intent i = new Intent(Tools.INTENT_OP_INSERTED);
				i.putExtra("accountIds", accountIds);
				sendOrderedBroadcast(i, null);
			}
			// Log.d("Radis", "put todayInMillis: " + todayInMillis);
			DBPrefsManager.getInstance(this).put("LAST_INSERT_DATE",
					todayInMillis);
			if (lastInsertDate == 0) {
				processScheduledOps();
			}
		}
		c.close();
	}

	public static void updateAccountSum(final long sumToAdd,
			final long accountId, final long opDate, CommonDbAdapter dbHelper) {
		dbHelper.updateProjection(accountId, sumToAdd, opDate);
	}

	private long insertSchOp(ScheduledOperation op, final long opRowId) {
		final long accountId = op.mAccountId;
		op.mScheduledId = opRowId;
		boolean needUpdate = mDbHelper.createOp(op, accountId);
		ScheduledOperation.addPeriodicityToDate(op);
		// Log.d("Radis", String.format("inserted op %s", op.mThirdParty));
		return needUpdate ? op.mSum : 0;
	}

	public static void acquireStaticLock(Context context) {
		getLock(context).acquire();
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					LOCK_NAME_STATIC);
			lockStatic.setReferenceCounted(true);
		}

		return lockStatic;
	}

}
