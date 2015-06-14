package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.support.v4.content.CursorLoader
import android.util.Log
import fr.geobert.radis.data.Operation
import fr.geobert.radis.tools.Tools
import hirondelle.date4j.DateTime

import java.util.GregorianCalendar
import java.util.TimeZone

public object OperationTable {
    public val DATABASE_OPERATIONS_TABLE: String = "operations"
    public val KEY_OP_DATE: String = "date"
    public val KEY_OP_THIRD_PARTY: String = "third_party"
    public val KEY_OP_TAG: String = "tag"
    public val KEY_OP_MODE: String = "mode"
    public val KEY_OP_SUM: String = "sum"
    public val KEY_OP_SCHEDULED_ID: String = "scheduled_id"
    public val KEY_OP_ACCOUNT_ID: String = "account_id"
    public val KEY_OP_ROWID: String = "_id"
    public val KEY_OP_NOTES: String = "notes"
    public val KEY_OP_TRANSFERT_ACC_ID: String = "transfert_acc_id"
    public val KEY_OP_TRANSFERT_ACC_NAME: String = "transfert_acc_src_name"
    public val KEY_OP_CHECKED: String = "checked"
    public val OP_ORDERING: String = "ops." + KEY_OP_DATE + " desc, ops." + KEY_OP_ROWID + " desc"
    public val DATABASE_OP_TABLE_JOINTURE: String = DATABASE_OPERATIONS_TABLE + " ops LEFT OUTER JOIN " + InfoTables.DATABASE_THIRD_PARTIES_TABLE + " tp ON ops." + KEY_OP_THIRD_PARTY + " = tp." + InfoTables.KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN " + InfoTables.DATABASE_MODES_TABLE + " mode ON ops." + KEY_OP_MODE + " = mode." + InfoTables.KEY_MODE_ROWID + " LEFT OUTER JOIN " + InfoTables.DATABASE_TAGS_TABLE + " tag ON ops." + KEY_OP_TAG + " = tag." + InfoTables.KEY_TAG_ROWID
    public val OP_COLS_QUERY: Array<String> = arrayOf("ops." + KEY_OP_ROWID, // 0
            "tp." + InfoTables.KEY_THIRD_PARTY_NAME, "tag." + InfoTables.KEY_TAG_NAME, "mode." + InfoTables.KEY_MODE_NAME, "ops." + KEY_OP_SUM, // 4
            "ops." + KEY_OP_DATE, "ops." + KEY_OP_ACCOUNT_ID, "ops." + KEY_OP_NOTES, "ops." + KEY_OP_SCHEDULED_ID, // 8
            "ops." + KEY_OP_TRANSFERT_ACC_ID, "ops." + KEY_OP_TRANSFERT_ACC_NAME, "ops." + KEY_OP_CHECKED) // 11
    public val RESTRICT_TO_ACCOUNT: String = "(ops." + KEY_OP_ACCOUNT_ID + " = ? OR ops." + KEY_OP_TRANSFERT_ACC_ID + " = ?)"
    protected val DATABASE_OP_CREATE: String = "create table " + DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ROWID + " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY + " integer, " + KEY_OP_TAG + " integer, " + KEY_OP_SUM + " integer not null, " + KEY_OP_ACCOUNT_ID + " integer not null, " + KEY_OP_MODE + " integer, " + KEY_OP_DATE + " integer not null, " + KEY_OP_NOTES + " text, " + KEY_OP_SCHEDULED_ID + " integer, " + KEY_OP_TRANSFERT_ACC_NAME + " text, " + KEY_OP_TRANSFERT_ACC_ID + " integer not null, " + KEY_OP_CHECKED + " integer not null, " + " FOREIGN KEY (" + KEY_OP_THIRD_PARTY + ") REFERENCES " + InfoTables.DATABASE_THIRD_PARTIES_TABLE + "(" + InfoTables.KEY_THIRD_PARTY_ROWID + "), FOREIGN KEY (" + KEY_OP_TAG + ") REFERENCES " + InfoTables.DATABASE_TAGS_TABLE + "(" + InfoTables.KEY_TAG_ROWID + "), FOREIGN KEY (" + KEY_OP_MODE + ") REFERENCES " + InfoTables.DATABASE_MODES_TABLE + "(" + InfoTables.KEY_MODE_ROWID + "), FOREIGN KEY (" + KEY_OP_SCHEDULED_ID + ") REFERENCES " + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE + "(" + ScheduledOperationTable.KEY_SCHEDULED_ROWID + "));"
    protected val INDEX_ON_ACCOUNT_ID_CREATE: String = "CREATE INDEX IF NOT EXISTS account_id_idx ON " + DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ACCOUNT_ID + ")"
    protected val TRIGGER_ON_DELETE_THIRD_PARTY_CREATE: String = "CREATE TRIGGER on_delete_third_party AFTER DELETE ON " + InfoTables.DATABASE_THIRD_PARTIES_TABLE + " BEGIN UPDATE " + DATABASE_OPERATIONS_TABLE + " SET " + KEY_OP_THIRD_PARTY + " = null WHERE " + KEY_OP_THIRD_PARTY + " = old." + InfoTables.KEY_THIRD_PARTY_ROWID + "; UPDATE " + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE + " SET " + KEY_OP_THIRD_PARTY + " = null WHERE " + KEY_OP_THIRD_PARTY + " = old." + InfoTables.KEY_THIRD_PARTY_ROWID + "; END"
    protected val TRIGGER_ON_DELETE_MODE_CREATE: String = "CREATE TRIGGER on_delete_mode AFTER DELETE ON " + InfoTables.DATABASE_MODES_TABLE + " BEGIN UPDATE " + DATABASE_OPERATIONS_TABLE + " SET " + KEY_OP_MODE + " = null WHERE " + KEY_OP_MODE + " = old." + InfoTables.KEY_MODE_ROWID + "; UPDATE " + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE + " SET " + KEY_OP_MODE + " = null WHERE " + KEY_OP_MODE + " = old." + InfoTables.KEY_MODE_ROWID + "; END"
    protected val TRIGGER_ON_DELETE_TAG_CREATE: String = "CREATE TRIGGER on_delete_tag AFTER DELETE ON " + InfoTables.DATABASE_TAGS_TABLE + " BEGIN UPDATE " + DATABASE_OPERATIONS_TABLE + " SET " + KEY_OP_TAG + " = null WHERE " + KEY_OP_TAG + " = old." + InfoTables.KEY_TAG_ROWID + "; UPDATE " + ScheduledOperationTable.DATABASE_SCHEDULED_TABLE + " SET " + KEY_OP_TAG + " = null WHERE " + KEY_OP_TAG + " = old." + InfoTables.KEY_TAG_ROWID + "; END"
    protected val ADD_NOTES_COLUNM: String = "ALTER TABLE " + DATABASE_OPERATIONS_TABLE + " ADD COLUMN op_notes text"
    protected val ADD_TRANSFERT_ID_COLUNM: String = "ALTER TABLE %s ADD COLUMN " + KEY_OP_TRANSFERT_ACC_ID + " integer not null DEFAULT 0"
    protected val ADD_TRANSFERT_NAME_COLUNM: String = "ALTER TABLE %s ADD COLUMN " + KEY_OP_TRANSFERT_ACC_NAME + " text"
    protected val ADD_CHECKED_COLUNM: String = "ALTER TABLE %s ADD COLUMN " + KEY_OP_CHECKED + " integer not null DEFAULT 0"

    private val TAG = "OperationTable"

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DATABASE_OP_CREATE)
    }

    fun createMeta(db: SQLiteDatabase) {
        db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE)
    }

    public fun fetchAllOps(ctx: Context, accountId: Long): Cursor {
        val c = ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT, arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId)), OP_ORDERING)
        c?.moveToFirst()
        return c
    }

    public fun fetchAllCheckedOps(ctx: Context, accountId: Long): Cursor {
        val c = ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + OperationTable.KEY_OP_CHECKED + " = ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), Integer.toString(1)), OP_ORDERING)
        c?.moveToFirst()
        return c
    }

    public fun fetchAllUncheckedOps(ctx: Context, accountId: Long, maxDate: Long): Cursor {
        val c = ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + OperationTable.KEY_OP_CHECKED + " = ? AND ops." + KEY_OP_DATE + " <= ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), Integer.toString(0), java.lang.Long.toString(maxDate)), OP_ORDERING)
        c?.moveToFirst()
        return c
    }

    public fun computeSumFromCursor(c: Cursor, curAccount: Long): Long {
        var sum = 0L
        val sumIdx = c.getColumnIndex(KEY_OP_SUM)
        val transIdx = c.getColumnIndex(KEY_OP_TRANSFERT_ACC_ID)
        if (!c.isBeforeFirst() && !c.isAfterLast()) {
            do {
                var s = c.getLong(sumIdx)
                if (c.getLong(transIdx) == curAccount) {
                    s = -s
                }
                sum = sum + s
            } while (c.moveToNext())
        }
        return sum
    }

    fun fetchOpEarlierThan(ctx: Context, date: Long, nbOps: Int, accountId: Long): Cursor {
        Log.d(TAG, "fetchOpEarlierThan date : " + Tools.getDateStr(date) + " with limit : " + nbOps)
        val c: Cursor?
        val limit = if (nbOps == 0) null else Integer.toString(nbOps)
        c = ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " and ops." + KEY_OP_DATE + " < ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(date)), OP_ORDERING + (if (limit == null) "" else " ops._id asc LIMIT " + limit))
        c?.moveToFirst()
        return c
    }

    public fun createOp(ctx: Context, op: Operation, accountId: Long): Long {
        return OperationTable.createOp(ctx, op, accountId, true)
    }

    public fun createOp(ctx: Context, op: Operation, accountId: Long, withUpdate: Boolean): Long {
        val initialValues = ContentValues()
        var key = op.mThirdParty
        InfoTables.putKeyIdInThirdParties(ctx, key, initialValues, false)

        key = op.mTag
        InfoTables.putKeyIdInTags(ctx, key, initialValues, false)

        key = op.mMode
        InfoTables.putKeyIdInModes(ctx, key, initialValues, false)

        initialValues.put(KEY_OP_SUM, op.mSum)
        initialValues.put(KEY_OP_DATE, op.getDate())
        initialValues.put(KEY_OP_ACCOUNT_ID, accountId)
        initialValues.put(KEY_OP_NOTES, op.mNotes)
        initialValues.put(KEY_OP_SCHEDULED_ID, op.mScheduledId)
        initialValues.put(KEY_OP_TRANSFERT_ACC_ID, op.mTransferAccountId)
        initialValues.put(KEY_OP_TRANSFERT_ACC_NAME, op.mTransSrcAccName)
        initialValues.put(KEY_OP_CHECKED, if (op.mIsChecked) 1 else 0)
        val res = ctx.getContentResolver().insert(DbContentProvider.OPERATION_URI, initialValues)
        op.mRowId = java.lang.Long.parseLong(res.getLastPathSegment())
        if (op.mRowId > -1) {
            if (withUpdate) {
                AccountTable.updateProjection(ctx, accountId, op.mSum, 0, op.getDate(), -1)
                if (op.mTransferAccountId > 0) {
                    AccountTable.updateProjection(ctx, op.mTransferAccountId, -op.mSum, 0, op.getDate(), -1)
                }
                if (op.mIsChecked) {
                    AccountTable.updateCheckedOpSum(ctx, op, op.mIsChecked)
                }
            }
            return op.mRowId
        }
        Log.e(TAG, "error in creating op")
        return -1
    }

    public fun deleteOp(ctx: Context, rowId: Long, accountId: Long): Boolean {
        val c = fetchOneOp(ctx, rowId, accountId)
        if (c.moveToFirst()) {
            val opSum = c.getLong(c.getColumnIndex(KEY_OP_SUM))
            val opDate = c.getLong(c.getColumnIndex(KEY_OP_DATE))
            val checked = c.getInt(c.getColumnIndex(KEY_OP_CHECKED)) == 1
            val transfertId = c.getLong(c.getColumnIndex(KEY_OP_TRANSFERT_ACC_ID))
            c.close()
            if (ctx.getContentResolver().delete(Uri.parse("${DbContentProvider.OPERATION_URI}/$rowId"), null, null) > 0) {
                AccountTable.updateProjection(ctx, accountId, -opSum, 0, opDate, -1)
                if (transfertId > 0) {
                    AccountTable.updateProjection(ctx, transfertId, opSum, 0, opDate, -1)
                }
                if (checked) {
                    AccountTable.updateCheckedOpSum(ctx, opSum, accountId, transfertId, false)
                }
                return true
            }
        }
        return false
    }

    public fun fetchLastOp(ctx: Context, accountId: Long): Cursor {
        return ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " = (SELECT max(ops2." + KEY_OP_DATE + ") FROM " + DATABASE_OPERATIONS_TABLE + " ops2 WHERE (ops2." + KEY_OP_ACCOUNT_ID + " = ? OR ops2." + KEY_OP_TRANSFERT_ACC_ID + " = ?)) ", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(accountId)), OP_ORDERING)
    }

    public fun fetchLastOpSince(ctx: Context, accountId: Long, time: Long): Cursor {
        return ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " = (SELECT max(ops2." + KEY_OP_DATE + ") FROM " + DATABASE_OPERATIONS_TABLE + " ops2 WHERE (ops2." + KEY_OP_ACCOUNT_ID + " = ? OR ops2." + KEY_OP_TRANSFERT_ACC_ID + " = ?) AND ops2." + KEY_OP_DATE + " < ?) ", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(time)), OP_ORDERING)
    }

    public fun fetchOneOp(ctx: Context, rowId: Long, accountId: Long): Cursor {
        val c = ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_ROWID + " = ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(rowId)), null)
        c?.moveToFirst()
        return c
    }

    public fun getOpsWithStartDateLoader(ctx: Context, earliestOpDate: Long?, accountId: Long): CursorLoader {
        return CursorLoader(ctx, DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " >= ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(earliestOpDate!!)), OP_ORDERING)
    }

    public fun getOpsBetweenDateLoader(ctx: Context, earliestOpDate: Long, latestOpDate: Long, accountId: Long): CursorLoader {
        return CursorLoader(ctx, DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " >= ? AND ops." + KEY_OP_DATE + " < ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(earliestOpDate), java.lang.Long.toString(latestOpDate)), OP_ORDERING)
    }

    public fun getOpsBetweenDate(ctx: Context, earliestOpDate: DateTime, latestOpDate: DateTime, accountId: Long): Cursor {
        val tz = TimeZone.getDefault()
        return ctx.getContentResolver().query(DbContentProvider.OPERATION_JOINED_URI, OP_COLS_QUERY, RESTRICT_TO_ACCOUNT + " AND ops." + KEY_OP_DATE + " >= ? AND ops." + KEY_OP_DATE + " <= ?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(accountId), java.lang.Long.toString(earliestOpDate.getMilliseconds(tz)), // TODO once converted to Kotlin, use TIME_ZONE
                java.lang.Long.toString(latestOpDate.getMilliseconds(tz))), OP_ORDERING)
    }

    // used in update op only
    private fun createContentValuesFromOp(ctx: Context, op: Operation, updateOccurrences: Boolean): ContentValues {
        val args = ContentValues()

        var key = op.mThirdParty
        InfoTables.putKeyIdInThirdParties(ctx, key, args, true)

        key = op.mTag
        InfoTables.putKeyIdInTags(ctx, key, args, true)

        key = op.mMode
        InfoTables.putKeyIdInModes(ctx, key, args, true)

        args.put(KEY_OP_SUM, op.mSum)
        args.put(KEY_OP_NOTES, op.mNotes)
        args.put(KEY_OP_TRANSFERT_ACC_ID, op.mTransferAccountId)
        args.put(KEY_OP_TRANSFERT_ACC_NAME, op.mTransSrcAccName)
        args.put(KEY_OP_CHECKED, if (op.mIsChecked) 1 else 0)
        if (!updateOccurrences) {
            args.put(KEY_OP_DATE, op.getDate())
            args.put(KEY_OP_SCHEDULED_ID, op.mScheduledId)
        }
        return args
    }

    // return if need to update OP_SUM
    public fun updateOp(ctx: Context, rowId: Long, op: Operation, originalOp: Operation): Boolean {
        val args = createContentValuesFromOp(ctx, op, false)
        if (ctx.getContentResolver().update(Uri.parse("${DbContentProvider.OPERATION_URI}/$rowId"), args, null, null) > 0) {
            AccountTable.updateProjection(ctx, op.mAccountId, op.mSum, originalOp.mSum, op.getDate(), originalOp.getDate())
            if (op.mTransferAccountId > 0) {
                if (originalOp.mTransferAccountId <= 0) {
                    // op was not a transfert, it is like adding an op in transfertAccountId
                    AccountTable.updateProjection(ctx, op.mTransferAccountId, -op.mSum, 0, op.getDate(), -1)
                } else {
                    // op was a transfert
                    if (originalOp.mTransferAccountId == op.mTransferAccountId) {
                        // op was a transfert on same account, update with sum diff
                        AccountTable.updateProjection(ctx, op.mTransferAccountId, -op.mSum, -originalOp.mSum, op.getDate(), originalOp.getDate())
                    } else {
                        // op was a transfert to another account
                        // update new transfert account
                        AccountTable.updateProjection(ctx, op.mTransferAccountId, -op.mSum, 0, op.getDate(), -1)
                        // remove the original sum on original transfert account
                        AccountTable.updateProjection(ctx, originalOp.mTransferAccountId, originalOp.mSum, 0, op.getDate(), originalOp.getDate())
                    }
                }
            } else if (originalOp.mTransferAccountId > 0) {
                // op become not transfert, but was a transfert
                AccountTable.updateProjection(ctx, originalOp.mTransferAccountId, originalOp.mSum, 0, op.getDate(), -1)
            }

            if (originalOp.mIsChecked != op.mIsChecked) {
                AccountTable.updateCheckedOpSum(ctx, op.mSum, op.mAccountId, op.mTransferAccountId, op.mIsChecked)
            }

            return true
        }
        return false
    }

    public fun deleteAllOccurrences(ctx: Context, accountId: Long, schOpId: Long, transfertId: Long): Int {
        val nb = ctx.getContentResolver().delete(DbContentProvider.OPERATION_URI, KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID + "=?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(schOpId)))
        if (nb > 0) {
            AccountTable.consolidateSums(ctx, accountId)
            if (transfertId > 0) {
                AccountTable.consolidateSums(ctx, transfertId)
            }
        }
        return nb
    }

    public fun deleteAllFutureOccurrences(ctx: Context, accountId: Long, schOpId: Long, date: Long, transfertId: Long): Int {
        val nbDel = ctx.getContentResolver().delete(DbContentProvider.OPERATION_URI, KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID + "=? AND " + KEY_OP_DATE + ">=?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(schOpId), java.lang.Long.toString(date)))
        if (nbDel > 0) {
            AccountTable.consolidateSums(ctx, accountId)
            if (transfertId > 0) {
                AccountTable.consolidateSums(ctx, transfertId)
            }
        }
        return nbDel
    }

    public fun updateAllOccurrences(ctx: Context, accountId: Long, schOpId: Long, op: Operation): Int {
        val args = createContentValuesFromOp(ctx, op, true)
        return ctx.getContentResolver().update(DbContentProvider.OPERATION_URI, args, KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID + "=?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(schOpId)))
    }

    public fun disconnectAllOccurrences(ctx: Context, accountId: Long, schOpId: Long): Int {
        val args = ContentValues()
        args.put(KEY_OP_SCHEDULED_ID, 0)
        return ctx.getContentResolver().update(DbContentProvider.OPERATION_URI, args, KEY_OP_ACCOUNT_ID + "=? AND " + KEY_OP_SCHEDULED_ID + "=?", arrayOf(java.lang.Long.toString(accountId), java.lang.Long.toString(schOpId)))
    }

    public fun updateOpCheckedStatus(ctx: Context, opId: Long, sum: Long, accountId: Long, transAccountId: Long, b: Boolean) {
        val values = ContentValues()
        values.put(KEY_OP_CHECKED, b)
        val res = ctx.getContentResolver().update(Uri.parse("${DbContentProvider.OPERATION_URI}/$opId"), values, null, null)
        if (res == 1) {
            AccountTable.updateCheckedOpSum(ctx, sum, accountId, transAccountId, b)
        } else {
            Log.e(TAG, "updateOpCheckedStatus should update only one operation")
        }
    }

    public fun updateOpCheckedStatus(ctx: Context, op: Operation, b: Boolean) {
        val values = ContentValues()
        values.put(KEY_OP_CHECKED, b)
        val res = ctx.getContentResolver().update(Uri.parse("${DbContentProvider.OPERATION_URI}/${op.mRowId}"), values, null, null)
        if (res == 1) {
            AccountTable.updateCheckedOpSum(ctx, op, b)
        } else {
            Log.e(TAG, "updateOpCheckedStatus should update only one operation")
        }
    }

    // UPGRADE FUNCTIONS
    fun upgradeFromV16(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ADD_CHECKED_COLUNM.format(DATABASE_OPERATIONS_TABLE))
    }

    fun upgradeFromV11(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ADD_TRANSFERT_ID_COLUNM.format(DATABASE_OPERATIONS_TABLE))
        db.execSQL(ADD_TRANSFERT_NAME_COLUNM.format(DATABASE_OPERATIONS_TABLE))

    }

    fun upgradeFromV9(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val c = db.query(DATABASE_OPERATIONS_TABLE, arrayOf(KEY_OP_ROWID, KEY_OP_DATE), null, null, null, null, null)
        if (null != c) {
            if (c.moveToFirst()) {
                val values: ContentValues
                do {
                    values = ContentValues()
                    val d = GregorianCalendar()
                    d.setTimeInMillis(c.getLong(c.getColumnIndex(KEY_OP_DATE)))
                    Tools.clearTimeOfCalendar(d)
                    values.put(KEY_OP_DATE, d.getTimeInMillis())
                    db.update(DATABASE_OPERATIONS_TABLE, values, KEY_OP_ROWID + "=" + c.getLong(c.getColumnIndex(KEY_OP_ROWID)), null)
                } while (c.moveToNext())
            }
            c.close()
        }
    }

    fun upgradeFromV2(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE)
    }

    fun upgradeFromV3(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ADD_NOTES_COLUNM)
    }

    fun upgradeFromV1(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DATABASE_OP_CREATE)
        val allAccounts = db.query(AccountTable.DATABASE_ACCOUNT_TABLE, arrayOf<String>(), null, null, null, null, null)
        if (null != allAccounts) {
            if (allAccounts.moveToFirst()) {
                do {
                    val accountId = allAccounts.getInt(allAccounts.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID))
                    val oldTableName = "ops_of_account_" + accountId
                    db.execSQL("INSERT INTO operations (" + KEY_OP_ACCOUNT_ID + ", " + KEY_OP_THIRD_PARTY + ", " + KEY_OP_TAG + ", " + KEY_OP_SUM + ", " + KEY_OP_MODE + ", " + KEY_OP_DATE + ", " + KEY_OP_SCHEDULED_ID + ") SELECT " + accountId + ", old.op_third_party, old.op_tag, old.op_sum, old.op_mode, old.op_date, old.op_scheduled_id FROM " + oldTableName + " old;")
                    db.execSQL("DROP TABLE " + oldTableName + ";")
                } while (allAccounts.moveToNext())
            }
            allAccounts.close()
            db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE)
            db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE)
        }
    }

    fun upgradeFromV5(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TRIGGER on_delete_third_party")
        db.execSQL("DROP TRIGGER on_delete_mode")
        db.execSQL("DROP TRIGGER on_delete_tag")
        db.execSQL("ALTER TABLE operations RENAME TO operations_old;")
        db.execSQL(DATABASE_OP_CREATE)
        db.execSQL("INSERT INTO operations (" + KEY_OP_ACCOUNT_ID + ", " + KEY_OP_THIRD_PARTY + ", " + KEY_OP_TAG + ", " + KEY_OP_SUM + ", " + KEY_OP_MODE + ", " + KEY_OP_DATE + ", " + KEY_OP_SCHEDULED_ID + ", " + KEY_OP_NOTES + ") SELECT old.op_account_id, old.op_third_party, old.op_tag, old.op_sum, old.op_mode, old.op_date, old.op_scheduled_id, old.op_notes FROM operations_old old;")
        db.execSQL("DROP TABLE operations_old;")
        db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE)
    }

    fun upgradeFromV6(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("ALTER TABLE operations RENAME TO operations_old;")
        db.execSQL(DATABASE_OP_CREATE)
        val c = db.query("operations_old", arrayOf(KEY_OP_ROWID, KEY_OP_ACCOUNT_ID, KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM, KEY_OP_MODE, KEY_OP_DATE, KEY_OP_SCHEDULED_ID, KEY_OP_NOTES), null, null, null, null, null)
        if (null != c && c.moveToFirst()) {
            do {
                val initialValues = ContentValues()
                initialValues.put(KEY_OP_THIRD_PARTY, c.getInt(c.getColumnIndex(KEY_OP_THIRD_PARTY)))
                initialValues.put(KEY_OP_TAG, c.getInt(c.getColumnIndex(KEY_OP_TAG)))
                val d = c.getDouble(c.getColumnIndex(KEY_OP_SUM))
                val l = Math.round(d * 100)
                initialValues.put(KEY_OP_SUM, l)
                initialValues.put(KEY_OP_ACCOUNT_ID, c.getLong(c.getColumnIndex(KEY_OP_ACCOUNT_ID)))
                initialValues.put(KEY_OP_MODE, c.getInt(c.getColumnIndex(KEY_OP_MODE)))
                initialValues.put(KEY_OP_DATE, c.getLong(c.getColumnIndex(KEY_OP_DATE)))
                initialValues.put(KEY_OP_SCHEDULED_ID, c.getLong(c.getColumnIndex(KEY_OP_SCHEDULED_ID)))
                initialValues.put(KEY_OP_NOTES, c.getString(c.getColumnIndex(KEY_OP_NOTES)))
                db.insert(DATABASE_OPERATIONS_TABLE, null, initialValues)
            } while (c.moveToNext())
            c.close()
        }
        db.execSQL("DROP TABLE operations_old;")
        db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE)
        db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE)
    }
}
