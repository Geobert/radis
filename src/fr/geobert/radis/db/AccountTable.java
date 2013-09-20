package fr.geobert.radis.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.ProjectionDateController;
import fr.geobert.radis.tools.Tools;
import org.acra.ACRA;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class AccountTable {
    private final static String TAG = "AccountTable";
    static final String DATABASE_ACCOUNT_TABLE = "accounts";
    public static final int PROJECTION_FURTHEST = 0;
    public static final int PROJECTION_DAY_OF_NEXT_MONTH = 1;
    public static final int PROJECTION_ABSOLUTE_DATE = 2;
    public static final String KEY_ACCOUNT_NAME = "account_name";
    public static final String KEY_ACCOUNT_DESC = "account_desc";
    public static final String KEY_ACCOUNT_START_SUM = "account_start_sum";
    public static final String KEY_ACCOUNT_CUR_SUM = "account_current_sum"; // according to current projection mode
    public static final String KEY_ACCOUNT_OP_SUM = "account_operations_sum";
    public static final String KEY_ACCOUNT_CURRENCY = "account_currency";
    public static final String KEY_ACCOUNT_ROWID = "_id";
    public static final String KEY_ACCOUNT_CUR_SUM_DATE = "account_current_sum_date"; // according to current projection mode
    public static final String KEY_ACCOUNT_PROJECTION_MODE = "account_projection_mode";
    public static final String KEY_ACCOUNT_PROJECTION_DATE = "account_projection_date";
    public static final String[] ACCOUNT_COLS = {KEY_ACCOUNT_ROWID,
            KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
            KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_PROJECTION_MODE,
            KEY_ACCOUNT_PROJECTION_DATE};

    public static final String[] ACCOUNT_FULL_COLS = {KEY_ACCOUNT_ROWID,
            KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
            KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_PROJECTION_MODE,
            KEY_ACCOUNT_PROJECTION_DATE, KEY_ACCOUNT_DESC,
            KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_OP_SUM};

    private static final String DATABASE_ACCOUNT_CREATE_v7 = "create table "
            + DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
            + " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
            + " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
            + KEY_ACCOUNT_START_SUM + " integer not null, "
            + KEY_ACCOUNT_OP_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM
            + " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE
            + " integer not null, " + KEY_ACCOUNT_CURRENCY + " text not null);";

    private static final String DATABASE_ACCOUNT_CREATE = "create table "
            + DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
            + " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
            + " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
            + KEY_ACCOUNT_START_SUM + " integer not null, "
            + KEY_ACCOUNT_OP_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM
            + " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE
            + " integer not null, " + KEY_ACCOUNT_CURRENCY + " text not null, "
            + KEY_ACCOUNT_PROJECTION_MODE + " integer not null, "
            + KEY_ACCOUNT_PROJECTION_DATE + " string);";

    protected static final String ADD_CUR_DATE_COLUNM = "ALTER TABLE "
            + DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
            + KEY_ACCOUNT_CUR_SUM_DATE + " integer not null DEFAULT 0";

    protected static final String ADD_PROJECTION_MODE_COLUNM = "ALTER TABLE "
            + DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
            + KEY_ACCOUNT_PROJECTION_MODE + " integer not null DEFAULT 0";

    protected static final String ADD_PROJECTION_MODE_DATE = "ALTER TABLE "
            + DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
            + KEY_ACCOUNT_PROJECTION_DATE + " string";

    protected static final String TRIGGER_ON_DELETE_ACCOUNT = "CREATE TRIGGER on_delete_account AFTER DELETE ON "
            + DATABASE_ACCOUNT_TABLE
            + " BEGIN "
            // if op is a transfert and was in deleted account, convert to an op in destination account
            + " UPDATE " + OperationTable.DATABASE_OPERATIONS_TABLE
            + " SET " + OperationTable.KEY_OP_ACCOUNT_ID + " = " + OperationTable.KEY_OP_TRANSFERT_ACC_ID + ", "
            + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = 0, "
            + OperationTable.KEY_OP_THIRD_PARTY + " = null, "
            + OperationTable.KEY_OP_SUM + " = -" + OperationTable.KEY_OP_SUM + " WHERE "
            + OperationTable.KEY_OP_ACCOUNT_ID + " = old." + AccountTable.KEY_ACCOUNT_ROWID + " AND "
            + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " != 0;"
            // if deleted account is in a tranfert, operation is not a transfert anymore
            + " UPDATE " + OperationTable.DATABASE_OPERATIONS_TABLE
            + " SET " + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = 0, "
            + OperationTable.KEY_OP_TRANSFERT_ACC_NAME + " = null WHERE "
            + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = old." + AccountTable.KEY_ACCOUNT_ROWID
            // if op is not a transfert and was in deleted account, delete it
            + "; DELETE FROM " + OperationTable.DATABASE_OPERATIONS_TABLE + " WHERE "
            + OperationTable.KEY_OP_ACCOUNT_ID + " = old." + AccountTable.KEY_ACCOUNT_ROWID + " AND "
            + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = 0;"
            // delete sch op if deleted account was involved, trigger in
            // sch op table manage to decorralate existing op
            + " DELETE FROM " + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE + " WHERE "
            + ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID + " = old." + AccountTable.KEY_ACCOUNT_ROWID + " OR "
            + OperationTable.KEY_OP_TRANSFERT_ACC_ID + " = old." + AccountTable.KEY_ACCOUNT_ROWID
            + "; END";

    private static int mProjectionMode = -1;
    private static long mProjectionDate;

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_ACCOUNT_CREATE);
    }

    public static long createAccount(Context ctx, String name, String desc,
                                     long start_sum, String currency, int projectionMode,
                                     String projectionDate) throws ParseException {
        ContentValues values = new ContentValues();
        values.put(KEY_ACCOUNT_NAME, name);
        values.put(KEY_ACCOUNT_DESC, desc);
        values.put(KEY_ACCOUNT_START_SUM, start_sum);
        values.put(KEY_ACCOUNT_CURRENCY, currency);
        values.put(KEY_ACCOUNT_PROJECTION_MODE, projectionMode);
        values.put(KEY_ACCOUNT_PROJECTION_DATE, projectionDate);

        setCurrentSumAndDate(ctx, 0, values, start_sum, projectionMode,
                projectionDate);
        Uri res = ctx.getContentResolver().insert(
                DbContentProvider.ACCOUNT_URI, values);
        return Long.parseLong(res.getLastPathSegment());
    }

    public static boolean deleteAccount(Context ctx, final long accountId) {
        // ScheduledOperationTable.deleteScheduledOpOfAccount(ctx, accountId);
        return ctx.getContentResolver().delete(
                Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
                null, null) > 0;
    }

    public static int consolidateSums(Context ctx, final long accountId) {
        int res = 0;
        if (0 != accountId) {
            ContentValues values = new ContentValues();
            Cursor account = fetchAccount(ctx, accountId);
            if (account != null) {
                if (account.moveToFirst()) {
                    try {
                        setCurrentSumAndDate(ctx, accountId, values,
                                account.getLong(account
                                        .getColumnIndex(KEY_ACCOUNT_START_SUM)),
                                account.getInt(account
                                        .getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
                                account.getString(account
                                        .getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));

                        res = ctx.getContentResolver().update(
                                Uri.parse(DbContentProvider.ACCOUNT_URI + "/"
                                        + accountId), values, null, null);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                account.close();
            }
        }
        return res;
    }

    private static void setCurrentSumAndDate(Context ctx, long accountId,
                                             ContentValues values, final long start_sum,
                                             final int projectionMode, final String projectionDate)
            throws ParseException {
        long date = 0;
        long opSum = 0;
        Log.d(TAG, "setCurrentSumAndDate mAccountId = " + accountId
                + "/ projectionMode : " + projectionMode);
        switch (projectionMode) {
            case PROJECTION_FURTHEST: {
                if (accountId > 0) {
                    Cursor allOps = OperationTable.fetchAllOps(ctx, accountId);
                    if (null != allOps) {
                        Log.d(TAG, "setCurrentSumAndDate allOps not null : "
                                + allOps.getCount());
                        if (allOps.moveToFirst()) {
                            date = allOps.getLong(allOps
                                    .getColumnIndex(OperationTable.KEY_OP_DATE));
                            opSum = OperationTable.computeSumFromCursor(allOps,
                                    accountId);
                            Log.d(TAG,
                                    "setCurrentSumAndDate allOps moved to first opSum = "
                                            + opSum);
                        }
                        allOps.close();
                    }
                }
            }
            break;
            case PROJECTION_DAY_OF_NEXT_MONTH: {
                GregorianCalendar projDate = Tools.createClearedCalendar();
                if (projDate.get(Calendar.DAY_OF_MONTH) >= Integer.parseInt(projectionDate)) {
                    projDate.add(Calendar.MONTH, 1);
                }
                projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(projectionDate));
                projDate.add(Calendar.DAY_OF_MONTH, 1); // for query
                Cursor op = OperationTable.fetchOpEarlierThan(ctx, projDate.getTimeInMillis(), 0, accountId);
                projDate.add(Calendar.DAY_OF_MONTH, -1); // restore date after query
                if (null != op) {
                    if (op.moveToFirst()) {
                        opSum = OperationTable.computeSumFromCursor(op, accountId);
                    }
                    op.close();
                }
                date = projDate.getTimeInMillis();
            }
            break;
            case PROJECTION_ABSOLUTE_DATE: {
                GregorianCalendar projDate = Tools.createClearedCalendar();
                projDate.setTime(Formater.getFullDateFormater().parse(
                        projectionDate));
                projDate.add(Calendar.DAY_OF_MONTH, 1); // roll for query
                Cursor op = OperationTable.fetchOpEarlierThan(ctx, projDate.getTimeInMillis(), 0, accountId);
                projDate.add(Calendar.DAY_OF_MONTH, -1); // restore date after
                if (null != op) {
                    if (op.moveToFirst()) {
                        opSum = OperationTable.computeSumFromCursor(op, accountId);
                    }
                    op.close();
                }
                date = projDate.getTimeInMillis();
            }
            break;
            default:
                break;
        }
        Log.d(TAG, "setCurrentSumAndDate opSum = " + opSum + "/ startSum = "
                + start_sum + "/ sum : " + (start_sum + opSum));
        values.put(KEY_ACCOUNT_OP_SUM, opSum);
        values.put(KEY_ACCOUNT_CUR_SUM, start_sum + opSum);
        Log.d(TAG, "setCurrentSumAndDate, KEY_ACCOUNT_CUR_SUM_DATE : "
                + Formater.getFullDateFormater().format(new Date(date)));
        values.put(KEY_ACCOUNT_CUR_SUM_DATE, date);
    }

    public static Cursor fetchAccount(Context ctx, final long accountId) {
        return ctx.getContentResolver().query(
                Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
                ACCOUNT_FULL_COLS, null, null, null);
    }

    public static CursorLoader getAccountLoader(Context ctx,
                                                final long accountId) {
        return new CursorLoader(ctx, Uri.parse(DbContentProvider.ACCOUNT_URI
                + "/" + accountId), AccountTable.ACCOUNT_FULL_COLS, null, null,
                null);
    }

    public static Cursor fetchAllAccounts(Context ctx) {
        return ctx.getContentResolver().query(DbContentProvider.ACCOUNT_URI,
                ACCOUNT_FULL_COLS, null, null, null);
    }

    public static CursorLoader getAllAccountsLoader(Context ctx) {
        return new CursorLoader(ctx, DbContentProvider.ACCOUNT_URI,
                ACCOUNT_COLS, null, null, null);
    }

    static boolean checkNeedUpdateProjection(Context ctx, Operation op,
                                             final long accountId) {
        Cursor c = fetchAccount(ctx, accountId);
        if (c.moveToFirst()) {
            initProjectionDate(c);
        }
        c.close();
        final long opDate = op.getDate();
        final long projDate = mProjectionDate;
        boolean res = (opDate <= projDate)
                || ((mProjectionMode == 0) && (opDate >= projDate))
                || (projDate == 0);
        Log.d(TAG,
                "checkNeedUpdateProjection : "
                        + res
                        + "/mProjectionDate : "
                        + Formater.getFullDateFormater().format(
                        new Date(mProjectionDate))
                        + "/opdate = "
                        + Formater.getFullDateFormater().format(
                        new Date(opDate)) + "/projMode = "
                        + mProjectionMode);
        return res;
    }

    public static void initProjectionDate(Cursor c) {
        Log.d(TAG, "initProjectionDate cursor : " + c);
        if (c != null) {
            mProjectionMode = c.getInt(c
                    .getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE));
            Log.d(TAG, "initProjectionDate mode : " + mProjectionMode);
            switch (mProjectionMode) {
                case PROJECTION_FURTHEST:
                    mProjectionDate = c.getLong(c
                            .getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE));
                    break;
                case PROJECTION_DAY_OF_NEXT_MONTH: {
                    GregorianCalendar projDate = Tools.createClearedCalendar();
                    projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(c
                            .getString(c
                                    .getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE))));
                    GregorianCalendar today = Tools.createClearedCalendar();
                    if (projDate.compareTo(today) <= 0) {
                        projDate.add(Calendar.MONTH, 1);
                    }
                    mProjectionDate = projDate.getTimeInMillis();
                }
                break;
                case PROJECTION_ABSOLUTE_DATE:
                    try {
                        Date projDate = Formater
                                .getFullDateFormater()
                                .parse(c.getString(c
                                        .getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));
                        GregorianCalendar cal = new GregorianCalendar();
                        cal.setTime(projDate);
                        cal.set(Calendar.HOUR, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        mProjectionDate = cal.getTimeInMillis();
                    } catch (ParseException e) {
                        ACRA.getErrorReporter().handleSilentException(e);
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public static boolean updateAccountProjectionDate(Context ctx,
                                                      long accountId, ProjectionDateController projectionController)
            throws ParseException {
        if (projectionController.hasChanged()) {
            updateAccountProjectionDate(ctx, accountId,
                    projectionController.getMode(),
                    projectionController.getDate(), true);

        }
        return true;
    }

    private static boolean updateAccountProjectionDate(Context ctx,
                                                       long accountId, final int projMode, final String projDate,
                                                       boolean updateSumAndDate) throws ParseException {
        Cursor account = fetchAccount(ctx, accountId);
        if (account.moveToFirst()) {
            ContentValues args = new ContentValues();
            long start_sum = account.getLong(account
                    .getColumnIndex(KEY_ACCOUNT_START_SUM));
            account.close();
            Log.d(TAG,
                    "updateAccountProjectionDate, KEY_ACCOUNT_PROJECTION_DATE : "
                            + projDate);
            args.put(KEY_ACCOUNT_PROJECTION_MODE, projMode);
            args.put(KEY_ACCOUNT_PROJECTION_DATE, projDate);
            if (updateSumAndDate) {
                setCurrentSumAndDate(ctx, accountId, args, start_sum, projMode,
                        projDate);
            }
            return ctx.getContentResolver().update(
                    Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
                    args, null, null) > 0;
        } else {
            return false;
        }
    }

    public static boolean updateAccountProjectionDate(Context ctx,
                                                      long accountId) throws ParseException {
        Cursor c = fetchAccount(ctx, accountId);
        try {
            boolean res = true;
            if (null != c) {
                if (c.moveToFirst()) {
                    res = updateAccountProjectionDate(
                            ctx,
                            accountId,
                            c.getInt(c
                                    .getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
                            c.getString(c
                                    .getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)), true);
                }
                c.close();
            }
            return res;
        } catch (ParseException e) {
            c.close();
            throw e;
        }
    }

    public static boolean updateAccountCurrency(Context ctx, long accountId,
                                                String currency) {
        ContentValues args = new ContentValues();
        args.put(KEY_ACCOUNT_CURRENCY, currency);
        return ctx.getContentResolver().update(
                Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
                args, null, null) > 0;
    }

    public static boolean updateAccount(Context ctx, long accountId,
                                        String name, String desc, long start_sum, String currency,
                                        ProjectionDateController projectionController)
            throws ParseException {
        ContentValues args = new ContentValues();
        args.put(KEY_ACCOUNT_NAME, name);
        args.put(KEY_ACCOUNT_DESC, desc);
        args.put(KEY_ACCOUNT_START_SUM, start_sum);
        args.put(KEY_ACCOUNT_CURRENCY, currency);
        args.put(KEY_ACCOUNT_PROJECTION_MODE, projectionController.getMode());
        args.put(KEY_ACCOUNT_PROJECTION_DATE, projectionController.getDate());
        setCurrentSumAndDate(ctx, accountId, args, start_sum,
                projectionController.getMode(), projectionController.getDate());
        return ctx.getContentResolver().update(
                Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
                args, null, null) > 0;
    }

    public static void updateProjection(Context ctx, long accountId,
                                        long sumToAdd, long opDate) {
        ContentValues args = new ContentValues();
        assert (mProjectionMode != -1);
        Log.d(TAG, "updateProjection, mProjectionMode " + mProjectionMode
                + " / opDate " + opDate);
        if (mProjectionMode == 0 && (opDate > mProjectionDate || opDate == 0)) {
            if (opDate == 0) {
                Cursor op = OperationTable.fetchLastOp(ctx, accountId);
                if (null != op) {
                    if (op.moveToFirst()) {
                        Log.d(TAG,
                                "updateProjection, KEY_ACCOUNT_CUR_SUM_DATE 0 : "
                                        + Formater
                                        .getFullDateFormater()
                                        .format(new Date(
                                                op.getLong(op
                                                        .getColumnIndex(OperationTable.KEY_OP_DATE)))));
                        args.put(KEY_ACCOUNT_CUR_SUM_DATE, op.getLong(op
                                .getColumnIndex(OperationTable.KEY_OP_DATE)));
                    }
                    op.close();
                }
            } else {
                Log.d(TAG,
                        "updateProjection, KEY_ACCOUNT_CUR_SUM_DATE 1 : "
                                + Formater.getFullDateFormater().format(
                                new Date(opDate)));
                args.put(KEY_ACCOUNT_CUR_SUM_DATE, opDate);
            }
        }
        Cursor accountCursor = fetchAccount(ctx, accountId);
        if (accountCursor.moveToFirst()) {
            long opSum = accountCursor.getLong(accountCursor
                    .getColumnIndex(KEY_ACCOUNT_OP_SUM));
            long startSum = accountCursor.getLong(accountCursor
                    .getColumnIndex(KEY_ACCOUNT_START_SUM));

            args.put(KEY_ACCOUNT_OP_SUM, opSum + sumToAdd);

            long date = 0;
            Long tmp = args.getAsLong(KEY_ACCOUNT_CUR_SUM_DATE);
            if (tmp != null) {
                date = tmp.longValue();
            }
            if (date == 0) {
                date = accountCursor.getLong(accountCursor
                        .getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE));
            }
            Log.d(TAG, "updateProjection date "
                    + Formater.getFullDateFormater().format(date) + "/opDate "
                    + Formater.getFullDateFormater().format(opDate));
            if (date == 0 || opDate == 0 || opDate <= date) {
                long curSum = accountCursor.getLong(accountCursor
                        .getColumnIndex(KEY_ACCOUNT_CUR_SUM));
                args.put(KEY_ACCOUNT_CUR_SUM, curSum + sumToAdd);
            }
            if (ctx.getContentResolver().update(
                    Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
                    args, null, null) > 0) {
                if (mProjectionMode == 0) {
                    mProjectionDate = opDate;
                }
            }
        }
        accountCursor.close();
    }

    // UPGRADE FUNCTIONS
    private static void rawSetCurrentSumAndDate(SQLiteDatabase db,
                                                long accountId, ContentValues values, final long start_sum,
                                                final int projectionMode, final String projectionDate)
            throws ParseException {
        long date = 0;
        long opSum = 0;
        Log.d(TAG, "raw setCurrentSumAndDate mAccountId = " + accountId
                + "/ mode : " + projectionMode);
        switch (projectionMode) {
            case 0: {
                if (accountId > 0) {
                    Cursor allOps = db.query(
                            OperationTable.DATABASE_OP_TABLE_JOINTURE,
                            OperationTable.OP_COLS_QUERY,
                            OperationTable.RESTRICT_TO_ACCOUNT,
                            new String[]{Long.toString(accountId),
                                    Long.toString(accountId)}, null, null,
                            OperationTable.OP_ORDERING, null);
                    if (null != allOps) {
                        Log.d(TAG, "raw setCurrentSumAndDate allOps not null : "
                                + allOps.getCount());
                        if (allOps.moveToFirst()) {
                            Log.d(TAG,
                                    "raw setCurrentSumAndDate allOps moved to first");
                            date = allOps.getLong(allOps
                                    .getColumnIndex(OperationTable.KEY_OP_DATE));
                            opSum = OperationTable.computeSumFromCursor(allOps,
                                    accountId);
                        }
                        allOps.close();
                    }
                }
            }
            break;
            case 1: {
                GregorianCalendar projDate = Tools.createClearedCalendar();
                if (projDate.get(Calendar.DAY_OF_MONTH) >= Integer.parseInt(projectionDate)) {
                    projDate.add(Calendar.MONTH, 1);
                }
                projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(projectionDate));
                projDate.add(Calendar.DAY_OF_MONTH, 1); // roll for query
                Cursor op = db.query(OperationTable.DATABASE_OP_TABLE_JOINTURE, OperationTable.OP_COLS_QUERY,
                        OperationTable.RESTRICT_TO_ACCOUNT + " and ops." + OperationTable.KEY_OP_DATE + " < ?",
                        new String[]{Long.toString(accountId), Long.toString(accountId),
                                Long.toString(projDate.getTimeInMillis())}, null, null, OperationTable.OP_ORDERING);
                projDate.add(Calendar.DAY_OF_MONTH, -1); // restore date after
                // query
                if (null != op) {
                    if (op.moveToFirst()) {
                        opSum = OperationTable.computeSumFromCursor(op, accountId);
                    }
                    op.close();
                }
                date = projDate.getTimeInMillis();
            }
            break;
            case 2: {
                GregorianCalendar projDate = Tools.createClearedCalendar();
                projDate.setTime(Formater.getFullDateFormater().parse(
                        projectionDate));
                projDate.add(Calendar.DAY_OF_MONTH, 1); // roll for query
                Cursor op = db.query(
                        OperationTable.DATABASE_OP_TABLE_JOINTURE,
                        OperationTable.OP_COLS_QUERY,
                        OperationTable.RESTRICT_TO_ACCOUNT + " and ops."
                                + OperationTable.KEY_OP_DATE + " < ?",
                        new String[]{Long.toString(accountId),
                                Long.toString(accountId),
                                Long.toString(projDate.getTimeInMillis())}, null,
                        null, OperationTable.OP_ORDERING);
                projDate.add(Calendar.DAY_OF_MONTH, -1); // restore date after
                // query
                if (null != op) {
                    if (op.moveToFirst()) {
                        opSum = OperationTable.computeSumFromCursor(op, accountId);
                    }
                    op.close();
                }
                date = projDate.getTimeInMillis();
            }
            break;
            default:
                break;
        }
        values.put(KEY_ACCOUNT_OP_SUM, opSum);
        values.put(KEY_ACCOUNT_CUR_SUM, start_sum + opSum);
        Log.d(TAG, "rawSetCurrentSumAndDate, KEY_ACCOUNT_CUR_SUM_DATE : "
                + Formater.getFullDateFormater().format(new Date(date)));
        values.put(KEY_ACCOUNT_CUR_SUM_DATE, date);
    }

    private static void rawConsolidateSums(SQLiteDatabase db, long accountId) {
        if (0 != accountId) {
            ContentValues values = new ContentValues();
            Cursor account = db.query(DATABASE_ACCOUNT_TABLE, ACCOUNT_FULL_COLS, "_id=?",
                    new String[]{Long.toString(accountId)}, null,
                    null, null);
            if (account != null) {
                if (account.moveToFirst()) {
                    try {
                        rawSetCurrentSumAndDate(db, accountId, values,
                                account.getLong(account.getColumnIndex(KEY_ACCOUNT_START_SUM)),
                                account.getInt(account.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
                                account.getString(account.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));
                        db.update(DATABASE_ACCOUNT_TABLE, values, "_id=?", new String[]{Long.toString(accountId)});
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                account.close();
            }
        }
    }

    static void upgradeDefault(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cursor c = db.query(DATABASE_ACCOUNT_TABLE,
                new String[]{KEY_ACCOUNT_ROWID}, null, null, null, null,
                null);
        if (null != c) {
            if (c.moveToFirst()) {
                do {
                    rawConsolidateSums(db, c.getLong(0));
                } while (c.moveToNext());
            }
            c.close();
        }
    }

    static void upgradeFromV9(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ADD_PROJECTION_MODE_COLUNM);
        db.execSQL(ADD_PROJECTION_MODE_DATE);
        Cursor c = db.query(DATABASE_ACCOUNT_TABLE, new String[]{}, null,
                null, null, null, null);
        if (null != c) {
            if (c.moveToFirst()) {
                do {
                    rawConsolidateSums(db,
                            c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)));
                } while (c.moveToNext());
            }
            c.close();
        }
    }

    static void upgradeFromV6(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TRIGGER on_delete_third_party");
        db.execSQL("DROP TRIGGER on_delete_mode");
        db.execSQL("DROP TRIGGER on_delete_tag");
        db.execSQL("ALTER TABLE accounts RENAME TO accounts_old;");
        db.execSQL(DATABASE_ACCOUNT_CREATE_v7);
        Cursor c = db.query("accounts_old", new String[]{KEY_ACCOUNT_ROWID,
                KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
                KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_DESC, KEY_ACCOUNT_OP_SUM,
                KEY_ACCOUNT_START_SUM}, null, null, null, null, null);
        if (null != c && c.moveToFirst()) {
            do {
                ContentValues initialValues = new ContentValues();
                initialValues.put(KEY_ACCOUNT_NAME,
                        c.getString(c.getColumnIndex(KEY_ACCOUNT_NAME)));
                initialValues.put(KEY_ACCOUNT_DESC,
                        c.getString(c.getColumnIndex(KEY_ACCOUNT_DESC)));
                double d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_START_SUM));
                long l = Math.round(d * 100);
                initialValues.put(KEY_ACCOUNT_START_SUM, l);
                d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_OP_SUM));
                l = Math.round(d * 100);
                initialValues.put(KEY_ACCOUNT_OP_SUM, l);
                d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM));
                l = Math.round(d * 100);
                initialValues.put(KEY_ACCOUNT_CUR_SUM, l);
                initialValues.put(KEY_ACCOUNT_CURRENCY,
                        c.getString(c.getColumnIndex(KEY_ACCOUNT_CURRENCY)));
                initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE,
                        c.getLong(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE)));
                long id = db
                        .insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
                initialValues = new ContentValues();
                initialValues.put(OperationTable.KEY_OP_ACCOUNT_ID, id);
                db.update(
                        OperationTable.DATABASE_OPERATIONS_TABLE,
                        initialValues,
                        OperationTable.KEY_OP_ACCOUNT_ID
                                + "="
                                + c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)),
                        null);

            } while (c.moveToNext());
            c.close();
            db.execSQL("DROP TABLE accounts_old;");
        }
    }

    static void upgradeFromV12(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(TRIGGER_ON_DELETE_ACCOUNT);
    }

    static void upgradeFromV4(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ADD_CUR_DATE_COLUNM);
    }
}
