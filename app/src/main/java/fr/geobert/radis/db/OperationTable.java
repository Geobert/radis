package fr.geobert.radis.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.tools.Tools;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.GregorianCalendar;

public class OperationTable {
    public static final String DATABASE_OPERATIONS_TABLE = "operations";
    public static final String KEY_OP_DATE = "date";
    public static final String KEY_OP_THIRD_PARTY = "third_party";
    public static final String KEY_OP_TAG = "tag";
    public static final String KEY_OP_MODE = "mode";
    public static final String KEY_OP_SUM = "sum";
    public static final String KEY_OP_SCHEDULED_ID = "scheduled_id";
    public static final String KEY_OP_ACCOUNT_ID = "account_id";
    public static final String KEY_OP_ROWID = "_id";
    public static final String KEY_OP_NOTES = "notes";
    public static final String KEY_OP_TRANSFERT_ACC_ID = "transfert_acc_id";
    public static final String KEY_OP_TRANSFERT_ACC_NAME = "transfert_acc_src_name";
    public static final String KEY_OP_CHECKED = "checked";
    public static final String OP_ORDERING = "ops." + KEY_OP_DATE
            + " desc, ops." + KEY_OP_ROWID + " desc";
    public static final String DATABASE_OP_TABLE_JOINTURE = DATABASE_OPERATIONS_TABLE
            + " ops LEFT OUTER JOIN "
            + InfoTables.DATABASE_THIRD_PARTIES_TABLE
            + " tp ON ops."
            + KEY_OP_THIRD_PARTY
            + " = tp."
            + InfoTables.KEY_THIRD_PARTY_ROWID
            + " LEFT OUTER JOIN "
            + InfoTables.DATABASE_MODES_TABLE
            + " mode ON ops."
            + KEY_OP_MODE
            + " = mode."
            + InfoTables.KEY_MODE_ROWID
            + " LEFT OUTER JOIN "
            + InfoTables.DATABASE_TAGS_TABLE
            + " tag ON ops."
            + KEY_OP_TAG
            + " = tag." + InfoTables.KEY_TAG_ROWID;
    public static final String[] OP_COLS_QUERY = {"ops." + KEY_OP_ROWID, // 0
            "tp." + InfoTables.KEY_THIRD_PARTY_NAME,
            "tag." + InfoTables.KEY_TAG_NAME,
            "mode." + InfoTables.KEY_MODE_NAME,
            "ops." + KEY_OP_SUM, // 4
            "ops." + KEY_OP_DATE,
            "ops." + KEY_OP_ACCOUNT_ID,
            "ops." + KEY_OP_NOTES,
            "ops." + KEY_OP_SCHEDULED_ID, // 8
            "ops." + KEY_OP_TRANSFERT_ACC_ID,
            "ops." + KEY_OP_TRANSFERT_ACC_NAME,
            "ops." + KEY_OP_CHECKED}; // 11
    public static final String RESTRICT_TO_ACCOUNT = "(ops."
            + KEY_OP_ACCOUNT_ID + " = ? OR ops." + KEY_OP_TRANSFERT_ACC_ID
            + " = ?)";
    protected static final String DATABASE_OP_CREATE = "create table "
            + DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ROWID + " integer primary key autoincrement, "
            + KEY_OP_THIRD_PARTY + " integer, "
            + KEY_OP_TAG + " integer, "
            + KEY_OP_SUM + " integer not null, "
            + KEY_OP_ACCOUNT_ID + " integer not null, "
            + KEY_OP_MODE + " integer, "
            + KEY_OP_DATE + " integer not null, "
            + KEY_OP_NOTES + " text, "
            + KEY_OP_SCHEDULED_ID + " integer, "
            + KEY_OP_TRANSFERT_ACC_NAME + " text, "
            + KEY_OP_TRANSFERT_ACC_ID + " integer not null, "
            + KEY_OP_CHECKED + " integer not null, "
            + " FOREIGN KEY (" + KEY_OP_THIRD_PARTY + ") REFERENCES " + InfoTables.DATABASE_THIRD_PARTIES_TABLE + "("
            + InfoTables.KEY_THIRD_PARTY_ROWID + "), FOREIGN KEY ("
            + KEY_OP_TAG + ") REFERENCES " + InfoTables.DATABASE_TAGS_TABLE
            + "(" + InfoTables.KEY_TAG_ROWID + "), FOREIGN KEY (" + KEY_OP_MODE
            + ") REFERENCES " + InfoTables.DATABASE_MODES_TABLE + "("
            + InfoTables.KEY_MODE_ROWID + "), FOREIGN KEY ("
            + KEY_OP_SCHEDULED_ID + ") REFERENCES "
            + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE + "("
            + ScheduledOperationTable.KEY_SCHEDULED_ROWID + "));";
    protected static final String INDEX_ON_ACCOUNT_ID_CREATE = "CREATE INDEX IF NOT EXISTS account_id_idx ON "
            + DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ACCOUNT_ID + ")";
    protected static final String TRIGGER_ON_DELETE_THIRD_PARTY_CREATE = "CREATE TRIGGER on_delete_third_party AFTER DELETE ON "
            + InfoTables.DATABASE_THIRD_PARTIES_TABLE
            + " BEGIN UPDATE "
            + DATABASE_OPERATIONS_TABLE
            + " SET "
            + KEY_OP_THIRD_PARTY
            + " = null WHERE "
            + KEY_OP_THIRD_PARTY
            + " = old."
            + InfoTables.KEY_THIRD_PARTY_ROWID
            + "; UPDATE "
            + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE
            + " SET "
            + KEY_OP_THIRD_PARTY
            + " = null WHERE "
            + KEY_OP_THIRD_PARTY
            + " = old." + InfoTables.KEY_THIRD_PARTY_ROWID + "; END";
    protected static final String TRIGGER_ON_DELETE_MODE_CREATE = "CREATE TRIGGER on_delete_mode AFTER DELETE ON "
            + InfoTables.DATABASE_MODES_TABLE
            + " BEGIN UPDATE "
            + DATABASE_OPERATIONS_TABLE
            + " SET "
            + KEY_OP_MODE
            + " = null WHERE "
            + KEY_OP_MODE
            + " = old."
            + InfoTables.KEY_MODE_ROWID
            + "; UPDATE "
            + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE
            + " SET "
            + KEY_OP_MODE
            + " = null WHERE "
            + KEY_OP_MODE
            + " = old."
            + InfoTables.KEY_MODE_ROWID + "; END";
    protected static final String TRIGGER_ON_DELETE_TAG_CREATE = "CREATE TRIGGER on_delete_tag AFTER DELETE ON "
            + InfoTables.DATABASE_TAGS_TABLE
            + " BEGIN UPDATE "
            + DATABASE_OPERATIONS_TABLE
            + " SET "
            + KEY_OP_TAG
            + " = null WHERE "
            + KEY_OP_TAG
            + " = old."
            + InfoTables.KEY_TAG_ROWID
            + "; UPDATE "
            + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE
            + " SET "
            + KEY_OP_TAG
            + " = null WHERE "
            + KEY_OP_TAG
            + " = old."
            + InfoTables.KEY_TAG_ROWID + "; END";
    protected static final String ADD_NOTES_COLUNM = "ALTER TABLE "
            + DATABASE_OPERATIONS_TABLE + " ADD COLUMN op_notes text";
    protected static final String ADD_TRANSFERT_ID_COLUNM = "ALTER TABLE %s ADD COLUMN "
            + KEY_OP_TRANSFERT_ACC_ID + " integer not null DEFAULT 0";
    protected static final String ADD_TRANSFERT_NAME_COLUNM = "ALTER TABLE %s ADD COLUMN "
            + KEY_OP_TRANSFERT_ACC_NAME + " text";
    protected static final String ADD_CHECKED_COLUNM = "ALTER TABLE %s ADD COLUMN "
            + KEY_OP_CHECKED + " integer not null DEFAULT 0";

    private static final String TAG = "OperationTable";

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_OP_CREATE);
    }

    static void createMeta(SQLiteDatabase db) {
        db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
    }

    public static Cursor fetchAllOps(Context ctx, final long accountId) {
        Cursor c = ctx.getContentResolver().query(
                DbContentProvider.OPERATION_JOINED_URI,
                OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT,
                new String[]{Long.toString(accountId),
                        Long.toString(accountId)}, OP_ORDERING
        );
        if (null != c) {
            c.moveToFirst();
        }
        return c;
    }

    public static Cursor fetchAllCheckedOps(Context ctx, final long accountId) {
        Cursor c = ctx.getContentResolver().query(
                DbContentProvider.OPERATION_JOINED_URI,
                OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT + " AND ops." + OperationTable.KEY_OP_CHECKED + " = ?",
                new String[]{Long.toString(accountId),
                        Long.toString(accountId), Integer.toString(1)}, OP_ORDERING
        );
        if (null != c) {
            c.moveToFirst();
        }
        return c;
    }

    public static Cursor fetchAllUncheckedOps(Context ctx, final long accountId, final long maxDate) {
        Cursor c = ctx.getContentResolver().query(
                DbContentProvider.OPERATION_JOINED_URI,
                OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT + " AND ops." + OperationTable.KEY_OP_CHECKED + " = ? AND ops." +
                        KEY_OP_DATE + " <= ?", new String[]{Long.toString(accountId),
                        Long.toString(accountId), Integer.toString(0), Long.toString(maxDate)}, OP_ORDERING
        );
        if (null != c) {
            c.moveToFirst();
        }
        return c;
    }

    public static long computeSumFromCursor(Cursor c, long curAccount) {
        long sum = 0L;
        final int sumIdx = c.getColumnIndex(KEY_OP_SUM);
        final int transIdx = c.getColumnIndex(KEY_OP_TRANSFERT_ACC_ID);
        if (!c.isBeforeFirst() && !c.isAfterLast()) {
            do {
                long s = c.getLong(sumIdx);
                if (c.getLong(transIdx) == curAccount) {
                    s = -s;
                }
                sum = sum + s;
            } while (c.moveToNext());
        }
        return sum;
    }

    static Cursor fetchOpEarlierThan(Context ctx, long date, int nbOps,
                                     final long accountId) {
        Log.d(TAG, "fetchOpEarlierThan date : " + Tools.getDateStr(date) + " with limit : " + nbOps);
        Cursor c;
        String limit = nbOps == 0 ? null : Integer.toString(nbOps);
        c = ctx.getContentResolver().query(
                DbContentProvider.OPERATION_JOINED_URI,
                OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT + " and ops." + KEY_OP_DATE + " < ?",
                new String[]{Long.toString(accountId),
                        Long.toString(accountId), Long.toString(date)},
                OP_ORDERING
                        + (limit == null ? "" : " ops._id asc LIMIT " + limit)
        );
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    public static long createOp(Context ctx, final Operation op, final long accountId) {
        return OperationTable.createOp(ctx, op, accountId, true);
    }

    public static long createOp(Context ctx, final Operation op,
                                final long accountId, final boolean withUpdate) {
        ContentValues initialValues = new ContentValues();
        String key = op.getmThirdParty();
        InfoTables.putKeyIdInThirdParties(ctx, key, initialValues, false);

        key = op.getmTag();
        InfoTables.putKeyIdInTags(ctx, key, initialValues, false);

        key = op.getmMode();
        InfoTables.putKeyIdInModes(ctx, key, initialValues, false);

        initialValues.put(KEY_OP_SUM, op.getmSum());
        initialValues.put(KEY_OP_DATE, op.getDate());
        initialValues.put(KEY_OP_ACCOUNT_ID, accountId);
        initialValues.put(KEY_OP_NOTES, op.getmNotes());
        initialValues.put(KEY_OP_SCHEDULED_ID, op.getmScheduledId());
        initialValues.put(KEY_OP_TRANSFERT_ACC_ID, op.getmTransferAccountId());
        initialValues.put(KEY_OP_TRANSFERT_ACC_NAME, op.getmTransSrcAccName());
        initialValues.put(KEY_OP_CHECKED, op.getmIsChecked() ? 1 : 0);
        Uri res = ctx.getContentResolver().insert(
                DbContentProvider.OPERATION_URI, initialValues);
        op.setmRowId(Long.parseLong(res.getLastPathSegment()));
        if (op.getmRowId() > -1) {
            if (withUpdate) {
                AccountTable.updateProjection(ctx, accountId, op.getmSum(), 0, op.getDate(), -1);
                if (op.getmTransferAccountId() > 0) {
                    AccountTable.updateProjection(ctx, op.getmTransferAccountId(), -op.getmSum(), 0, op.getDate(), -1);
                }
                if (op.getmIsChecked()) {
                    AccountTable.updateCheckedOpSum(ctx, op, op.getmIsChecked());
                }
            }
            return op.getmRowId();
        }
        Log.e(TAG, "error in creating op");
        return -1;
    }

    public static boolean deleteOp(Context ctx, long rowId, final long accountId) {
        Cursor c = fetchOneOp(ctx, rowId, accountId);
        if (c.moveToFirst()) {
            final long opSum = c.getLong(c.getColumnIndex(KEY_OP_SUM));
            final long opDate = c.getLong(c.getColumnIndex(KEY_OP_DATE));
            final boolean checked = c.getInt(c.getColumnIndex(KEY_OP_CHECKED)) == 1;
            final long transfertId = c.getLong(c.getColumnIndex(KEY_OP_TRANSFERT_ACC_ID));
            c.close();
            if (ctx.getContentResolver().delete(Uri.parse(DbContentProvider.OPERATION_URI + "/" + rowId), null, null) > 0) {
                AccountTable.updateProjection(ctx, accountId, -opSum, 0, opDate, -1);
                if (transfertId > 0) {
                    AccountTable.updateProjection(ctx, transfertId, opSum, 0, opDate, -1);
                }
                if (checked) {
                    AccountTable.updateCheckedOpSum(ctx, opSum, accountId, transfertId, false);
                }
                return true;
            }
        }
        return false;
    }

    public static Cursor fetchLastOp(Context ctx, final long accountId) {
        return ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " = (SELECT max(ops2." + KEY_OP_DATE + ") FROM "
                        + DATABASE_OPERATIONS_TABLE + " ops2 WHERE (ops2." + KEY_OP_ACCOUNT_ID + " = ? OR ops2." +
                        KEY_OP_TRANSFERT_ACC_ID + " = ?)) ",
                new String[]{Long.toString(accountId), Long.toString(accountId), Long.toString(accountId),
                        Long.toString(accountId)}, OP_ORDERING
        );
    }

    public static Cursor fetchLastOpSince(Context ctx, final long accountId, final long time) {
        return ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " = (SELECT max(ops2." + KEY_OP_DATE + ") FROM "
                        + DATABASE_OPERATIONS_TABLE + " ops2 WHERE (ops2." + KEY_OP_ACCOUNT_ID + " = ? OR ops2." +
                        KEY_OP_TRANSFERT_ACC_ID + " = ?) AND ops2." + KEY_OP_DATE + " < ?) ",
                new String[]{Long.toString(accountId), Long.toString(accountId), Long.toString(accountId),
                        Long.toString(accountId), Long.toString(time)}, OP_ORDERING
        );
    }

    public static Cursor fetchOneOp(Context ctx, final long rowId, final long accountId) {
        Cursor c = ctx.getContentResolver().query(
                DbContentProvider.OPERATION_JOINED_URI,
                OP_COLS_QUERY,
                RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_ROWID + " = ?",
                new String[]{Long.toString(accountId),
                        Long.toString(accountId), Long.toString(rowId)}, null
        );
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    @NotNull
    public static CursorLoader getOpsWithStartDateLoader(Context ctx, final Long earliestOpDate, final long accountId) {
        return new CursorLoader(ctx, DbContentProvider.OPERATION_JOINED_URI,
                OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " >= ?",
                new String[]{Long.toString(accountId),
                        Long.toString(accountId),
                        Long.toString(earliestOpDate)},
                OP_ORDERING
        );
    }

    public static Cursor getOpsBetweenDate(Context ctx, final Date earliestOpDate, final Date latestOpDate, final long accountId) {
        return ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT
                        + " AND ops." + KEY_OP_DATE + " >= ? AND ops." + KEY_OP_DATE + " <= ?",
                new String[]{Long.toString(accountId),
                        Long.toString(accountId),
                        Long.toString(earliestOpDate.getTime()),
                        Long.toString(latestOpDate.getTime())},
                OP_ORDERING
        );
    }

    // used in update op only
    private static ContentValues createContentValuesFromOp(Context ctx,
                                                           final Operation op,
                                                           final boolean updateOccurrences) {
        ContentValues args = new ContentValues();

        String key = op.getmThirdParty();
        InfoTables.putKeyIdInThirdParties(ctx, key, args, true);

        key = op.getmTag();
        InfoTables.putKeyIdInTags(ctx, key, args, true);

        key = op.getmMode();
        InfoTables.putKeyIdInModes(ctx, key, args, true);

        args.put(KEY_OP_SUM, op.getmSum());
        args.put(KEY_OP_NOTES, op.getmNotes());
        args.put(KEY_OP_TRANSFERT_ACC_ID, op.getmTransferAccountId());
        args.put(KEY_OP_TRANSFERT_ACC_NAME, op.getmTransSrcAccName());
        args.put(KEY_OP_CHECKED, op.getmIsChecked() ? 1 : 0);
        if (!updateOccurrences) {
            args.put(KEY_OP_DATE, op.getDate());
            args.put(KEY_OP_SCHEDULED_ID, op.getmScheduledId());
        }
        return args;
    }

    // return if need to update OP_SUM
    public static boolean updateOp(Context ctx, final long rowId,
                                   final Operation op, final Operation originalOp) {
        ContentValues args = createContentValuesFromOp(ctx, op, false);
        if (ctx.getContentResolver().update(Uri.parse(DbContentProvider.OPERATION_URI + "/" + rowId),
                args, null, null) > 0) {
            AccountTable.updateProjection(ctx, op.getmAccountId(), op.getmSum(), originalOp.getmSum(),
                    op.getDate(), originalOp.getDate());
            if (op.getmTransferAccountId() > 0) {
                if (originalOp.getmTransferAccountId() <= 0) {
                    // op was not a transfert, it is like adding an op in transfertAccountId
                    AccountTable.updateProjection(ctx, op.getmTransferAccountId(), -op.getmSum(), 0, op.getDate(), -1);
                } else {
                    // op was a transfert
                    if (originalOp.getmTransferAccountId() == op.getmTransferAccountId()) {
                        // op was a transfert on same account, update with sum diff
                        AccountTable.updateProjection(ctx, op.getmTransferAccountId(), -op.getmSum(),
                                -originalOp.getmSum(), op.getDate(), originalOp.getDate());
                    } else {
                        // op was a transfert to another account
                        // update new transfert account
                        AccountTable.updateProjection(ctx, op.getmTransferAccountId(),
                                -op.getmSum(), 0, op.getDate(), -1);
                        // remove the original sum on original transfert account
                        AccountTable.updateProjection(ctx, originalOp.getmTransferAccountId(), originalOp.getmSum(),
                                0, op.getDate(), originalOp.getDate());
                    }
                }
            } else if (originalOp.getmTransferAccountId() > 0) {
                // op become not transfert, but was a transfert
                AccountTable.updateProjection(ctx, originalOp.getmTransferAccountId(), originalOp.getmSum(), 0,
                        op.getDate(), -1);
            }

            if (originalOp.getmIsChecked() != op.getmIsChecked()) {
                AccountTable.updateCheckedOpSum(ctx, op.getmSum(), op.getmAccountId(), op.getmTransferAccountId(),
                        op.getmIsChecked());
            }

            return true;
        }
        return false;
    }

    public static int deleteAllOccurrences(Context ctx, final long accountId,
                                           final long schOpId, final long transfertId) {
        int nb = ctx.getContentResolver()
                .delete(DbContentProvider.OPERATION_URI,
                        KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID
                                + "=?",
                        new String[]{Long.toString(accountId),
                                Long.toString(schOpId)}
                );
        if (nb > 0) {
            AccountTable.consolidateSums(ctx, accountId);
            if (transfertId > 0) {
                AccountTable.consolidateSums(ctx, transfertId);
            }
        }
        return nb;
    }

    public static int deleteAllFutureOccurrences(Context ctx,
                                                 final long accountId, final long schOpId, final long date,
                                                 final long transfertId) {
        int nbDel = ctx.getContentResolver().delete(
                DbContentProvider.OPERATION_URI,
                KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID + "=? AND "
                        + KEY_OP_DATE + ">=?",
                new String[]{Long.toString(accountId),
                        Long.toString(schOpId), Long.toString(date)}
        );
        if (nbDel > 0) {
            AccountTable.consolidateSums(ctx, accountId);
            if (transfertId > 0) {
                AccountTable.consolidateSums(ctx, transfertId);
            }
        }
        return nbDel;
    }

    public static int updateAllOccurrences(Context ctx, final long accountId,
                                           final long schOpId, final Operation op) {
        ContentValues args = createContentValuesFromOp(ctx, op, true);
        return ctx.getContentResolver()
                .update(DbContentProvider.OPERATION_URI,
                        args,
                        KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID
                                + "=?",
                        new String[]{Long.toString(accountId),
                                Long.toString(schOpId)}
                );
    }

    public static int disconnectAllOccurrences(Context ctx,
                                               final long accountId, final long schOpId) {
        ContentValues args = new ContentValues();
        args.put(KEY_OP_SCHEDULED_ID, 0);
        return ctx.getContentResolver()
                .update(DbContentProvider.OPERATION_URI,
                        args,
                        KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID
                                + "=?",
                        new String[]{Long.toString(accountId),
                                Long.toString(schOpId)}
                );
    }

    public static void updateOpCheckedStatus(Context ctx, final long opId, final long sum, final long accountId,
                                             final long transAccountId, boolean b) {
        ContentValues values = new ContentValues();
        values.put(KEY_OP_CHECKED, b);
        final int res =
                ctx.getContentResolver().update(Uri.parse(DbContentProvider.OPERATION_URI + "/" + opId),
                        values, null, null);
        if (res == 1) {
            AccountTable.updateCheckedOpSum(ctx, sum, accountId, transAccountId, b);
        } else {
            Log.e(TAG, "updateOpCheckedStatus should update only one operation");
        }
    }

    public static void updateOpCheckedStatus(Context ctx, Operation op, boolean b) {
        ContentValues values = new ContentValues();
        values.put(KEY_OP_CHECKED, b);
        final int res =
                ctx.getContentResolver().update(Uri.parse(DbContentProvider.OPERATION_URI + "/" + op.getmRowId()),
                        values, null, null);
        if (res == 1) {
            AccountTable.updateCheckedOpSum(ctx, op, b);
        } else {
            Log.e(TAG, "updateOpCheckedStatus should update only one operation");
        }
    }

    // UPGRADE FUNCTIONS
    static void upgradeFromV16(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(String.format(ADD_CHECKED_COLUNM, DATABASE_OPERATIONS_TABLE));
    }

    static void upgradeFromV11(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(String.format(ADD_TRANSFERT_ID_COLUNM,
                DATABASE_OPERATIONS_TABLE));
        db.execSQL(String.format(ADD_TRANSFERT_NAME_COLUNM,
                DATABASE_OPERATIONS_TABLE));

    }

    static void upgradeFromV9(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
        Cursor c = db.query(DATABASE_OPERATIONS_TABLE, new String[]{
                KEY_OP_ROWID, KEY_OP_DATE}, null, null, null, null, null);
        if (null != c) {
            if (c.moveToFirst()) {
                ContentValues values;
                do {
                    values = new ContentValues();
                    GregorianCalendar d = new GregorianCalendar();
                    d.setTimeInMillis(c.getLong(c.getColumnIndex(KEY_OP_DATE)));
                    Tools.clearTimeOfCalendar(d);
                    values.put(KEY_OP_DATE, d.getTimeInMillis());
                    db.update(DATABASE_OPERATIONS_TABLE, values, KEY_OP_ROWID
                                    + "=" + c.getLong(c.getColumnIndex(KEY_OP_ROWID)),
                            null
                    );
                } while (c.moveToNext());
            }
            c.close();
        }
    }

    static void upgradeFromV2(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
    }

    static void upgradeFromV3(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ADD_NOTES_COLUNM);
    }

    static void upgradeFromV1(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DATABASE_OP_CREATE);
        Cursor allAccounts = db.query(AccountTable.DATABASE_ACCOUNT_TABLE,
                new String[]{}, null, null, null, null, null);
        if (null != allAccounts) {
            if (allAccounts.moveToFirst()) {
                do {
                    int accountId = allAccounts.getInt(allAccounts
                            .getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID));
                    String oldTableName = "ops_of_account_" + accountId;
                    db.execSQL("INSERT INTO operations ("
                            + KEY_OP_ACCOUNT_ID
                            + ", "
                            + KEY_OP_THIRD_PARTY
                            + ", "
                            + KEY_OP_TAG
                            + ", "
                            + KEY_OP_SUM
                            + ", "
                            + KEY_OP_MODE
                            + ", "
                            + KEY_OP_DATE
                            + ", "
                            + KEY_OP_SCHEDULED_ID
                            + ") SELECT "
                            + accountId
                            + ", old.op_third_party, old.op_tag, old.op_sum, old.op_mode, old.op_date, old.op_scheduled_id FROM "
                            + oldTableName + " old;");
                    db.execSQL("DROP TABLE " + oldTableName + ";");
                } while (allAccounts.moveToNext());
            }
            allAccounts.close();
            db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE);
            db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
        }
    }

    static void upgradeFromV5(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TRIGGER on_delete_third_party");
        db.execSQL("DROP TRIGGER on_delete_mode");
        db.execSQL("DROP TRIGGER on_delete_tag");
        db.execSQL("ALTER TABLE operations RENAME TO operations_old;");
        db.execSQL(DATABASE_OP_CREATE);
        db.execSQL("INSERT INTO operations ("
                + KEY_OP_ACCOUNT_ID
                + ", "
                + KEY_OP_THIRD_PARTY
                + ", "
                + KEY_OP_TAG
                + ", "
                + KEY_OP_SUM
                + ", "
                + KEY_OP_MODE
                + ", "
                + KEY_OP_DATE
                + ", "
                + KEY_OP_SCHEDULED_ID
                + ", "
                + KEY_OP_NOTES
                + ") SELECT old.op_account_id, old.op_third_party, old.op_tag, old.op_sum, old.op_mode, old.op_date, old.op_scheduled_id, old.op_notes FROM operations_old old;");
        db.execSQL("DROP TABLE operations_old;");
        db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
    }

    static void upgradeFromV6(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE operations RENAME TO operations_old;");
        db.execSQL(DATABASE_OP_CREATE);
        Cursor c = db.query("operations_old", new String[]{KEY_OP_ROWID,
                        KEY_OP_ACCOUNT_ID, KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM,
                        KEY_OP_MODE, KEY_OP_DATE, KEY_OP_SCHEDULED_ID, KEY_OP_NOTES},
                null, null, null, null, null
        );
        if (null != c && c.moveToFirst()) {
            do {
                ContentValues initialValues = new ContentValues();
                initialValues.put(KEY_OP_THIRD_PARTY,
                        c.getInt(c.getColumnIndex(KEY_OP_THIRD_PARTY)));
                initialValues.put(KEY_OP_TAG,
                        c.getInt(c.getColumnIndex(KEY_OP_TAG)));
                double d = c.getDouble(c.getColumnIndex(KEY_OP_SUM));
                long l = Math.round(d * 100);
                initialValues.put(KEY_OP_SUM, l);
                initialValues.put(KEY_OP_ACCOUNT_ID,
                        c.getLong(c.getColumnIndex(KEY_OP_ACCOUNT_ID)));
                initialValues.put(KEY_OP_MODE,
                        c.getInt(c.getColumnIndex(KEY_OP_MODE)));
                initialValues.put(KEY_OP_DATE,
                        c.getLong(c.getColumnIndex(KEY_OP_DATE)));
                initialValues.put(KEY_OP_SCHEDULED_ID,
                        c.getLong(c.getColumnIndex(KEY_OP_SCHEDULED_ID)));
                initialValues.put(KEY_OP_NOTES,
                        c.getString(c.getColumnIndex(KEY_OP_NOTES)));
                db.insert(DATABASE_OPERATIONS_TABLE, null, initialValues);
            } while (c.moveToNext());
            c.close();
        }
        db.execSQL("DROP TABLE operations_old;");
        db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
    }
}
