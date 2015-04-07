package fr.geobert.radis.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.formatDate
import fr.geobert.radis.ui.ConfigFragment
import java.util.LinkedHashMap
import kotlin.platform.platformStatic

public class RadisService : android.app.IntentService(RadisService.TAG) {

    private fun consolidateDbAfterRestore() {
        val prefs = fr.geobert.radis.tools.DBPrefsManager.getInstance(this)
        val needConsolidate = prefs.getBoolean(CONSOLIDATE_DB, false)
        android.util.Log.d(TAG, "needConsolidate : " + needConsolidate)
        if (needConsolidate!!) {
            val cursor = fr.geobert.radis.db.AccountTable.Companion.fetchAllAccounts(this)
            prefs.put(CONSOLIDATE_DB, false)
            //            prefs.commit();
            if (cursor.moveToFirst()) {
                do {
                    android.util.Log.d(TAG, "CONSOLIDATE ON consolidateDB set to true : " + cursor.getLong(0))
                    fr.geobert.radis.db.AccountTable.Companion.consolidateSums(this, cursor.getLong(0))
                } while (cursor.moveToNext())
            }
            cursor.close()

            fr.geobert.radis.MainActivity.Companion.refreshAccountList(this)
        }
    }

    override fun onHandleIntent(intent: android.content.Intent) {
        try {
            val prefs = fr.geobert.radis.tools.DBPrefsManager.getInstance(this)
            prefs.fillCache(this)
            processScheduledOps()
        } finally {
            if (getLock(this).isHeld()) {
                android.util.Log.d(TAG, "release lock")
                getLock(this).release()
            }
            // If the DB was restored, we consolidate the sums
            consolidateDbAfterRestore()
        }
        stopSelf()
    }

    private fun keepGreatestDate(greatestDatePerAccount: java.util.HashMap<Long, Long>, accountId: Long, curOpDate: Long) {
        val date: Long = greatestDatePerAccount.get(accountId) ?: 0L
        if (curOpDate > date) {
            greatestDatePerAccount.put(accountId, curOpDate)
        }
    }


    synchronized private fun processScheduledOps() {
        val schOpsCursor = ScheduledOperationTable.fetchAllScheduledOps(this)
        if (schOpsCursor.isFirst()) {
            val sumsPerAccount = LinkedHashMap<Long, Long>()
            val greatestDatePerAccount = LinkedHashMap<Long, Long>()

            // TODO convert to more a Kotlin way with a map on the cursor
            do {
                val OP_ROW_ID = schOpsCursor.getLong(schOpsCursor.getColumnIndex("_id"))
                val op = ScheduledOperation(schOpsCursor)
                val accountId = op.mAccountId
                val transId = op.mTransferAccountId
                var sum: Long = 0
                var needUpdate = false

                val accountCursor = AccountTable.fetchAccount(this, accountId)
                if (!accountCursor.moveToFirst()) {
                    schOpsCursor.moveToNext()
                    continue
                } else {
                    AccountTable.Companion.initProjectionDate(accountCursor)
                }
                val account = Account(accountCursor)
                accountCursor.close()

                val timeParams = TimeParams.computeTimeParams(this, account)

                Log.d("RadisService", "after compute timeparams timeParams.today: ${timeParams.today.formatDate()} / timeParams.insertionDate: ${timeParams.insertionDate.formatDate()}")

                // insert all scheduled of the past until current month
                while (op.getDate() <= timeParams.currentMonth && !op.isObsolete()) {
                    Log.d("RadisService", "insert past sch op until current month, op: ${op.getDate().formatDate()} / limit: ${timeParams.currentMonth.formatDate()}")
                    val opSum = insertSchOp(op, OP_ROW_ID)
                    if (opSum != 0L) {
                        sum = sum + opSum
                        needUpdate = true
                    }
                }

                Log.d("RadisService", "processScheduledOps timeParams.today: ${timeParams.today.formatDate()} / timeParams.insertionDate: ${timeParams.insertionDate.formatDate()}")
                if (timeParams.today >= timeParams.insertionDate) {
                    // it's time to insert scheduled ops
                    while (op.getDate() < timeParams.limitInsertionDate && !op.isObsolete()) {
                        keepGreatestDate(greatestDatePerAccount, accountId, op.getDate())
                        if (transId > 0) {
                            keepGreatestDate(greatestDatePerAccount, transId, op.getDate())
                        }
                        val opSum = insertSchOp(op, OP_ROW_ID)
                        if (opSum != 0L) {
                            sum = sum + opSum
                            needUpdate = true
                        }
                    }
                }
                ScheduledOperationTable.updateScheduledOp(this, OP_ROW_ID, op, false)
                if (needUpdate) {
                    val curSum: Long = sumsPerAccount.get(accountId) ?: 0L
                    sumsPerAccount.put(accountId, curSum + sum)
                    keepGreatestDate(greatestDatePerAccount, accountId, op.getDate())
                    // the sch op is a transfert, update the dst account sum with -sum
                    if (transId > 0) {
                        val curSum2 = sumsPerAccount.get(transId) ?: 0L
                        sumsPerAccount.put(transId, curSum2 - sum)
                        keepGreatestDate(greatestDatePerAccount, transId, op.getDate())
                    }
                }
                if (account.overrideInsertDate) {
                    updateAccountLastInsertDate(account.id, timeParams.today)
                }
            } while (schOpsCursor.moveToNext())

            sumsPerAccount.entrySet().forEach {
                updateAccountSum(it.getValue(), 0, it.getKey(), greatestDatePerAccount.get(it.getKey()))
            }

            if (sumsPerAccount.entrySet().count() > 0) {
                sendOrderedBroadcast(Intent(Tools.INTENT_REFRESH_NEEDED), null)
            }

            DBPrefsManager.getInstance(this).put(ConfigFragment.KEY_LAST_INSERTION_DATE,
                    Tools.createClearedCalendar().getTimeInMillis())
        }
        schOpsCursor.close()
    }


    private fun insertSchOp(op: fr.geobert.radis.data.ScheduledOperation, opRowId: Long): Long {
        val accountId = op.mAccountId
        op.mScheduledId = opRowId
        val needUpdate = fr.geobert.radis.db.OperationTable.createOp(this, op, accountId, false) > -1
        fr.geobert.radis.data.ScheduledOperation.Companion.addPeriodicityToDate(op)
        return if (needUpdate) op.mSum else 0
    }

    private fun updateAccountLastInsertDate(accountId: Long, date: Long) {
        AccountTable.updateAccountLastInsertDate(this, accountId, date)
    }

    public fun updateAccountSum(opSum: Long, oldSum: Long, accountId: Long, opDate: Long) {
        AccountTable.updateProjection(this, accountId, opSum, oldSum, opDate, (-2).toLong())
    }

    companion object {
        private val TAG = "RadisService"
        public val LOCK_NAME_STATIC: String = "fr.geobert.radis.StaticLock"
        public val CONSOLIDATE_DB: String = "consolidateDB"
        private var lockStatic: android.os.PowerManager.WakeLock? = null

        public fun callMe(context: android.content.Context) {
            RadisService.acquireStaticLock(context)
            context.startService(android.content.Intent(context, javaClass<RadisService>()))
        }

        platformStatic public fun acquireStaticLock(context: android.content.Context) {
            android.util.Log.d(TAG, "acquireStaticLock")
            getLock(context).acquire()
        }

        synchronized private fun getLock(context: Context): PowerManager.WakeLock {
            val lock = if (lockStatic == null) {
                val mgr = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val l = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC)
                l.setReferenceCounted(true)
                lockStatic = l
                l
            } else {
                lockStatic!!
            }
            return lock
        }
    }

}
