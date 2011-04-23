package fr.geobert.radis.service;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.PowerManager;
import android.util.Log;
import fr.geobert.radis.ConfigManager;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.Tools;

public class RadisService extends IntentService {
	public static final String LOCK_NAME_STATIC = "fr.geobert.radis.StaticLock";
	private static PowerManager.WakeLock lockStatic = null;
	private CommonDbAdapter mDbHelper;
	private SharedPreferences mPrefs;

	public RadisService() {
		super("RadisService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			mPrefs = getSharedPreferences(ConfigManager.SHARED_PREF_NAME,
					Activity.MODE_PRIVATE);
			mDbHelper = CommonDbAdapter.getInstance(getApplicationContext());
			mDbHelper.open();
			processScheduledOps();
			//mDbHelper.close();
		} finally {
			getLock(this).release();
			//mDbHelper.close();
		}
		stopSelf();
	}

	private void processScheduledOps() {
		Cursor c = mDbHelper.fetchAllScheduledOps();
		if (c.isFirst()) {
			GregorianCalendar today = new GregorianCalendar();
			Tools.clearTimeOfCalendar(today);
			final long todayInMillis = today.getTimeInMillis();

			GregorianCalendar insertionDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(insertionDate);
			insertionDate.set(Calendar.DAY_OF_MONTH,
					mPrefs.getInt(ConfigManager.INSERTION_DATE, 20));
			final long insertionDateInMillis = insertionDate.getTimeInMillis();
			final int insertionMonth = insertionDate.get(Calendar.MONTH);

			HashMap<Long, Double> sumsPerAccount = new LinkedHashMap<Long, Double>();
			HashMap<Long, Long> greatestDatePerAccount = new LinkedHashMap<Long, Long>();

			do {
				final long opRowId = c.getLong(c.getColumnIndex("_id"));
				ScheduledOperation op = new ScheduledOperation(c);
				final Long accountId = Long.valueOf(op.mAccountId);
				double sum = 0;
				boolean needUpdate = false;
				if (!op.isObsolete(todayInMillis)) {
					// insert past missed insertion
					while (op.getDate() <= insertionDateInMillis) {
						sum = sum + insertSchOp(op, opRowId);
						needUpdate = true;
					}
					if (todayInMillis >= insertionDateInMillis) {
						while (op.getMonth() <= (insertionMonth + 1)) {
							Long date = greatestDatePerAccount.get(accountId);
							if (null == date) {
								date = Long.valueOf(0);
							}
							if (op.getDate() > date) {
								greatestDatePerAccount.put(accountId,
										op.getDate());
							}
							sum = sum + insertSchOp(op, opRowId);
							needUpdate = true;
						}
					}
					if (needUpdate) {
						mDbHelper.updateScheduledOp(opRowId, op, false);
					}
				}
				if (needUpdate) {
					Double curSum = sumsPerAccount.get(accountId);
					if (curSum == null) {
						curSum = Double.valueOf(0);
					}
					sumsPerAccount.put(accountId, curSum + sum);
				}
			} while (c.moveToNext());
			boolean needUpdate = false;
			Long[] accountIds = sumsPerAccount.keySet().toArray(
					new Long[sumsPerAccount.size()]);
			for (HashMap.Entry<Long, Double> e : sumsPerAccount.entrySet()) {
				needUpdate = true;
				updateAccountSum(e.getValue().doubleValue(), e.getKey()
						.longValue(), greatestDatePerAccount.get(e.getKey()));
			}
			if (needUpdate) {
				Intent i = new Intent(Tools.INTENT_OP_INSERTED);
				i.putExtra("accountIds", accountIds);
				sendOrderedBroadcast(i, null);
			}
		}
		c.close();
	}

	private void updateAccountSum(final double sumToAdd, final long accountId,
			final long date) {
		Cursor accountCursor = mDbHelper.fetchAccount(accountId);
		double curSum = accountCursor.getDouble(accountCursor
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
		mDbHelper.updateOpSum(accountId, curSum + sumToAdd);
		final long curDate = accountCursor.getLong(accountCursor
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE));
		if (date > curDate) {
			mDbHelper.updateCurrentSum(accountId, date);
		} else {
			mDbHelper.updateCurrentSum(accountId, 0);
		}
		accountCursor.close();
	}

	private double insertSchOp(ScheduledOperation op, final long opRowId) {
		final long accountId = op.mAccountId;
		op.mScheduledId = opRowId;
		mDbHelper.createOp(op, accountId);
		ScheduledOperation.addPeriodicityToDate(op);
		Log.d("Radis", String.format("inserted op %s", op.mThirdParty));
		return op.mSum;
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
