package fr.geobert.radis.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.PowerManager;
import android.util.Log;
import fr.geobert.radis.MainActivity;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.data.ScheduledOperation;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.tools.DBPrefsManager;
import fr.geobert.radis.tools.PrefsManager;
import fr.geobert.radis.tools.Tools;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class RadisService extends IntentService {
    private static final String TAG = "RadisService";
    public static final String LOCK_NAME_STATIC = "fr.geobert.radis.StaticLock";
    public static final String CONSOLIDATE_DB = "consolidateDB";
    private static PowerManager.WakeLock lockStatic = null;

    public static void callMe(Context context) {
        RadisService.acquireStaticLock(context);
        context.startService(new Intent(context, RadisService.class));
    }

    public RadisService() {
        super(TAG);
    }

    private void consolidateDbAfterRestore() {
        PrefsManager prefs = PrefsManager.getInstance(this);
        Boolean needConsolidate = prefs.getBoolean(CONSOLIDATE_DB, false);
        Log.d(TAG, "needConsolidate : " + needConsolidate);
        if (needConsolidate) {
            Cursor cursor = AccountTable.fetchAllAccounts(this);
            prefs.put(CONSOLIDATE_DB, false);
            prefs.commit();
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        Log.d(TAG, "CONSOLIDATE ON consolidateDB set to true : " + cursor.getLong(0));
                        AccountTable.consolidateSums(this, cursor.getLong(0));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            MainActivity.refreshAccountList(this);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            DBPrefsManager prefs = DBPrefsManager.getInstance(this);
            prefs.fillCache(this);
            processScheduledOps();
        } finally {
            if (getLock(this).isHeld()) {
                Log.d(TAG, "release lock");
                getLock(this).release();
            }
            // If the DB was restored, we consolidate the sums
            consolidateDbAfterRestore();
        }
        stopSelf();
    }

    private void keepGreatestDate(HashMap<Long, Long> greatestDatePerAccount,
                                  final long accountId, final long curOpDate) {
        Long date = greatestDatePerAccount.get(accountId);
        if (null == date) {
            date = (long) 0;
        }
        if (curOpDate > date) {
            greatestDatePerAccount.put(accountId, curOpDate);
        }
    }

//    private class TimeParams {
//        long today = 0;
//        long insertionDate = 0;
//        long currentMonth = 0;
//        long limitInsertionDate = 0;
//    }

//    private TimeParams computeTimeParameters() {
//        //DateFormat formater = Formater.getFullDateFormater(); // used in Log.d
//        GregorianCalendar today = Tools.createClearedCalendar();
//        final long todayInMillis = today.getTimeInMillis();
////        Log.d(TAG, "today : " + Formater.getFullDateFormater().format(today.getTime()));
//
//        GregorianCalendar currentMonth = new GregorianCalendar();
//        currentMonth.setTimeInMillis(todayInMillis);
//        currentMonth.set(Calendar.DAY_OF_MONTH, currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
////        Log.d(TAG, "currentMonth : " + formater.format(currentMonth.getTime()));
//
//        GregorianCalendar insertionDate = new GregorianCalendar();
//        final int maxDayOfCurMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH);
//        // manage February if insertionDayOfMonth is 29, 30 or 31
//        final int cfgInsertDay = DBPrefsManager.getInstance(this).getInt(RadisConfiguration.KEY_INSERTION_DATE,
//                Integer.parseInt(RadisConfiguration.DEFAULT_INSERTION_DATE));
//        final int insertionDayOfMonth = cfgInsertDay > maxDayOfCurMonth ? maxDayOfCurMonth : cfgInsertDay;
//        insertionDate.set(Calendar.DAY_OF_MONTH, insertionDayOfMonth);
//        Tools.clearTimeOfCalendar(insertionDate);
////        Log.d(TAG, "insertionDate : " + Formater.getFullDateFormater().format(insertionDate.getTime()));
//
//        if (today.compareTo(insertionDate) > 0) { // what is that for?
//            insertionDate.add(Calendar.MONTH, 1);
////            Log.d(TAG, "adjusted insertionDate : " + Formater.getFullDateFormater().format(insertionDate.getTime()));
//        }
//
//        long insertionDateInMillis = insertionDate.getTimeInMillis();
//        GregorianCalendar limitInsertionDate = new GregorianCalendar();
//        limitInsertionDate.setTimeInMillis(insertionDateInMillis);
//        limitInsertionDate.add(Calendar.MONTH, 1);
////        Log.d(TAG, "limitInsertionDate : " + Formater.getFullDateFormater().format(limitInsertionDate.getTime()));
//
//        final long lastInsertDate =
//                DBPrefsManager.getInstance(this).getLong(RadisConfiguration.KEY_LAST_INSERTION_DATE, 0);
////        Log.d(TAG, "lastInsertDate : " + Formater.getFullDateFormater().format(lastInsertDate));
//        if (lastInsertDate > insertionDateInMillis) {
//            insertionDate.add(Calendar.MONTH, 1);
////            Log.d(TAG, "modified insertionDate : " + Formater.getFullDateFormater().format(insertionDate.getTime()));
//            insertionDateInMillis = insertionDate.getTimeInMillis();
//        }
//        limitInsertionDate.set(Calendar.DAY_OF_MONTH, limitInsertionDate.getActualMaximum(Calendar.DAY_OF_MONTH));
////        Log.d(TAG, "final limitInsertionDate : " + Formater.getFullDateFormater().format(limitInsertionDate.getTime()));
//        TimeParams res = new TimeParams();
//        res.today = todayInMillis;
//        res.currentMonth = currentMonth.getTimeInMillis();
//        res.insertionDate = insertionDateInMillis;
//        res.limitInsertionDate = limitInsertionDate.getTimeInMillis();
//        return res;
//    }

    private synchronized void processScheduledOps() {
        Cursor schOpsCursor = ScheduledOperationTable.fetchAllScheduledOps(this);
        if (schOpsCursor.isFirst()) {
//            DateFormat formater = Formater.getFullDateFormater(); // used in Log.d
            TimeParams timeParams = TimeParams.object$.computeTimeParams(this);

            HashMap<Long, Long> sumsPerAccount = new LinkedHashMap<Long, Long>();
            HashMap<Long, Long> greatestDatePerAccount = new LinkedHashMap<Long, Long>();
            ScheduledOperation op;
            do {
                final long OP_ROW_ID = schOpsCursor.getLong(schOpsCursor.getColumnIndex("_id"));
                op = new ScheduledOperation(schOpsCursor);
                final Long accountId = op.mAccountId;
                final Long transId = op.mTransferAccountId;
                long sum = 0;
                boolean needUpdate = false;

                Cursor accountCursor = AccountTable.fetchAccount(this, accountId);
//                if (null != accountCursor) { // IntelliJ tells me it is always true
                if (!accountCursor.moveToFirst()) {
                    schOpsCursor.moveToNext();
                    continue;
                } else {
                    AccountTable.initProjectionDate(accountCursor);
                }
                accountCursor.close();
//                } else {
//                    schOpsCursor.moveToNext();
//                    continue;
//                }

                // insert all scheduled of the past until current month
//                Log.d(TAG, "insert all scheduled of the past until current month");
//                int i = 0; // for logging purpose
                while (op.getDate() <= timeParams.getCurrentMonth() && !op.isObsolete()) {
                    long opSum = insertSchOp(op, OP_ROW_ID);
//                    i++;
                    if (opSum != 0) {
                        sum = sum + opSum;
                        needUpdate = true;
                    }
                }
//                Log.d(TAG, "inserted " + i + " past scheduled op until current month");
//                i = 0;
                if (timeParams.getToday() >= timeParams.getInsertionDate()) { // it's time to insert scheduled ops
                    while (op.getDate() < timeParams.getLimitInsertionDate() && !op.isObsolete()) {
                        keepGreatestDate(greatestDatePerAccount, accountId, op.getDate());
                        if (transId > 0) {
                            keepGreatestDate(greatestDatePerAccount, transId, op.getDate());
                        }
//                        Log.d(TAG, "op month before : " + op.getMonth());
                        long opSum = insertSchOp(op, OP_ROW_ID);
//                        Log.d(TAG, "op month after : " + op.getMonth());
//                        i++;
                        if (opSum != 0) {
                            sum = sum + opSum;
                            needUpdate = true;
                        }
                    }
//                    Log.d(TAG, "inserted " + i + " ops current month scheduled op");
                }
                ScheduledOperationTable.updateScheduledOp(this, OP_ROW_ID, op, false);
                if (needUpdate) {
                    Long curSum = sumsPerAccount.get(accountId);
                    if (curSum == null) {
                        curSum = (long) 0;
                    }
                    sumsPerAccount.put(accountId, curSum + sum);
                    keepGreatestDate(greatestDatePerAccount, accountId, op.getDate());
                    // the sch op is a transfert, update the dst account sum with -sum
                    if (transId > 0) {
                        curSum = sumsPerAccount.get(transId);
                        if (curSum == null) {
                            curSum = (long) 0;
                        }
                        sumsPerAccount.put(transId, curSum - sum);
                        keepGreatestDate(greatestDatePerAccount, transId, op.getDate());
                    }
                }
            } while (schOpsCursor.moveToNext());

            boolean needUpdate = false;
            Long[] accountIds = sumsPerAccount.keySet().toArray(new Long[sumsPerAccount.size()]);
            for (HashMap.Entry<Long, Long> e : sumsPerAccount.entrySet()) {
                needUpdate = true;
                updateAccountSum(e.getValue(), 0, e.getKey(),
                        greatestDatePerAccount.get(e.getKey()), this);
            }
//            Log.d(TAG, "DOES NEED UPDATE : " + needUpdate);
            if (needUpdate) {
                Intent i = new Intent(Tools.INTENT_REFRESH_NEEDED);
                i.putExtra("accountIds", accountIds);
//                Log.d(TAG, "sendOrderedBroadcast" + i.toString());
                sendOrderedBroadcast(i, null);
            }
//            Log.d(TAG, "save LAST_INSERT_DATE is todayInMillis: " + Formater.getFullDateFormater().format(p.today));
            DBPrefsManager.getInstance(this).put(RadisConfiguration.KEY_LAST_INSERTION_DATE, timeParams.getToday());
        }
        schOpsCursor.close();
    }

    public static void updateAccountSum(final long opSum, final long oldSum, final long accountId,
                                        final long opDate, final Context ctx) {
        AccountTable.updateProjection(ctx, accountId, opSum, oldSum, opDate, -2);
    }

    private long insertSchOp(ScheduledOperation op, final long opRowId) {
        final long accountId = op.mAccountId;
        op.mScheduledId = opRowId;
        boolean needUpdate = OperationTable.createOp(this, op, accountId, false) > -1;
        ScheduledOperation.addPeriodicityToDate(op);
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
