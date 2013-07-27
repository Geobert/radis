package fr.geobert.radis.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.PowerManager;
import android.util.Log;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class RadisService extends IntentService {
    private static final String TAG = "RadisService";
    public static final String LOCK_NAME_STATIC = "fr.geobert.radis.StaticLock";
    private static PowerManager.WakeLock lockStatic = null;

    public static void callMe(Context context) {
        RadisService.acquireStaticLock(context);
        context.startService(new Intent(context, RadisService.class));
    }

    public RadisService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            DBPrefsManager.getInstance(this).fillCache(this);
            InfoTables.fillCachesSync(this);
            processScheduledOps();
        } finally {
            if (getLock(this).isHeld()) {
                Log.d(TAG, "release lock");
                getLock(this).release();
            }
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

    private class TimeParams {
        long today = 0;
        long insertionDate = 0;
        long currentMonth = 0;
        long limitInsertionDate = 0;
    }

    private TimeParams computeTimeParameters() {
        DateFormat formater = Formater.getFullDateFormater(); // used in Log.d
        TimeParams res = new TimeParams();
        GregorianCalendar today = Tools.createClearedCalendar();
        final long todayInMillis = today.getTimeInMillis();
//        Log.d(TAG, "today : " + Formater.getFullDateFormater().format(today.getTime()));

        GregorianCalendar currentMonth = new GregorianCalendar();
        currentMonth.setTimeInMillis(todayInMillis);
        Tools.clearTimeOfCalendar(currentMonth);
        currentMonth.set(Calendar.DAY_OF_MONTH,
                currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
//        Log.d(TAG, "currentMonth : " + formater.format(currentMonth.getTime()));

        GregorianCalendar insertionDate = new GregorianCalendar();
        int insertionDayOfMonth = DBPrefsManager
                .getInstance(this)
                .getInt(RadisConfiguration.KEY_INSERTION_DATE,
                        Integer.parseInt(RadisConfiguration.DEFAULT_INSERTION_DATE));
        final int maxDayOfCurMonth = today
                .getActualMaximum(Calendar.DAY_OF_MONTH);
//        Log.d(TAG, "maxDayOfCurMonth : " + maxDayOfCurMonth);
//        Log.d(TAG, "insertionDayOfMonth : " + insertionDayOfMonth);
        // manage February if insertionDayOfMonth is 29, 30 or 31
        insertionDayOfMonth = insertionDayOfMonth > maxDayOfCurMonth ? maxDayOfCurMonth
                : insertionDayOfMonth;
//        Log.d(TAG, "final insertionDayOfMonth : " + insertionDayOfMonth);

        insertionDate.set(Calendar.DAY_OF_MONTH, insertionDayOfMonth);
        Tools.clearTimeOfCalendar(insertionDate);
//        Log.d(TAG, "insertionDate : " + Formater.getFullDateFormater().format(insertionDate.getTime()));

        if (today.compareTo(insertionDate) > 0) {
            insertionDate.add(Calendar.MONTH, 1);
//            Log.d(TAG, "adjusted insertionDate : " + Formater.getFullDateFormater().format(insertionDate.getTime()));
        }


        long insertionDateInMillis = insertionDate.getTimeInMillis();

        GregorianCalendar limitInsertionDate = new GregorianCalendar();
        limitInsertionDate.setTimeInMillis(insertionDateInMillis);
        limitInsertionDate.add(Calendar.MONTH, 1);
//        Log.d(TAG, "limitInsertionDate : " + Formater.getFullDateFormater().format(limitInsertionDate.getTime()));

        final long lastInsertDate = DBPrefsManager.getInstance(this).getLong(
                RadisConfiguration.KEY_LAST_INSERTION_DATE, 0);
//        Log.d(TAG, "lastInsertDate : " + Formater.getFullDateFormater().format(lastInsertDate));
        if (lastInsertDate > insertionDateInMillis) {
            insertionDate.add(Calendar.MONTH, 1);
//            Log.d(TAG, "modified insertionDate : " + Formater.getFullDateFormater().format(insertionDate.getTime()));
            insertionDateInMillis = insertionDate.getTimeInMillis();
        }
        limitInsertionDate.set(Calendar.DAY_OF_MONTH,
                limitInsertionDate.getActualMaximum(Calendar.DAY_OF_MONTH));
//        Log.d(TAG, "final limitInsertionDate : " + Formater.getFullDateFormater().format(limitInsertionDate.getTime()));
        res.today = todayInMillis;
        res.currentMonth = currentMonth.getTimeInMillis();
        res.insertionDate = insertionDateInMillis;
        res.limitInsertionDate = limitInsertionDate.getTimeInMillis();
        return res;
    }

    private synchronized void processScheduledOps() {
        Cursor c = ScheduledOperationTable.fetchAllScheduledOps(this);
        if (c.isFirst()) {
            DateFormat formater = Formater.getFullDateFormater(); // used in Log.d
            TimeParams p = computeTimeParameters();

            HashMap<Long, Long> sumsPerAccount = new LinkedHashMap<Long, Long>();
            HashMap<Long, Long> greatestDatePerAccount = new LinkedHashMap<Long, Long>();

            do {
                final long opRowId = c.getLong(c.getColumnIndex("_id"));
                ScheduledOperation op = new ScheduledOperation(c);
                final Long accountId = Long.valueOf(op.mAccountId);
                final Long transId = Long.valueOf(op.mTransferAccountId);
                long sum = 0;
                boolean needUpdate = false;
                Cursor accountCursor = AccountTable.fetchAccount(this,
                        accountId);
                if (null != accountCursor) {
                    if (!accountCursor.moveToFirst()) {
                        c.moveToNext();
                        continue;
                    } else {
                        AccountTable.initProjectionDate(accountCursor);
                    }
                    accountCursor.close();
                } else {
                    c.moveToNext();
                    continue;
                }

                // insert all scheduled of the past until current month
//                Log.d(TAG, "insert all scheduled of the past until current month");
                int i = 0; // for logging purpose
                while (op.getDate() <= p.currentMonth && !op.isObsolete()) {
                    long opSum = insertSchOp(op, opRowId);
                    i++;
                    if (opSum != 0) {
                        sum = sum + opSum;
                        needUpdate = true;
                    }
                }
//                Log.d(TAG, "inserted " + i + " past scheduled op until current month");
                i = 0;
                if (p.today >= p.insertionDate) {
                    while (op.getDate() < p.limitInsertionDate && !op.isObsolete()) {
                        keepGreatestDate(greatestDatePerAccount, accountId, op.getDate());
                        if (transId > 0) {
                            keepGreatestDate(greatestDatePerAccount, transId, op.getDate());
                        }
//                        Log.d(TAG, "op month before : " + op.getMonth());
                        long opSum = insertSchOp(op, opRowId);
//                        Log.d(TAG, "op month after : " + op.getMonth());
                        i++;
                        if (opSum != 0) {
                            sum = sum + opSum;
                            needUpdate = true;
                        }
                    }
//                    Log.d(TAG, "inserted " + i + " ops current month scheduled op");
                }
                ScheduledOperationTable.updateScheduledOp(this, opRowId, op, false);
                if (needUpdate) {
                    Long curSum = sumsPerAccount.get(accountId);
                    if (curSum == null) {
                        curSum = Long.valueOf(0);
                    }
                    sumsPerAccount.put(accountId, curSum + sum);
                    keepGreatestDate(greatestDatePerAccount, accountId, op.getDate());
                    // the sch op is a transfert, update the dst account sum with -sum
                    if (transId > 0) {
                        curSum = sumsPerAccount.get(transId);
                        if (curSum == null) {
                            curSum = Long.valueOf(0);
                        }
                        sumsPerAccount.put(transId, curSum - sum);
                        keepGreatestDate(greatestDatePerAccount, transId, op.getDate());
                    }
                }
            } while (c.moveToNext());
            boolean needUpdate = false;
            Long[] accountIds = sumsPerAccount.keySet().toArray(new Long[sumsPerAccount.size()]);
            for (HashMap.Entry<Long, Long> e : sumsPerAccount.entrySet()) {
                needUpdate = true;
                updateAccountSum(e.getValue().longValue(), e.getKey().longValue(),
                        greatestDatePerAccount.get(e.getKey()), this);
            }
//            Log.d(TAG, "DOES NEED UPDATE : " + needUpdate);
            if (needUpdate) {
                Intent i = new Intent(Tools.INTENT_OP_INSERTED);
                i.putExtra("accountIds", accountIds);
//                Log.d(TAG, "sendOrderedBroadcast" + i.toString());
                sendOrderedBroadcast(i, null);
            }
//            Log.d(TAG, "save LAST_INSERT_DATE is todayInMillis: " + Formater.getFullDateFormater().format(p.today));
            DBPrefsManager.getInstance(this).put(RadisConfiguration.KEY_LAST_INSERTION_DATE, p.today);
        }
        c.close();
    }

    public static void updateAccountSum(final long sumToAdd, final long accountId,
                                        final long opDate, final Context ctx) {
        AccountTable.updateProjection(ctx, accountId, sumToAdd, opDate);
    }

    private long insertSchOp(ScheduledOperation op, final long opRowId) {
        final long accountId = op.mAccountId;
        op.mScheduledId = opRowId;
        boolean needUpdate = OperationTable.createOp(this, op, accountId, false);
//        Log.d(TAG, "before addPeriodicity : " + op.getDateStr());
        ScheduledOperation.addPeriodicityToDate(op);
//        Log.d(TAG, "after addPeriodicity : " + op.getDateStr());
        return needUpdate ? op.mSum : 0;
    }

    public static void acquireStaticLock(Context context) {
        Log.d(TAG, "acquireStaticLock");
        getLock(context).acquire();
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);
            lockStatic.setReferenceCounted(true);
        }
        return lockStatic;
    }

}
