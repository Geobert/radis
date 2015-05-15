package fr.geobert.radis.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.tools.DBPrefsManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class DbHelper extends SQLiteOpenHelper {
    protected static final String DATABASE_NAME = "radisDb";
    protected static final int DATABASE_VERSION = 20;

    private Context mCtx;

    private DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mCtx = context;
    }

    public static DbHelper instance = null;

    public static synchronized DbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DbHelper(ctx);
        }
        return instance;
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {
        AccountTable.onCreate(db);
        OperationTable.onCreate(db);
        InfoTables.onCreate(db);
        OperationTable.createMeta(db);
        PreferenceTable.onCreate(db);
        ScheduledOperationTable.onCreate(db);
        StatisticTable.onCreate(db);
    }

    @Override
    public void onUpgrade(@NotNull SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DbHelper", "onUpgrade " + oldVersion + " -> " + newVersion);
        switch (oldVersion) {
            case 1:
                OperationTable.upgradeFromV1(db, oldVersion, newVersion );
            case 2:
                OperationTable.upgradeFromV2(db, oldVersion, newVersion);
            case 3:
                OperationTable.upgradeFromV3(db, oldVersion, newVersion);
            case 4:
                AccountTable.upgradeFromV4(db);
            case 5:
                ScheduledOperationTable.upgradeFromV5(db, oldVersion, newVersion);
                OperationTable.upgradeFromV5(db, oldVersion, newVersion);
            case 6:
                AccountTable.upgradeFromV6(db);
                ScheduledOperationTable.upgradeFromV6(db, oldVersion, newVersion);
                OperationTable.upgradeFromV6(db, oldVersion, newVersion);
            case 7:
                InfoTables.upgradeFromV7(db, oldVersion, newVersion);
            case 8:
                InfoTables.upgradeFromV8(db, oldVersion, newVersion);
            case 9:
                AccountTable.upgradeFromV9(db);
            case 10:
                PreferenceTable.upgradeFromV10(mCtx, db);
            case 11:
                OperationTable.upgradeFromV11(db, oldVersion, newVersion);
                ScheduledOperationTable.upgradeFromV11(db, oldVersion, newVersion);
            case 12:
                ScheduledOperationTable.upgradeFromV12(db, oldVersion, newVersion);
                AccountTable.upgradeFromV12(db);
            case 13:
                InfoTables.upgradeFromV13(db, oldVersion, newVersion);
            case 14:
                ScheduledOperationTable.upgradeFromV12(db, oldVersion, newVersion);
            case 15:
                InfoTables.upgradeFromV15(db, oldVersion, newVersion);
            case 16:
                InfoTables.upgradeFromV16(db, oldVersion, newVersion);
                OperationTable.upgradeFromV16(db, oldVersion, newVersion);
                AccountTable.upgradeFromV16(db);
            case 17:
                StatisticTable.upgradeFromV17(db);
            case 18:
                AccountTable.upgradeFromV18(db);
                PreferenceTable.upgradeFromV18(db);
            case 19:
                StatisticTable.upgradeFromV19(db);
            default:
                AccountTable.upgradeDefault(db);
        }
    }

    public static void trashDatabase(Context ctx) {
        Log.d("Radis", "trashDatabase DbHelper");
        ctx.deleteDatabase(DATABASE_NAME);
    }

    public static boolean backupDatabase() {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            if (sd.canWrite()) {
                String currentDBPath = "data/fr.geobert.radis/databases/radisDb";
                String backupDBDir = "/radis/";
                String backupDBPath = "/radis/radisDb";
                File currentDB = new File(data, currentDBPath);
                File backupDir = new File(sd, backupDBDir);
                backupDir.mkdirs();
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileInputStream srcFIS = new FileInputStream(currentDB);
                    FileOutputStream dstFOS = new FileOutputStream(backupDB);
                    FileChannel src = srcFIS.getChannel();
                    FileChannel dst = dstFOS.getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    srcFIS.close();
                    dstFOS.close();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean restoreDatabase(Context ctx) {
        try {
            File sd = Environment.getExternalStorageDirectory();

            String backupDBPath = "/radis/radisDb";
            File currentDB = ctx.getDatabasePath(DATABASE_NAME);
            File backupDB = new File(sd, backupDBPath);

            if (backupDB.exists()) {
                DbContentProvider.close();
                FileInputStream srcFIS = new FileInputStream(backupDB);
                FileOutputStream dstFOS = new FileOutputStream(currentDB);
                FileChannel dst = dstFOS.getChannel();
                FileChannel src = srcFIS.getChannel();

                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                srcFIS.close();
                dstFOS.close();
                DbContentProvider.reinit(ctx);
                DBPrefsManager.getInstance(ctx).put(RadisService.CONSOLIDATE_DB, true);
//                PrefsManager.getInstance(ctx).commit();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void delete() {
        instance = null;
    }
}
