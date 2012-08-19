package fr.geobert.radis.service;

import java.util.HashMap;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;

public class RadisService extends IntentService {
	public static final String LOCK_NAME_STATIC = "fr.geobert.radis.StaticLock";
	private static PowerManager.WakeLock lockStatic = null;

	// private CommonDbAdapter mDbHelper;

	public RadisService() {
		super("RadisService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			// mDbHelper = CommonDbAdapter.getInstance(getApplicationContext());
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
		// Cursor c = mDbHelper.fetchAllScheduledOps();
		// if (c.isFirst()) {
		// GregorianCalendar today = new GregorianCalendar();
		// Tools.clearTimeOfCalendar(today);
		// final long todayInMillis = today.getTimeInMillis();
		// Log.d("RadisService",
		// "today : "
		// + Formater.getFullDateFormater(
		// getApplicationContext()).format(
		// today.getTime()));
		//
		// GregorianCalendar currentMonth = new GregorianCalendar();
		// currentMonth.setTimeInMillis(todayInMillis);
		// Tools.clearTimeOfCalendar(currentMonth);
		// currentMonth.set(Calendar.DAY_OF_MONTH,
		// currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
		// Log.d("RadisService", "currentMonth : "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(currentMonth.getTime()));
		//
		// GregorianCalendar insertionDate = new GregorianCalendar();
		// int insertionDayOfMonth = DBPrefsManager
		// .getInstance(this)
		// .getInt(RadisConfiguration.KEY_INSERTION_DATE,
		// Integer.parseInt(RadisConfiguration.DEFAULT_INSERTION_DATE))
		// .intValue();
		// final int maxDayOfCurMonth = today
		// .getActualMaximum(Calendar.DAY_OF_MONTH);
		// Log.d("RadisService", "maxDayOfCurMonth : " + maxDayOfCurMonth);
		// Log.d("RadisService", "insertionDayOfMonth : "
		// + insertionDayOfMonth);
		// // manage February if insertionDayOfMonth is 29, 30 or 31
		// insertionDayOfMonth = insertionDayOfMonth > maxDayOfCurMonth ?
		// maxDayOfCurMonth
		// : insertionDayOfMonth;
		// Log.d("RadisService", "final insertionDayOfMonth : "
		// + insertionDayOfMonth);
		//
		// insertionDate.set(Calendar.DAY_OF_MONTH, insertionDayOfMonth);
		// Tools.clearTimeOfCalendar(insertionDate);
		// Log.d("RadisService", "insertionDate : "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(insertionDate.getTime()));
		//
		// long insertionDateInMillis = insertionDate.getTimeInMillis();
		//
		// GregorianCalendar limitInsertionDate = new GregorianCalendar();
		// limitInsertionDate.setTimeInMillis(insertionDateInMillis);
		// limitInsertionDate.add(Calendar.MONTH, 1);
		// Log.d("RadisService", "limitInsertionDate : "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(limitInsertionDate.getTime()));
		//
		// final long lastInsertDate = DBPrefsManager.getInstance(this)
		// .getLong(RadisConfiguration.KEY_LAST_INSERTION_DATE, 0);
		// Log.d("RadisService", "lastInsertDate : "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(lastInsertDate));
		// if (lastInsertDate > insertionDateInMillis) {
		// insertionDate.add(Calendar.MONTH, 1);
		// Log.d("RadisService", "modified insertionDate : "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(insertionDate.getTime()));
		// insertionDateInMillis = insertionDate.getTimeInMillis();
		// }
		// limitInsertionDate.set(Calendar.DAY_OF_MONTH,
		// limitInsertionDate.getActualMaximum(Calendar.DAY_OF_MONTH));
		// Log.d("RadisService", "final limitInsertionDate : "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(limitInsertionDate.getTime()));
		//
		// HashMap<Long, Long> sumsPerAccount = new LinkedHashMap<Long, Long>();
		// HashMap<Long, Long> greatestDatePerAccount = new LinkedHashMap<Long,
		// Long>();
		//
		// do {
		// final long opRowId = c.getLong(c.getColumnIndex("_id"));
		// ScheduledOperation op = new ScheduledOperation(c);
		// final Long accountId = Long.valueOf(op.mAccountId);
		// final Long transId = Long.valueOf(op.mTransferAccountId);
		// long sum = 0;
		// boolean needUpdate = false;
		// Cursor accountCursor = mDbHelper.fetchAccount(accountId);
		// if (null != accountCursor) {
		// if (!accountCursor.moveToFirst()) {
		// c.moveToNext();
		// continue;
		// }
		// accountCursor.close();
		// } else {
		// c.moveToNext();
		// continue;
		// }
		//
		// // insert all scheduled of the past until current month
		// Log.d("RadisService",
		// "insert all scheduled of the past until current month");
		// int i = 0; // for logging purpose
		// while (op.getDate() <= currentMonth.getTimeInMillis()
		// && !op.isObsolete()) {
		// long opSum = insertSchOp(op, opRowId);
		// i++;
		// if (opSum != 0) {
		// sum = sum + opSum;
		// needUpdate = true;
		// }
		// }
		// Log.d("RadisService", "inserted " + i
		// + " past scheduled op until current month");
		// i = 0;
		// if (todayInMillis >= insertionDateInMillis) {
		// while (op.getDate() < limitInsertionDate.getTimeInMillis()
		// && !op.isObsolete()) {
		// keepGreatestDate(greatestDatePerAccount, accountId,
		// op.getDate());
		// Log.d("RadisService",
		// "op month before : " + op.getMonth());
		// long opSum = insertSchOp(op, opRowId);
		// Log.d("RadisService",
		// "op month after : " + op.getMonth());
		// i++;
		// if (opSum != 0) {
		// sum = sum + opSum;
		// needUpdate = true;
		// }
		// }
		// Log.d("RadisService", "inserted " + i
		// + " ops current month scheduled op");
		// }
		// mDbHelper.updateScheduledOp(opRowId, op, false);
		// if (needUpdate) {
		// Long curSum = sumsPerAccount.get(accountId);
		// if (curSum == null) {
		// curSum = Long.valueOf(0);
		// }
		// sumsPerAccount.put(accountId, curSum + sum);
		// keepGreatestDate(greatestDatePerAccount, accountId,
		// op.getDate());
		// // the sch op is a transfert, update the dst account sum
		// // with -sum
		// if (transId > 0) {
		// curSum = sumsPerAccount.get(transId);
		// if (curSum == null) {
		// curSum = Long.valueOf(0);
		// }
		// sumsPerAccount.put(transId, curSum - sum);
		// keepGreatestDate(greatestDatePerAccount, transId,
		// op.getDate());
		// }
		// }
		// } while (c.moveToNext());
		// boolean needUpdate = false;
		// Long[] accountIds = sumsPerAccount.keySet().toArray(
		// new Long[sumsPerAccount.size()]);
		// for (HashMap.Entry<Long, Long> e : sumsPerAccount.entrySet()) {
		// needUpdate = true;
		// updateAccountSum(e.getValue().longValue(), e.getKey()
		// .longValue(), greatestDatePerAccount.get(e.getKey()),
		// mDbHelper);
		// }
		// if (needUpdate) {
		// Intent i = new Intent(Tools.INTENT_OP_INSERTED);
		// i.putExtra("accountIds", accountIds);
		// sendOrderedBroadcast(i, null);
		// }
		// Log.d("RadisService", "save LAST_INSERT_DATE is todayInMillis: "
		// + Formater.getFullDateFormater(getApplicationContext())
		// .format(todayInMillis));
		// DBPrefsManager.getInstance(this).put(
		// RadisConfiguration.KEY_LAST_INSERTION_DATE, todayInMillis);
		// // if (lastInsertDate == 0) {
		// // processScheduledOps();
		// // }
		// }
		// c.close();
	}

	public static void updateAccountSum(final long sumToAdd,
			final long accountId, final long opDate, final Context ctx) {
		AccountTable.updateProjection(ctx, accountId, sumToAdd, opDate);
	}

	private long insertSchOp(ScheduledOperation op, final long opRowId) {
		final long accountId = op.mAccountId;
		op.mScheduledId = opRowId;
		// boolean needUpdate = mDbHelper.createOp(op, accountId);
		// Log.d("RadisService", "before addPeriodicity : " + op.getDateStr());
		// ScheduledOperation.addPeriodicityToDate(op);
		// Log.d("RadisService", "after addPeriodicity : " + op.getDateStr());
		// return needUpdate ? op.mSum : 0;
		return 0;
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
