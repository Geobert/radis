package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import fr.geobert.radis.data.ScheduledOperation

public class ScheduledOperationTable {

    public fun fetchScheduledOpsOfAccount(db: SQLiteDatabase, accountId: Long): Cursor {
        val c = db.query(DATABASE_SCHEDULED_TABLE_JOINTURE,
                SCHEDULED_OP_COLS_QUERY, "sch.$KEY_SCHEDULED_ACCOUNT_ID = $accountId", null, null, null,
                SCHEDULED_OP_ORDERING)
        c?.moveToFirst()
        return c
    }

    companion object {
        private val TAG = "ScheduledOperationTable"
        val DATABASE_SCHEDULED_TABLE = "scheduled_ops"
        public val KEY_SCHEDULED_END_DATE: String = "end_date"
        public val KEY_SCHEDULED_PERIODICITY: String = "periodicity"
        public val KEY_SCHEDULED_ACCOUNT_ID: String = "scheduled_account_id"
        public val KEY_SCHEDULED_ROWID: String = "_id"
        public val KEY_SCHEDULED_PERIODICITY_UNIT: String = "periodicity_units"

        protected val DATABASE_SCHEDULED_CREATE: String = "create table $DATABASE_SCHEDULED_TABLE(" +
                "$KEY_SCHEDULED_ROWID integer primary key autoincrement, " +
                "${OperationTable.KEY_OP_THIRD_PARTY} integer, " +
                "${OperationTable.KEY_OP_TAG} integer, " +
                "${OperationTable.KEY_OP_SUM} integer not null, " +
                "$KEY_SCHEDULED_ACCOUNT_ID integer not null, " +
                "${OperationTable.KEY_OP_MODE} integer, " +
                "${OperationTable.KEY_OP_DATE} integer not null, " +
                "$KEY_SCHEDULED_END_DATE integer, " +
                "$KEY_SCHEDULED_PERIODICITY integer, " +
                "$KEY_SCHEDULED_PERIODICITY_UNIT integer not null, " +
                "${OperationTable.KEY_OP_NOTES} text, " +
                "${OperationTable.KEY_OP_TRANSFERT_ACC_NAME} text, " +
                "${OperationTable.KEY_OP_TRANSFERT_ACC_ID} integer not null, " +
                "FOREIGN KEY (${OperationTable.KEY_OP_THIRD_PARTY}) REFERENCES ${InfoTables.DATABASE_THIRD_PARTIES_TABLE}(${InfoTables.KEY_THIRD_PARTY_ROWID}), " +
                "FOREIGN KEY (" + OperationTable.KEY_OP_TAG + ") REFERENCES " + InfoTables.DATABASE_TAGS_TABLE + "(" + InfoTables.KEY_TAG_ROWID + "), FOREIGN KEY (" + OperationTable.KEY_OP_MODE + ") REFERENCES " + InfoTables.DATABASE_MODES_TABLE + "(" + InfoTables.KEY_MODE_ROWID + "));"

        public val SCHEDULED_OP_ORDERING: String = "sch." + OperationTable.KEY_OP_DATE + " desc, sch." + KEY_SCHEDULED_ROWID + " desc"

        public val DATABASE_SCHEDULED_TABLE_JOINTURE: String = DATABASE_SCHEDULED_TABLE + " sch LEFT OUTER JOIN " + InfoTables.DATABASE_THIRD_PARTIES_TABLE + " tp ON sch." + OperationTable.KEY_OP_THIRD_PARTY + " = tp." + InfoTables.KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN " + InfoTables.DATABASE_MODES_TABLE + " mode ON sch." + OperationTable.KEY_OP_MODE + " = mode." + InfoTables.KEY_MODE_ROWID + " LEFT OUTER JOIN " + InfoTables.DATABASE_TAGS_TABLE + " tag ON sch." + OperationTable.KEY_OP_TAG + " = tag." + InfoTables.KEY_TAG_ROWID + " LEFT OUTER JOIN " + AccountTable.DATABASE_ACCOUNT_TABLE + " acc ON sch." + KEY_SCHEDULED_ACCOUNT_ID + " = acc." + AccountTable.KEY_ACCOUNT_ROWID

        public val SCHEDULED_OP_COLS_QUERY: Array<String> = arrayOf("sch." + KEY_SCHEDULED_ROWID, "tp." + InfoTables.KEY_THIRD_PARTY_NAME, "tag." + InfoTables.KEY_TAG_NAME, "mode." + InfoTables.KEY_MODE_NAME, "sch." + OperationTable.KEY_OP_SUM, "sch." + OperationTable.KEY_OP_DATE, "sch." + KEY_SCHEDULED_ACCOUNT_ID, "acc." + AccountTable.KEY_ACCOUNT_NAME, "sch." + OperationTable.KEY_OP_NOTES, "sch." + KEY_SCHEDULED_END_DATE, "sch." + KEY_SCHEDULED_PERIODICITY, "sch." + KEY_SCHEDULED_PERIODICITY_UNIT, "sch." + OperationTable.KEY_OP_TRANSFERT_ACC_ID, "sch." + OperationTable.KEY_OP_TRANSFERT_ACC_NAME)

        protected val TRIGGER_ON_DELETE_SCHED_CREATE: String = "CREATE TRIGGER on_delete_sch_op AFTER DELETE ON " + DATABASE_SCHEDULED_TABLE + " BEGIN UPDATE " + OperationTable.DATABASE_OPERATIONS_TABLE + " SET " + OperationTable.KEY_OP_SCHEDULED_ID + " = 0 WHERE " + OperationTable.KEY_OP_SCHEDULED_ID + " = old." + ScheduledOperationTable.KEY_SCHEDULED_ROWID + "; END"

        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_SCHEDULED_CREATE)
            db.execSQL(TRIGGER_ON_DELETE_SCHED_CREATE)
        }

        fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            when (oldVersion) {
                5 -> {
                    db.execSQL(DATABASE_SCHEDULED_CREATE)
                    upgradeFromV6(db, oldVersion, newVersion)
                    upgradeFromV11(db, oldVersion, newVersion)
                    upgradeFromV12(db, oldVersion, newVersion)
                }
                6 -> {
                    upgradeFromV6(db, oldVersion, newVersion)
                    upgradeFromV11(db, oldVersion, newVersion)
                    upgradeFromV12(db, oldVersion, newVersion)
                }
                11 -> {
                    upgradeFromV11(db, oldVersion, newVersion)
                    upgradeFromV12(db, oldVersion, newVersion)
                }
                12 -> upgradeFromV12(db, oldVersion, newVersion)
            }
        }

        public fun fetchAllScheduledOps(ctx: Context): Cursor {
            val c = ctx.contentResolver.query(
                    DbContentProvider.SCHEDULED_JOINED_OP_URI,
                    SCHEDULED_OP_COLS_QUERY, null, null, SCHEDULED_OP_ORDERING)
            c?.moveToFirst()
            return c
        }

        public fun updateAllOccurences(ctx: Context, op: ScheduledOperation, prevSum: Long, rowId: Long) {
            Log.d(TAG, "updateAllOccurences")
            val accountId = op.mAccountId
            OperationTable.updateAllOccurrences(ctx, accountId, rowId, op)
            AccountTable.consolidateSums(ctx, accountId)
        }

        public fun deleteAllOccurences(ctx: Context, schOpId: Long) {
            val schOp = fetchOneScheduledOp(ctx, schOpId)
            val accountId = schOp!!.getLong(schOp.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID))
            val transfertId = schOp.getLong(schOp.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))
            OperationTable.deleteAllOccurrences(ctx, accountId, schOpId, transfertId)
            AccountTable.consolidateSums(ctx, accountId)
            schOp.close()
        }

        public fun fetchOneScheduledOp(ctx: Context, rowId: Long): Cursor? {
            val c = ctx.contentResolver.query(
                    Uri.parse("${DbContentProvider.SCHEDULED_JOINED_OP_URI}/$rowId"), SCHEDULED_OP_COLS_QUERY, null, null, null)
            c?.moveToFirst()
            return c
        }

        public fun createScheduledOp(ctx: Context, op: ScheduledOperation): Long {
            val initialValues = ContentValues()
            var key = op.mThirdParty
            InfoTables.putKeyIdInThirdParties(ctx, key, initialValues, false)

            key = op.mTag
            InfoTables.putKeyIdInTags(ctx, key, initialValues, false)

            key = op.mMode
            InfoTables.putKeyIdInModes(ctx, key, initialValues, false)

            initialValues.put(OperationTable.KEY_OP_SUM, op.mSum)
            initialValues.put(OperationTable.KEY_OP_DATE, op.getDate())
            initialValues.put(OperationTable.KEY_OP_TRANSFERT_ACC_ID, op.mTransferAccountId)
            initialValues.put(OperationTable.KEY_OP_TRANSFERT_ACC_NAME, op.mTransSrcAccName)
            initialValues.put(OperationTable.KEY_OP_NOTES, op.mNotes)

            initialValues.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId)
            initialValues.put(KEY_SCHEDULED_END_DATE, op.getEndDate())
            initialValues.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity)
            initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit)
            val res = ctx.contentResolver.insert(
                    DbContentProvider.SCHEDULED_OP_URI, initialValues)
            return java.lang.Long.parseLong(res.lastPathSegment)
        }

        public fun updateScheduledOp(ctx: Context, rowId: Long,
                                     op: ScheduledOperation, isUpdatedFromOccurence: Boolean): Boolean {
            Log.d(TAG, "updateScheduledOp")
            val args = ContentValues()

            var key = op.mThirdParty
            InfoTables.putKeyIdInThirdParties(ctx, key, args, true)

            key = op.mTag
            InfoTables.putKeyIdInTags(ctx, key, args, true)

            key = op.mMode
            InfoTables.putKeyIdInModes(ctx, key, args, true)

            args.put(OperationTable.KEY_OP_SUM, op.mSum)
            args.put(OperationTable.KEY_OP_NOTES, op.mNotes)
            args.put(OperationTable.KEY_OP_TRANSFERT_ACC_ID, op.mTransferAccountId)
            args.put(OperationTable.KEY_OP_TRANSFERT_ACC_NAME, op.mTransSrcAccName)
            if (!isUpdatedFromOccurence) {
                // update from schedule editor
                args.put(KEY_SCHEDULED_END_DATE, op.getEndDate())
                args.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity)
                args.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit)
                args.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId)
                args.put(OperationTable.KEY_OP_DATE, op.getDate())
            }
            return ctx.contentResolver.update(
                    Uri.parse("${DbContentProvider.SCHEDULED_OP_URI}/$rowId"),
                    args, null, null) > 0
        }

        fun deleteScheduledOpOfAccount(ctx: Context, accountId: Long): Boolean {
            return ctx.contentResolver.delete(
                    DbContentProvider.SCHEDULED_OP_URI,
                    KEY_SCHEDULED_ACCOUNT_ID + "=?",
                    arrayOf(java.lang.Long.toString(accountId))) > 0
        }

        public fun deleteScheduledOp(ctx: Context, schOpId: Long): Boolean {
            return ctx.contentResolver.delete(
                    Uri.parse("${DbContentProvider.SCHEDULED_OP_URI}/$schOpId"),
                    null, null) > 0
        }

        // UPGRADE FUNCTIONS

        fun upgradeFromV12(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            try {
                db.execSQL(TRIGGER_ON_DELETE_SCHED_CREATE)
            } catch (e: SQLiteException) {
                // nothing to do
            }

        }

        fun upgradeFromV11(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(OperationTable.ADD_TRANSFERT_ID_COLUNM.format(DATABASE_SCHEDULED_TABLE))
            db.execSQL(OperationTable.ADD_TRANSFERT_NAME_COLUNM.format(DATABASE_SCHEDULED_TABLE))
        }

        fun upgradeFromV6(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("ALTER TABLE scheduled_ops RENAME TO scheduled_ops_old;")
            db.execSQL(DATABASE_SCHEDULED_CREATE)
            val c = db.query("scheduled_ops_old", arrayOf(KEY_SCHEDULED_ROWID, OperationTable.KEY_OP_THIRD_PARTY,
                    OperationTable.KEY_OP_TAG, OperationTable.KEY_OP_SUM, KEY_SCHEDULED_ACCOUNT_ID,
                    OperationTable.KEY_OP_MODE, OperationTable.KEY_OP_DATE, KEY_SCHEDULED_END_DATE,
                    KEY_SCHEDULED_PERIODICITY, KEY_SCHEDULED_PERIODICITY_UNIT, OperationTable.KEY_OP_NOTES),
                    null, null, null, null, null)
            if (null != c && c.moveToFirst()) {
                do {
                    var initialValues = ContentValues()
                    initialValues.put(OperationTable.KEY_OP_THIRD_PARTY, c.getInt(c.getColumnIndex(OperationTable.KEY_OP_THIRD_PARTY)))
                    initialValues.put(OperationTable.KEY_OP_TAG,
                            c.getInt(c.getColumnIndex(OperationTable.KEY_OP_TAG)))
                    val d = c.getDouble(c.getColumnIndex(OperationTable.KEY_OP_SUM))
                    val l = Math.round(d * 100)
                    initialValues.put(OperationTable.KEY_OP_SUM, l)
                    initialValues.put(KEY_SCHEDULED_ACCOUNT_ID,
                            c.getInt(c.getColumnIndex(KEY_SCHEDULED_ACCOUNT_ID)))
                    initialValues.put(OperationTable.KEY_OP_MODE,
                            c.getInt(c.getColumnIndex(OperationTable.KEY_OP_MODE)))
                    initialValues.put(OperationTable.KEY_OP_DATE, c.getLong(c.getColumnIndex(OperationTable.KEY_OP_DATE)))
                    initialValues.put(KEY_SCHEDULED_END_DATE,
                            c.getLong(c.getColumnIndex(KEY_SCHEDULED_END_DATE)))
                    initialValues.put(KEY_SCHEDULED_PERIODICITY,
                            c.getInt(c.getColumnIndex(KEY_SCHEDULED_PERIODICITY)))
                    initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT, c.getInt(c.getColumnIndex(KEY_SCHEDULED_PERIODICITY_UNIT)))
                    initialValues.put(OperationTable.KEY_OP_NOTES, c.getString(c.getColumnIndex(OperationTable.KEY_OP_NOTES)))
                    val id = db.insert(DATABASE_SCHEDULED_TABLE, null,
                            initialValues)
                    initialValues = ContentValues()
                    initialValues.put(OperationTable.KEY_OP_SCHEDULED_ID, id)
                    db.update(
                            OperationTable.DATABASE_OPERATIONS_TABLE,
                            initialValues,
                            OperationTable.KEY_OP_SCHEDULED_ID + "=" + c.getLong(c.getColumnIndex(KEY_SCHEDULED_ROWID)),
                            null)

                } while (c.moveToNext())
                c.close()
            }
            db.execSQL("DROP TABLE scheduled_ops_old;")
        }

        fun upgradeFromV5(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(DATABASE_SCHEDULED_CREATE)
        }
    }
}
