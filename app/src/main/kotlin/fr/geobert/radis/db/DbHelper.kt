package fr.geobert.radis.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import fr.geobert.radis.service.RadisService
import fr.geobert.radis.tools.DBPrefsManager

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

public class DbHelper private constructor(private val mCtx: Context) : SQLiteOpenHelper(mCtx, DbHelper.DATABASE_NAME, null, DbHelper.DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        AccountTable.onCreate(db)
        OperationTable.onCreate(db)
        InfoTables.onCreate(db)
        OperationTable.createMeta(db)
        PreferenceTable.onCreate(db)
        ScheduledOperationTable.onCreate(db)
        StatisticTable.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DbHelper", "onUpgrade " + oldVersion + " -> " + newVersion)
        if (oldVersion <= 1) upgradeFromV1(db, oldVersion, newVersion)
        if (oldVersion <= 2) upgradeFromV2(db, oldVersion, newVersion)
        if (oldVersion <= 3) upgradeFromV3(db, oldVersion, newVersion)
        if (oldVersion <= 4) upgradeFromV4(db)
        if (oldVersion <= 5) upgradeFromV5(db, oldVersion, newVersion)
        if (oldVersion <= 6) upgradeFromV6(db, oldVersion, newVersion)
        if (oldVersion <= 7) upgradeFromV7(db, oldVersion, newVersion)
        if (oldVersion <= 8) upgradeFromV8(db, oldVersion, newVersion)
        if (oldVersion <= 9) upgradeFromV9(db)
        if (oldVersion <= 10) upgradeFromV10(db)
        if (oldVersion <= 11) upgradeFromV11(db, oldVersion, newVersion)
        if (oldVersion <= 12) upgradeFromV12(db, oldVersion, newVersion)
        if (oldVersion <= 13) upgradeFromV13(db, oldVersion, newVersion)
        if (oldVersion <= 14) upgradeFromV14(db, oldVersion, newVersion)
        if (oldVersion <= 15) upgradeFromV15(db, oldVersion, newVersion)
        if (oldVersion <= 16) upgradeFromV16(db, oldVersion, newVersion)
        if (oldVersion <= 17) upgradeFromV17(db)
        if (oldVersion <= 18) upgradeFromV18(db)
        if (oldVersion <= 19) upgradeFromV19(db)
        upgradeDefault(db)
    }

    private fun upgradeDefault(db: SQLiteDatabase) {
        AccountTable.upgradeDefault(db)
    }

    private fun upgradeFromV19(db: SQLiteDatabase) {
        StatisticTable.upgradeFromV19(db)
    }

    private fun upgradeFromV18(db: SQLiteDatabase) {
        AccountTable.upgradeFromV18(db)
        PreferenceTable.upgradeFromV18(db)
    }

    private fun upgradeFromV17(db: SQLiteDatabase) {
        StatisticTable.upgradeFromV17(db)
    }

    private fun upgradeFromV16(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        InfoTables.upgradeFromV16(db, oldVersion, newVersion)
        OperationTable.upgradeFromV16(db, oldVersion, newVersion)
        AccountTable.upgradeFromV16(db)
    }

    private fun upgradeFromV15(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        InfoTables.upgradeFromV15(db, oldVersion, newVersion)
    }

    private fun upgradeFromV14(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ScheduledOperationTable.upgradeFromV12(db, oldVersion, newVersion)
    }

    private fun upgradeFromV13(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        InfoTables.upgradeFromV13(db, oldVersion, newVersion)
    }

    private fun upgradeFromV12(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ScheduledOperationTable.upgradeFromV12(db, oldVersion, newVersion)
        AccountTable.upgradeFromV12(db)
    }

    private fun upgradeFromV11(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        OperationTable.upgradeFromV11(db, oldVersion, newVersion)
        ScheduledOperationTable.upgradeFromV11(db, oldVersion, newVersion)
    }

    private fun upgradeFromV10(db: SQLiteDatabase) {
        PreferenceTable.upgradeFromV10(mCtx, db)
    }

    private fun upgradeFromV9(db: SQLiteDatabase) {
        AccountTable.upgradeFromV9(db)
    }

    private fun upgradeFromV8(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        InfoTables.upgradeFromV8(db, oldVersion, newVersion)
    }

    private fun upgradeFromV7(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        InfoTables.upgradeFromV7(db, oldVersion, newVersion)
    }

    private fun upgradeFromV6(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        AccountTable.upgradeFromV6(db)
        ScheduledOperationTable.upgradeFromV6(db, oldVersion, newVersion)
        OperationTable.upgradeFromV6(db, oldVersion, newVersion)
    }

    private fun upgradeFromV5(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ScheduledOperationTable.upgradeFromV5(db, oldVersion, newVersion)
        OperationTable.upgradeFromV5(db, oldVersion, newVersion)
    }

    private fun upgradeFromV4(db: SQLiteDatabase) {
        AccountTable.upgradeFromV4(db)
    }

    private fun upgradeFromV3(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        OperationTable.upgradeFromV3(db, oldVersion, newVersion)
    }

    private fun upgradeFromV1(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        OperationTable.upgradeFromV1(db, oldVersion, newVersion)
    }

    private fun upgradeFromV2(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        OperationTable.upgradeFromV2(db, oldVersion, newVersion)
    }

    companion object {
        protected val DATABASE_NAME: String = "radisDb"
        protected val DATABASE_VERSION: Int = 20

        public var instance: DbHelper? = null

        synchronized public fun getInstance(ctx: Context): DbHelper {
            if (instance == null) {
                instance = DbHelper(ctx)
            }
            return instance!!
        }

        public fun trashDatabase(ctx: Context) {
            Log.d("Radis", "trashDatabase DbHelper")
            ctx.deleteDatabase(DATABASE_NAME)
        }

        public fun backupDatabase(): Boolean {
            try {
                val sd = Environment.getExternalStorageDirectory()
                val data = Environment.getDataDirectory()
                if (sd.canWrite()) {
                    val currentDBPath = "data/fr.geobert.radis/databases/radisDb"
                    val backupDBDir = "/radis/"
                    val backupDBPath = "/radis/radisDb"
                    val currentDB = File(data, currentDBPath)
                    val backupDir = File(sd, backupDBDir)
                    backupDir.mkdirs()
                    val backupDB = File(sd, backupDBPath)
                    if (currentDB.exists()) {
                        val srcFIS = FileInputStream(currentDB)
                        val dstFOS = FileOutputStream(backupDB)
                        val src = srcFIS.getChannel()
                        val dst = dstFOS.getChannel()
                        dst.transferFrom(src, 0, src.size())
                        src.close()
                        dst.close()
                        srcFIS.close()
                        dstFOS.close()
                    }
                    return true
                }
                return false
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }

        public fun restoreDatabase(ctx: Context): Boolean {
            try {
                val sd = Environment.getExternalStorageDirectory()

                val backupDBPath = "/radis/radisDb"
                val currentDB = ctx.getDatabasePath(DATABASE_NAME)
                val backupDB = File(sd, backupDBPath)

                if (backupDB.exists()) {
                    DbContentProvider.close()
                    val srcFIS = FileInputStream(backupDB)
                    val dstFOS = FileOutputStream(currentDB)
                    val dst = dstFOS.getChannel()
                    val src = srcFIS.getChannel()

                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                    srcFIS.close()
                    dstFOS.close()
                    DbContentProvider.reinit(ctx)
                    DBPrefsManager.getInstance(ctx).put(RadisService.CONSOLIDATE_DB, true)
                    //                PrefsManager.getInstance(ctx).commit();
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }

        public fun delete() {
            instance = null
        }
    }
}
