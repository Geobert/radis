package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.support.v4.content.CursorLoader
import android.util.Log
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.tools.ProjectionDateController
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.formatDate
import fr.geobert.radis.tools.parseDate
import fr.geobert.radis.ui.ConfigFragment
import java.lang
import java.text.ParseException
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import kotlin.platform.platformStatic

public class AccountTable {
    companion object {
        private val TAG = "AccountTable"
        val DATABASE_ACCOUNT_TABLE = "accounts"
        public val PROJECTION_FURTHEST: Int = 0
        public val PROJECTION_DAY_OF_NEXT_MONTH: Int = 1
        public val PROJECTION_ABSOLUTE_DATE: Int = 2
        public val KEY_ACCOUNT_NAME: String = "account_name"
        public val KEY_ACCOUNT_DESC: String = "account_desc"
        public val KEY_ACCOUNT_START_SUM: String = "account_start_sum"
        public val KEY_ACCOUNT_CUR_SUM: String = "account_current_sum" // according to current projection mode
        public val KEY_ACCOUNT_OP_SUM: String = "account_operations_sum"
        public val KEY_ACCOUNT_CURRENCY: String = "account_currency"
        public val KEY_ACCOUNT_ROWID: String = "_id"
        public val KEY_ACCOUNT_CUR_SUM_DATE: String = "account_current_sum_date" // according to current projection mode
        public val KEY_ACCOUNT_PROJECTION_MODE: String = "account_projection_mode"
        public val KEY_ACCOUNT_PROJECTION_DATE: String = "account_projection_date"
        public val KEY_ACCOUNT_CHECKED_OP_SUM: String = "account_checked_op_sum"

        // booleans
        public val KEY_ACCOUNT_OVERRIDE_HIDE_QUICK_ADD: String = "account_over_hide_quick_add"
        public val KEY_ACCOUNT_OVERRIDE_INSERT_DATE: String = "account_over_insert_date"
        public val KEY_ACCOUNT_OVERRIDE_INVERT_QUICKADD_COMPLETION: String = "account_over_quickadd_completion"
        public val KEY_ACCOUNT_OVERRIDE_USE_WEIGHTED_INFO: String = "account_over_use_weighted_info"
        public val KEY_ACCOUNT_OVERRIDE_NB_MONTH_AHEAD: String = "account_over_nb_month_ahead"

        // values of preferences
        public val KEY_ACCOUNT_HIDE_QUICK_ADD: String = "account_hide_quick_add"
        public val KEY_ACCOUNT_INSERT_DATE: String = "account_insert_date"
        public val KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION: String = "account_quickadd_completion"
        public val KEY_ACCOUNT_USE_WEIGHTED_INFO: String = "account_use_weighted_info"
        public val KEY_ACCOUNT_LAST_INSERTION_DATE: String = "account_last_insertion_date"
        public val KEY_ACCOUNT_NB_MONTH_AHEAD: String = "account_nb_month_ahead"

        // light request for spinners
        public val ACCOUNT_ID_AND_NAME_COLS: Array<String> = array(KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME)

        public val ACCOUNT_FULL_COLS: Array<String> = array(KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM,
                KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_PROJECTION_MODE, KEY_ACCOUNT_PROJECTION_DATE,
                KEY_ACCOUNT_DESC, KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_OP_SUM, KEY_ACCOUNT_CHECKED_OP_SUM,
                KEY_ACCOUNT_OVERRIDE_HIDE_QUICK_ADD, KEY_ACCOUNT_OVERRIDE_INSERT_DATE,
                KEY_ACCOUNT_OVERRIDE_INVERT_QUICKADD_COMPLETION, KEY_ACCOUNT_OVERRIDE_USE_WEIGHTED_INFO,
                KEY_ACCOUNT_HIDE_QUICK_ADD, KEY_ACCOUNT_INSERT_DATE,
                KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION, KEY_ACCOUNT_USE_WEIGHTED_INFO, KEY_ACCOUNT_LAST_INSERTION_DATE,
                KEY_ACCOUNT_OVERRIDE_NB_MONTH_AHEAD, KEY_ACCOUNT_NB_MONTH_AHEAD)

        private val DATABASE_ACCOUNT_CREATE_v7 = "create table $DATABASE_ACCOUNT_TABLE($KEY_ACCOUNT_ROWID integer primary key autoincrement, " +
                "$KEY_ACCOUNT_NAME text not null, $KEY_ACCOUNT_DESC text not null, $KEY_ACCOUNT_START_SUM integer not null, " +
                "$KEY_ACCOUNT_OP_SUM integer not null, $KEY_ACCOUNT_CUR_SUM integer not null, " +
                "$KEY_ACCOUNT_CUR_SUM_DATE integer not null, $KEY_ACCOUNT_CURRENCY text not null);"

        private val DATABASE_ACCOUNT_CREATE = "create table $DATABASE_ACCOUNT_TABLE($KEY_ACCOUNT_ROWID integer primary key autoincrement, " +
                "$KEY_ACCOUNT_NAME text not null, $KEY_ACCOUNT_DESC text not null, $KEY_ACCOUNT_START_SUM integer not null, " +
                "$KEY_ACCOUNT_OP_SUM integer not null, $KEY_ACCOUNT_CUR_SUM integer not null, $KEY_ACCOUNT_CUR_SUM_DATE integer not null, " +
                "$KEY_ACCOUNT_CURRENCY text not null, $KEY_ACCOUNT_PROJECTION_MODE integer not null, " +
                "$KEY_ACCOUNT_PROJECTION_DATE string, $KEY_ACCOUNT_CHECKED_OP_SUM integer not null, " +
                "$KEY_ACCOUNT_OVERRIDE_HIDE_QUICK_ADD integer not null, $KEY_ACCOUNT_HIDE_QUICK_ADD integer not null, " +
                "$KEY_ACCOUNT_OVERRIDE_INVERT_QUICKADD_COMPLETION integer not null, $KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION integer not null, " +
                "$KEY_ACCOUNT_OVERRIDE_USE_WEIGHTED_INFO integer not null, $KEY_ACCOUNT_USE_WEIGHTED_INFO integer not null, " +
                "$KEY_ACCOUNT_OVERRIDE_INSERT_DATE integer not null, $KEY_ACCOUNT_INSERT_DATE integer not null, " +
                "$KEY_ACCOUNT_LAST_INSERTION_DATE integer not null, $KEY_ACCOUNT_OVERRIDE_NB_MONTH_AHEAD integer not null, $KEY_ACCOUNT_NB_MONTH_AHEAD integer not null);"

        protected val ADD_CUR_DATE_COLUNM: String = "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_CUR_SUM_DATE integer not null DEFAULT 0"

        protected val ADD_PROJECTION_MODE_COLUNM: String = "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_PROJECTION_MODE integer not null DEFAULT 0"

        protected val ADD_PROJECTION_MODE_DATE: String = "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_PROJECTION_DATE string"

        protected val ADD_CHECKED_SUM_COLUNM: String = "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_CHECKED_OP_SUM integer not null DEFAULT 0"

        protected val ADD_OVERRIDE_HIDE_QUICK_ADD_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_OVERRIDE_HIDE_QUICK_ADD integer not null DEFAULT 0"
        protected val ADD_HIDE_QUICK_ADD_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_HIDE_QUICK_ADD integer not null DEFAULT 0"
        protected val ADD_OVERRIDE_INVERT_QUiCKADD_COMPLETION_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_OVERRIDE_INVERT_QUICKADD_COMPLETION integer not null DEFAULT 0"
        protected val ADD_INVERT_QUiCKADD_COMPLETION_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION integer not null DEFAULT 0"
        protected val ADD_OVERRIDE_USE_WEIGHTED_INFO_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_OVERRIDE_USE_WEIGHTED_INFO integer not null DEFAULT 0"
        protected val ADD_USE_WEIGHTED_INFO_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_USE_WEIGHTED_INFO integer not null DEFAULT 0"
        protected val ADD_OVERRIDE_INSERT_DATE_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_OVERRIDE_INSERT_DATE integer not null DEFAULT 0"
        protected val ADD_INSERT_DATE_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_INSERT_DATE integer not null DEFAULT 0"
        protected val ADD_LAST_INSERT_DATE_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_LAST_INSERTION_DATE integer not null DEFAULT 0"
        protected val ADD_OVERRIDE_NB_MONTH_AHEAD_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_OVERRIDE_NB_MONTH_AHEAD integer not null DEFAULT 0"
        protected val ADD_NB_MONTH_AHEAD_COLUMN: String =
                "ALTER TABLE $DATABASE_ACCOUNT_TABLE ADD COLUMN $KEY_ACCOUNT_NB_MONTH_AHEAD integer not null DEFAULT ${ConfigFragment.DEFAULT_NB_MONTH_AHEAD}"

        protected val TRIGGER_ON_DELETE_ACCOUNT: String = "CREATE TRIGGER on_delete_account AFTER DELETE ON $DATABASE_ACCOUNT_TABLE BEGIN " +
                // if op is a transfert and was in deleted account, convert to an op in destination account
                "UPDATE ${OperationTable.DATABASE_OPERATIONS_TABLE} " +
                "SET ${OperationTable.KEY_OP_ACCOUNT_ID} = ${OperationTable.KEY_OP_TRANSFERT_ACC_ID}, " +
                "${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = 0, ${OperationTable.KEY_OP_THIRD_PARTY} = null, " +
                "${OperationTable.KEY_OP_SUM} = -${OperationTable.KEY_OP_SUM} WHERE " +
                "${OperationTable.KEY_OP_ACCOUNT_ID} = old.$KEY_ACCOUNT_ROWID AND ${OperationTable.KEY_OP_TRANSFERT_ACC_ID} != 0; " +
                // if deleted account is in a tranfert, operation is not a transfert anymore
                "UPDATE ${OperationTable.DATABASE_OPERATIONS_TABLE} " +
                "SET ${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = 0, ${OperationTable.KEY_OP_TRANSFERT_ACC_NAME} = null WHERE " +
                "${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = old.$KEY_ACCOUNT_ROWID; " +
                // if op is not a transfert and was in deleted account, delete it
                "DELETE FROM ${OperationTable.DATABASE_OPERATIONS_TABLE} WHERE ${OperationTable.KEY_OP_ACCOUNT_ID} = old.$KEY_ACCOUNT_ROWID AND ${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = 0; " +
                // delete sch op if deleted account was involved, trigger in
                // sch op table manage to decorralate existing op
                "DELETE FROM ${ScheduledOperationTable.DATABASE_SCHEDULED_TABLE} WHERE " +
                "${ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID} = old.$KEY_ACCOUNT_ROWID OR ${OperationTable.KEY_OP_TRANSFERT_ACC_ID} = old.$KEY_ACCOUNT_ROWID; END"


        private var mProjectionMode = -1
        private var mProjectionDate: Long = 0

        platformStatic fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_ACCOUNT_CREATE)
        }

        fun createValuesOf(account: Account): ContentValues {
            val values = ContentValues()
            values.put(KEY_ACCOUNT_NAME, account.name)
            values.put(KEY_ACCOUNT_DESC, account.description)
            values.put(KEY_ACCOUNT_START_SUM, account.startSum)
            values.put(KEY_ACCOUNT_CURRENCY, account.currency)
            values.put(KEY_ACCOUNT_PROJECTION_MODE, account.projMode)
            values.put(KEY_ACCOUNT_PROJECTION_DATE, account.projDate)
            values.put(KEY_ACCOUNT_CHECKED_OP_SUM, 0)
            values.put(KEY_ACCOUNT_OVERRIDE_HIDE_QUICK_ADD, account.overrideHideQuickAdd)
            values.put(KEY_ACCOUNT_HIDE_QUICK_ADD, account.hideQuickAdd)
            values.put(KEY_ACCOUNT_OVERRIDE_INSERT_DATE, account.overrideInsertDate)
            values.put(KEY_ACCOUNT_INSERT_DATE, account.insertDate)
            values.put(KEY_ACCOUNT_LAST_INSERTION_DATE, account.lastInsertDate)
            values.put(KEY_ACCOUNT_OVERRIDE_USE_WEIGHTED_INFO, account.overrideUseWeighedInfo)
            values.put(KEY_ACCOUNT_USE_WEIGHTED_INFO, account.useWeighedInfo)
            values.put(KEY_ACCOUNT_OVERRIDE_INVERT_QUICKADD_COMPLETION, account.overrideInvertQuickAddComp)
            values.put(KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION, account.invertQuickAddComp)
            values.put(KEY_ACCOUNT_OVERRIDE_NB_MONTH_AHEAD, account.overrideNbMonthsAhead)
            values.put(KEY_ACCOUNT_NB_MONTH_AHEAD, account.nbMonthsAhead)
            return values
        }

        throws(javaClass<ParseException>())
        public fun createAccount(ctx: Context, account: Account): Long {
            val values = createValuesOf(account)
            setCurrentSumAndDate(ctx, account, values)
            val res = ctx.getContentResolver().insert(DbContentProvider.ACCOUNT_URI, values)
            return lang.Long.parseLong(res.getLastPathSegment())
        }

        public fun deleteAccount(ctx: Context, accountId: Long): Boolean {
            return ctx.getContentResolver().delete(Uri.parse("${DbContentProvider.ACCOUNT_URI}/$accountId"), null, null) > 0
        }

        platformStatic public fun consolidateSums(ctx: Context, accountId: Long): Int {
            var res = 0
            if (0L != accountId) {
                val values = ContentValues()
                val accountCursor = fetchAccount(ctx, accountId)
                if (accountCursor.moveToFirst()) {
                    try {
                        val account = Account(accountCursor)
                        setCurrentSumAndDate(ctx, account, values)

                        val allOps = OperationTable.fetchAllCheckedOps(ctx, accountId)
                        var sum: Long = 0
                        if (allOps != null) {
                            if (allOps.moveToFirst()) {
                                val sumIdx = allOps.getColumnIndex(OperationTable.KEY_OP_SUM)
                                val tranAccIdx = allOps.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID)
                                do {
                                    var s = allOps.getLong(sumIdx)
                                    if (allOps.getLong(tranAccIdx) == accountId) {
                                        s = -s
                                    }
                                    sum += s
                                } while (allOps.moveToNext())
                            }
                            allOps.close()
                        }
                        Log.d(TAG, "consolidate checked sum : " + sum)
                        values.put(KEY_ACCOUNT_CHECKED_OP_SUM, sum)
                        res = ctx.getContentResolver().update(Uri.parse("${DbContentProvider.ACCOUNT_URI}/$accountId"),
                                values, null, null)
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }

                }
                accountCursor.close()
            }
            return res
        }

        throws(javaClass<ParseException>())
        private fun setCurrentSumAndDate(ctx: Context, account: Account, values: ContentValues) {
            var date: Long = 0
            var opSum: Long = 0
            Log.d(TAG, "setCurrentSumAndDate mAccountId = ${account.id}/ projectionMode :�${account.projMode}")
            when (account.projMode) {
                PROJECTION_FURTHEST -> {
                    if (account.id > 0) {
                        val allOps = OperationTable.fetchAllOps(ctx, account.id)
                        if (null != allOps) {
                            Log.d(TAG, "setCurrentSumAndDate allOps not null : ${allOps.getCount()}")
                            if (allOps.moveToFirst()) {
                                date = allOps.getLong(allOps.getColumnIndex(OperationTable.KEY_OP_DATE))
                                opSum = OperationTable.computeSumFromCursor(allOps, account.id)
                                Log.d(TAG, "setCurrentSumAndDate allOps moved to first opSum = $opSum")
                            }
                            allOps.close()
                        }
                    }
                }
                PROJECTION_DAY_OF_NEXT_MONTH -> {
                    val projDate = Tools.createClearedCalendar()
                    if (projDate.get(Calendar.DAY_OF_MONTH) >= account.projDate?.toInt() ?: 0) {
                        projDate.add(Calendar.MONTH, 1)
                    }
                    projDate.set(Calendar.DAY_OF_MONTH, account.projDate?.toInt() ?: 1)
                    projDate.add(Calendar.DAY_OF_MONTH, 1) // for query
                    val op = OperationTable.fetchOpEarlierThan(ctx, projDate.getTimeInMillis(), 0, account.id)
                    projDate.add(Calendar.DAY_OF_MONTH, -1) // restore date after query
                    if (null != op) {
                        if (op.moveToFirst()) {
                            opSum = OperationTable.computeSumFromCursor(op, account.id)
                        }
                        op.close()
                    }
                    date = projDate.getTimeInMillis()
                }
                PROJECTION_ABSOLUTE_DATE -> {
                    val projDate = Tools.createClearedCalendar()
                    projDate.setTime(account.projDate?.parseDate() as Date)
                    projDate.add(Calendar.DAY_OF_MONTH, 1) // roll for query
                    val op = OperationTable.fetchOpEarlierThan(ctx, projDate.getTimeInMillis(), 0, account.id)
                    projDate.add(Calendar.DAY_OF_MONTH, -1) // restore date after
                    if (null != op) {
                        if (op.moveToFirst()) {
                            opSum = OperationTable.computeSumFromCursor(op, account.id)
                        }
                        op.close()
                    }
                    date = projDate.getTimeInMillis()
                }
                else -> {
                }
            }
            Log.d(TAG, "setCurrentSumAndDate opSum = $opSum/ startSum = $account.startSum/ sum :�${account.startSum + opSum}")
            values.put(KEY_ACCOUNT_OP_SUM, opSum)
            values.put(KEY_ACCOUNT_CUR_SUM, account.startSum + opSum)
            Log.d(TAG, "setCurrentSumAndDate, KEY_ACCOUNT_CUR_SUM_DATE : ${Date(date).formatDate()}")
            values.put(KEY_ACCOUNT_CUR_SUM_DATE, date)
        }

        platformStatic public fun fetchAccount(ctx: Context, accountId: Long): Cursor {
            return ctx.getContentResolver().query(Uri.parse("${DbContentProvider.ACCOUNT_URI}/$accountId"), ACCOUNT_FULL_COLS, null, null, null)
        }

        public fun getAccountLoader(ctx: Context, accountId: Long): CursorLoader {
            return CursorLoader(ctx, Uri.parse("${DbContentProvider.ACCOUNT_URI}/$accountId"), ACCOUNT_FULL_COLS, null, null, null)
        }

        platformStatic public fun fetchAllAccounts(ctx: Context): Cursor {
            return ctx.getContentResolver().query(DbContentProvider.ACCOUNT_URI, ACCOUNT_FULL_COLS, null, null, null)
        }

        platformStatic public fun getAllAccountsLoader(ctx: Context): CursorLoader {
            return CursorLoader(ctx, DbContentProvider.ACCOUNT_URI, ACCOUNT_FULL_COLS, null, null, null)
        }

        //        fun checkNeedUpdateProjection(ctx: Context, op: Operation, accountId: Long): Boolean {
        //            val c = fetchAccount(ctx, accountId)
        //            if (c.moveToFirst()) {
        //                initProjectionDate(c)
        //            }
        //            c.close()
        //            val opDate = op.getDate()
        //            val projDate = mProjectionDate
        //            val res = (opDate <= projDate) || ((mProjectionMode == 0) && (opDate >= projDate)) || (projDate == 0L)
        //            Log.d(TAG, "checkNeedUpdateProjection : $res/mProjectionDate : ${Date(mProjectionDate).formatDate()}/opdate = ${Date(opDate).formatDate()}/projMode = $mProjectionMode")
        //            return res
        //        }

        fun updateAccountLastInsertDate(ctx: Context, accountId: Long, date: Long) {
            val values = ContentValues()
            values.put(KEY_ACCOUNT_LAST_INSERTION_DATE, date)
            updateAccount(ctx, accountId, values)
        }

        platformStatic public fun initProjectionDate(c: Cursor?) {
            if (c != null) {
                mProjectionMode = c.getInt(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE))
                when (mProjectionMode) {
                    PROJECTION_FURTHEST -> mProjectionDate = c.getLong(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE))
                    PROJECTION_DAY_OF_NEXT_MONTH -> {
                        val projDate = Tools.createClearedCalendar()
                        projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(c.getString(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE))))
                        val today = Tools.createClearedCalendar()
                        if (projDate.compareTo(today) <= 0) {
                            projDate.add(Calendar.MONTH, 1)
                        }
                        mProjectionDate = projDate.getTimeInMillis()
                    }
                    PROJECTION_ABSOLUTE_DATE -> try {
                        val projDate = c.getString(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)).parseDate()
                        val cal = GregorianCalendar()
                        cal.setTime(projDate)
                        cal.set(Calendar.HOUR, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        mProjectionDate = cal.getTimeInMillis()
                    } catch (e: ParseException) {
                        //ACRA.getErrorReporter().handleSilentException(e);
                        e.printStackTrace()
                    }

                    else -> {
                    }
                }
            }
        }

        //        throws(javaClass<ParseException>())
        //        platformStatic public fun updateAccountProjectionDate(ctx: Context, accountId: Long, projectionController: ProjectionDateController): Boolean {
        //            if (projectionController.hasChanged()) {
        //                updateAccountProjectionDate(ctx, accountId, projectionController.getMode(), projectionController.getDate(), true)
        //
        //            }
        //            return true
        //        }

        //        throws(javaClass<ParseException>())
        //        private fun updateAccountProjectionDate(ctx: Context, accountId: Long, projMode: Int, projDate: String,
        //                                                updateSumAndDate: Boolean): Boolean {
        //            val account = fetchAccount(ctx, accountId)
        //            if (account.moveToFirst()) {
        //                val args = ContentValues()
        //                val start_sum = account.getLong(account.getColumnIndex(KEY_ACCOUNT_START_SUM))
        //                account.close()
        //                Log.d(TAG, "updateAccountProjectionDate, KEY_ACCOUNT_PROJECTION_DATE : $projDate")
        //                args.put(KEY_ACCOUNT_PROJECTION_MODE, projMode)
        //                args.put(KEY_ACCOUNT_PROJECTION_DATE, projDate)
        //                if (updateSumAndDate) {
        //                    setCurrentSumAndDate(ctx, accountId, args, start_sum, projMode, projDate)
        //                }
        //                return updateAccount(ctx, accountId, args) > 0
        //            } else {
        //                return false
        //            }
        //        }

        //        throws(javaClass<ParseException>())
        //        platformStatic public fun updateAccountProjectionDate(ctx: Context, accountId: Long): Boolean {
        //            val c = fetchAccount(ctx, accountId)
        //            try {
        //                var res = true
        //                if (null != c) {
        //                    if (c.moveToFirst()) {
        //                        res = updateAccountProjectionDate(ctx, accountId, c.getInt(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)), c.getString(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)), true)
        //                    }
        //                    c.close()
        //                }
        //                return res
        //            } catch (e: ParseException) {
        //                c.close()
        //                throw e
        //            }
        //
        //        }

        //        platformStatic public fun updateAccountCurrency(ctx: Context, accountId: Long, currency: String): Boolean {
        //            val args = ContentValues()
        //            args.put(KEY_ACCOUNT_CURRENCY, currency)
        //            return updateAccount(ctx, accountId, args) > 0
        //        }

        public fun updateAccount(ctx: Context, accountId: Long, values: ContentValues): Int {
            return ctx.getContentResolver().update(Uri.parse("${DbContentProvider.ACCOUNT_URI}/$accountId"), values,
                    null, null)
        }

        throws(javaClass<ParseException>())
        public fun updateAccount(ctx: Context, account: Account): Boolean {
            val args = createValuesOf(account)
            setCurrentSumAndDate(ctx, account, args)
            return updateAccount(ctx, account.id, args) > 0
        }


        platformStatic public fun updateProjection(ctx: Context, accountId: Long, opSum: Long, oldOpSum: Long, opDate: Long,
                                                   origOpDate: Long) {
            val args = ContentValues()
            processProjectionFurthestCase(ctx, accountId, opDate, args)

            val accountCursor = fetchAccount(ctx, accountId)
            if (accountCursor.moveToFirst()) {
                val accOpSum = accountCursor.getLong(accountCursor.getColumnIndex(KEY_ACCOUNT_OP_SUM))

                args.put(KEY_ACCOUNT_OP_SUM, accOpSum + (-oldOpSum + opSum))

                var projDate: Long = 0
                val tmp = args.getAsLong(KEY_ACCOUNT_CUR_SUM_DATE)
                if (tmp != null) {
                    projDate = tmp
                }
                if (projDate == 0L) {
                    projDate = accountCursor.getLong(accountCursor.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE))
                }

                Log.d(TAG, "updateProjection projDate ${projDate.formatDate()}/opDate ${opDate.formatDate()}")

                if (origOpDate == -2L) {
                    // called from RadisService, oldSum = 0 and we always need to add opSum.
                    val curSum = accountCursor.getLong(accountCursor.getColumnIndex(KEY_ACCOUNT_CUR_SUM))
                    args.put(KEY_ACCOUNT_CUR_SUM, curSum + opSum)
                } else if (projDate == 0L || opDate == 0L || opDate <= projDate) {
                    val curSum = accountCursor.getLong(accountCursor.getColumnIndex(KEY_ACCOUNT_CUR_SUM))
                    args.put(KEY_ACCOUNT_CUR_SUM, curSum + (-oldOpSum + opSum))
                } else if (opDate > projDate && (origOpDate == -1L || origOpDate <= projDate)) {
                    val curSum = accountCursor.getLong(accountCursor.getColumnIndex(KEY_ACCOUNT_CUR_SUM))
                    args.put(KEY_ACCOUNT_CUR_SUM, curSum - oldOpSum)
                }

                if (updateAccount(ctx, accountId, args) > 0) {
                    if (mProjectionMode == PROJECTION_FURTHEST) {
                        mProjectionDate = opDate
                    }
                }
            }
            accountCursor.close()
        }

        private fun processProjectionFurthestCase(ctx: Context, accountId: Long, opDate: Long, args: ContentValues) {
            assert((mProjectionMode != -1))
            Log.d(TAG, "updateProjection, mProjectionMode $mProjectionMode / opDate $opDate")
            if (mProjectionMode == PROJECTION_FURTHEST && (opDate > mProjectionDate || opDate == 0L)) {
                if (opDate == 0L) {
                    val op = OperationTable.fetchLastOp(ctx, accountId)
                    if (null != op) {
                        if (op.moveToFirst()) {
                            Log.d(TAG, "updateProjection, KEY_ACCOUNT_CUR_SUM_DATE 0 : " +
                                    "${op.getLong(op.getColumnIndex(OperationTable.KEY_OP_DATE)).formatDate()}")
                            args.put(KEY_ACCOUNT_CUR_SUM_DATE, op.getLong(op.getColumnIndex(OperationTable.KEY_OP_DATE)))
                        }
                        op.close()
                    }
                } else {
                    Log.d(TAG, "updateProjection, KEY_ACCOUNT_CUR_SUM_DATE 1 : ${opDate.formatDate()}")
                    args.put(KEY_ACCOUNT_CUR_SUM_DATE, opDate)
                }
            }
        }

        platformStatic public fun getProjectionDate(): Long {
            return mProjectionDate
        }

        platformStatic public fun updateCheckedOpSum(ctx: Context, sum: Long, accountId: Long, transAccountId: Long, b: Boolean) {
            var acc: Cursor? = fetchAccount(ctx, accountId)
            if (acc != null) {
                if (acc!!.moveToFirst()) {
                    val values = ContentValues()
                    values.put(KEY_ACCOUNT_CHECKED_OP_SUM, acc!!.getLong(acc!!.getColumnIndex(KEY_ACCOUNT_CHECKED_OP_SUM)) + (if (b) sum else -sum))
                    updateAccount(ctx, accountId, values)

                    if (transAccountId > 0) {
                        values.clear()
                        acc!!.close()
                        acc = fetchAccount(ctx, transAccountId)
                        if (acc != null) {
                            if (acc!!.moveToFirst()) {
                                values.put(KEY_ACCOUNT_CHECKED_OP_SUM, acc!!.getLong(acc!!.getColumnIndex(KEY_ACCOUNT_CHECKED_OP_SUM)) + (if (b) -sum else sum))
                                updateAccount(ctx, transAccountId, values)
                            }
                            acc!!.close()
                        }
                    } else {
                        acc!!.close()
                    }
                } else {
                    acc!!.close()
                }
            }
        }

        //        public fun updateCheckedOpSum(ctx: Context, op: Cursor, b: Boolean) {
        //            val sum = op.getLong(op.getColumnIndex(OperationTable.KEY_OP_SUM))
        //            val accountId = op.getLong(op.getColumnIndex(OperationTable.KEY_OP_ACCOUNT_ID))
        //            val transAccountId = op.getLong(op.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID))
        //            updateCheckedOpSum(ctx, sum, accountId, transAccountId, b)
        //        }

        platformStatic public fun updateCheckedOpSum(ctx: Context, op: Operation, b: Boolean) {
            val sum = op.mSum
            val accountId = op.mAccountId
            val transAccountId = op.mTransferAccountId
            updateCheckedOpSum(ctx, sum, accountId, transAccountId, b)
        }

        platformStatic public fun getCheckedSum(ctx: Context, accountId: Long?): Long {
            val c = fetchAccount(ctx, accountId!!)
            var res: Long = 0
            if (c != null) {
                if (c.moveToFirst()) {
                    res = c.getLong(c.getColumnIndex(KEY_ACCOUNT_CHECKED_OP_SUM))
                }
                c.close()
            }
            return res
        }


        // UPGRADE�FUNCTIONS
        throws(javaClass<ParseException>())
        private fun rawSetCurrentSumAndDate(db: SQLiteDatabase, accountId: Long, values: ContentValues, start_sum: Long, projectionMode: Int, projectionDate: String) {
            var date: Long = 0
            var opSum: Long = 0
            Log.d(TAG, "raw setCurrentSumAndDate mAccountId = " + accountId + "/ mode : " + projectionMode)
            when (projectionMode) {
                0 -> {
                    if (accountId > 0) {
                        val allOps = db.query(OperationTable.DATABASE_OP_TABLE_JOINTURE, OperationTable.OP_COLS_QUERY, OperationTable.RESTRICT_TO_ACCOUNT, array(lang.Long.toString(accountId), lang.Long.toString(accountId)), null, null, OperationTable.OP_ORDERING, null)
                        if (null != allOps) {
                            Log.d(TAG, "raw setCurrentSumAndDate allOps not null : " + allOps.getCount())
                            if (allOps.moveToFirst()) {
                                Log.d(TAG, "raw setCurrentSumAndDate allOps moved to first")
                                date = allOps.getLong(allOps.getColumnIndex(OperationTable.KEY_OP_DATE))
                                opSum = OperationTable.computeSumFromCursor(allOps, accountId)
                            }
                            allOps.close()
                        }
                    }
                }
                1 -> {
                    val projDate = Tools.createClearedCalendar()
                    if (projDate.get(Calendar.DAY_OF_MONTH) >= Integer.parseInt(projectionDate)) {
                        projDate.add(Calendar.MONTH, 1)
                    }
                    projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(projectionDate))
                    projDate.add(Calendar.DAY_OF_MONTH, 1) // roll for query
                    val op = db.query(OperationTable.DATABASE_OP_TABLE_JOINTURE, OperationTable.OP_COLS_QUERY, OperationTable.RESTRICT_TO_ACCOUNT + " and ops." + OperationTable.KEY_OP_DATE + " < ?", array(lang.Long.toString(accountId), lang.Long.toString(accountId), lang.Long.toString(projDate.getTimeInMillis())), null, null, OperationTable.OP_ORDERING)
                    projDate.add(Calendar.DAY_OF_MONTH, -1) // restore date after
                    // query
                    if (null != op) {
                        if (op.moveToFirst()) {
                            opSum = OperationTable.computeSumFromCursor(op, accountId)
                        }
                        op.close()
                    }
                    date = projDate.getTimeInMillis()
                }
                2 -> {
                    val projDate = Tools.createClearedCalendar()
                    projDate.setTime(projectionDate.parseDate())
                    projDate.add(Calendar.DAY_OF_MONTH, 1) // roll for query
                    val op = db.query(OperationTable.DATABASE_OP_TABLE_JOINTURE, OperationTable.OP_COLS_QUERY, OperationTable.RESTRICT_TO_ACCOUNT + " and ops." + OperationTable.KEY_OP_DATE + " < ?", array(lang.Long.toString(accountId), lang.Long.toString(accountId), lang.Long.toString(projDate.getTimeInMillis())), null, null, OperationTable.OP_ORDERING)
                    projDate.add(Calendar.DAY_OF_MONTH, -1) // restore date after
                    // query
                    if (null != op) {
                        if (op.moveToFirst()) {
                            opSum = OperationTable.computeSumFromCursor(op, accountId)
                        }
                        op.close()
                    }
                    date = projDate.getTimeInMillis()
                }
                else -> {
                }
            }
            values.put(KEY_ACCOUNT_OP_SUM, opSum)
            values.put(KEY_ACCOUNT_CUR_SUM, start_sum + opSum)
            Log.d(TAG, "rawSetCurrentSumAndDate, KEY_ACCOUNT_CUR_SUM_DATE : " + date.formatDate())
            values.put(KEY_ACCOUNT_CUR_SUM_DATE, date)
        }

        private fun rawConsolidateSums(db: SQLiteDatabase, accountId: Long) {
            val values = ContentValues()
            val account = db.query(DATABASE_ACCOUNT_TABLE, ACCOUNT_FULL_COLS, "_id=?",
                    array(lang.Long.toString(accountId)), null, null, null)
            if (account != null) {
                if (account.moveToFirst()) {
                    try {
                        rawSetCurrentSumAndDate(db, accountId, values,
                                account.getLong(account.getColumnIndex(KEY_ACCOUNT_START_SUM)),
                                account.getInt(account.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
                                account.getString(account.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)))
                        db.update(DATABASE_ACCOUNT_TABLE, values, "_id=?", array(lang.Long.toString(accountId)))
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }

                }
                account.close()
            }
        }

        platformStatic fun upgradeDefault(db: SQLiteDatabase) {
            val c = db.query(DATABASE_ACCOUNT_TABLE, array(KEY_ACCOUNT_ROWID), null, null, null, null, null)
            if (null != c) {
                if (c.moveToFirst()) {
                    do {
                        rawConsolidateSums(db, c.getLong(0))
                    } while (c.moveToNext())
                }
                c.close()
            }
        }

        platformStatic fun upgradeFromV18(db: SQLiteDatabase) {
            db.execSQL(ADD_OVERRIDE_HIDE_QUICK_ADD_COLUMN)
            db.execSQL(ADD_HIDE_QUICK_ADD_COLUMN)
            db.execSQL(ADD_OVERRIDE_INVERT_QUiCKADD_COMPLETION_COLUMN)
            db.execSQL(ADD_INVERT_QUiCKADD_COMPLETION_COLUMN)
            db.execSQL(ADD_OVERRIDE_USE_WEIGHTED_INFO_COLUMN)
            db.execSQL(ADD_USE_WEIGHTED_INFO_COLUMN)
            db.execSQL(ADD_OVERRIDE_INSERT_DATE_COLUMN)
            db.execSQL(ADD_INSERT_DATE_COLUMN)
            db.execSQL(ADD_LAST_INSERT_DATE_COLUMN)
            db.execSQL(ADD_OVERRIDE_NB_MONTH_AHEAD_COLUMN)
            db.execSQL(ADD_NB_MONTH_AHEAD_COLUMN)
            val values = ContentValues()
            val prefs = PreferenceTable.getAllPrefs(db)

            values.put(KEY_ACCOUNT_HIDE_QUICK_ADD, prefs.get(ConfigFragment.KEY_HIDE_OPS_QUICK_ADD)?.toInt() ?: 0)
            values.put(KEY_ACCOUNT_INSERT_DATE, prefs.get(ConfigFragment.KEY_INSERTION_DATE)?.toInt() ?: ConfigFragment.DEFAULT_INSERTION_DATE.toInt())
            values.put(KEY_ACCOUNT_LAST_INSERTION_DATE, prefs.get(ConfigFragment.KEY_LAST_INSERTION_DATE)?.toLong() ?: 0L)
            values.put(KEY_ACCOUNT_USE_WEIGHTED_INFO, prefs.get(ConfigFragment.KEY_USE_WEIGHTED_INFOS)?.toInt() ?: 0)
            values.put(KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION, prefs.get(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD)?.toInt() ?: 1)
            db.update(DATABASE_ACCOUNT_TABLE, values, null, null)
        }

        platformStatic fun upgradeFromV16(db: SQLiteDatabase) {
            db.execSQL(ADD_CHECKED_SUM_COLUNM)
        }

        platformStatic fun upgradeFromV9(db: SQLiteDatabase) {
            db.execSQL(ADD_PROJECTION_MODE_COLUNM)
            db.execSQL(ADD_PROJECTION_MODE_DATE)
            val c = db.query(DATABASE_ACCOUNT_TABLE, array<String>(), null, null, null, null, null)
            if (null != c) {
                if (c.moveToFirst()) {
                    do {
                        rawConsolidateSums(db, c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)))
                    } while (c.moveToNext())
                }
                c.close()
            }
        }

        platformStatic fun upgradeFromV6(db: SQLiteDatabase) {
            db.execSQL("DROP TRIGGER on_delete_third_party")
            db.execSQL("DROP TRIGGER on_delete_mode")
            db.execSQL("DROP TRIGGER on_delete_tag")
            db.execSQL("ALTER TABLE accounts RENAME TO accounts_old;")
            db.execSQL(DATABASE_ACCOUNT_CREATE_v7)
            val c = db.query("accounts_old", array(KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_DESC, KEY_ACCOUNT_OP_SUM, KEY_ACCOUNT_START_SUM), null, null, null, null, null)
            if (null != c && c.moveToFirst()) {
                do {
                    var initialValues = ContentValues()
                    initialValues.put(KEY_ACCOUNT_NAME, c.getString(c.getColumnIndex(KEY_ACCOUNT_NAME)))
                    initialValues.put(KEY_ACCOUNT_DESC, c.getString(c.getColumnIndex(KEY_ACCOUNT_DESC)))
                    var d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_START_SUM))
                    var l = Math.round(d * 100)
                    initialValues.put(KEY_ACCOUNT_START_SUM, l)
                    d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_OP_SUM))
                    l = Math.round(d * 100)
                    initialValues.put(KEY_ACCOUNT_OP_SUM, l)
                    d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM))
                    l = Math.round(d * 100)
                    initialValues.put(KEY_ACCOUNT_CUR_SUM, l)
                    initialValues.put(KEY_ACCOUNT_CURRENCY, c.getString(c.getColumnIndex(KEY_ACCOUNT_CURRENCY)))
                    initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE, c.getLong(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE)))
                    val id = db.insert(DATABASE_ACCOUNT_TABLE, null, initialValues)
                    initialValues = ContentValues()
                    initialValues.put(OperationTable.KEY_OP_ACCOUNT_ID, id)
                    db.update(OperationTable.DATABASE_OPERATIONS_TABLE, initialValues, "${OperationTable.KEY_OP_ACCOUNT_ID}=${c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID))}", null)

                } while (c.moveToNext())
                c.close()
                db.execSQL("DROP TABLE accounts_old;")
            }
        }

        platformStatic fun upgradeFromV12(db: SQLiteDatabase) {
            db.execSQL(TRIGGER_ON_DELETE_ACCOUNT)
        }

        platformStatic fun upgradeFromV4(db: SQLiteDatabase) {
            db.execSQL(ADD_CUR_DATE_COLUNM)
        }

    }
}