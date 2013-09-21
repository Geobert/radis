package fr.geobert.radis.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import fr.geobert.radis.tools.AsciiUtils;

import java.util.HashMap;
import java.util.Vector;

// this class manage simple info tables which are _id / name schema
public class InfoTables {
    static final String DATABASE_THIRD_PARTIES_TABLE = "third_parties";
    static final String DATABASE_TAGS_TABLE = "tags";
    static final String DATABASE_MODES_TABLE = "modes";

    public static final String KEY_THIRD_PARTY_ROWID = "_id";
    public static final String KEY_THIRD_PARTY_NAME = "third_party_name";
    public static final String KEY_THIRD_PARTY_NORMALIZED_NAME = "third_party_norm_name";
    public static final String KEY_TAG_ROWID = "_id";
    public static final String KEY_TAG_NAME = "tag_name";
    public static final String KEY_TAG_NORMALIZED_NAME = "tag_norm_name";
    public static final String KEY_MODE_ROWID = "_id";
    public static final String KEY_MODE_NAME = "mode_name";
    public static final String KEY_MODE_NORMALIZED_NAME = "mode_norm_name";

    private static final String DATABASE_THIRD_PARTIES_CREATE = "create table "
            + DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
            + " integer primary key autoincrement, " + KEY_THIRD_PARTY_NAME
            + " text not null, " + KEY_THIRD_PARTY_NORMALIZED_NAME
            + " text not null);";

    private static final String DATABASE_TAGS_CREATE = "create table "
            + DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
            + " integer primary key autoincrement, " + KEY_TAG_NAME
            + " text not null, " + KEY_TAG_NORMALIZED_NAME + " text not null);";

    private static final String DATABASE_MODES_CREATE = "create table "
            + DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID
            + " integer primary key autoincrement, " + KEY_MODE_NAME
            + " text not null, " + KEY_MODE_NORMALIZED_NAME
            + " text not null);";

    protected static final String ADD_NORMALIZED_MODE = "ALTER TABLE "
            + DATABASE_MODES_TABLE + " ADD COLUMN " + KEY_MODE_NORMALIZED_NAME
            + " text not null DEFAULT ''";

    protected static final String ADD_NORMALIZED_TAG = "ALTER TABLE "
            + DATABASE_TAGS_TABLE + " ADD COLUMN " + KEY_TAG_NORMALIZED_NAME
            + " text not null DEFAULT ''";

    protected static final String ADD_NORMALIZED_THIRD_PARTY = "ALTER TABLE "
            + DATABASE_THIRD_PARTIES_TABLE + " ADD COLUMN "
            + KEY_THIRD_PARTY_NORMALIZED_NAME + " text not null DEFAULT ''";

    @SuppressWarnings("serial")
    private static final HashMap<String, String> mColNameNormName = new HashMap<String, String>() {
        {
            put(InfoTables.KEY_THIRD_PARTY_NAME,
                    InfoTables.KEY_THIRD_PARTY_NORMALIZED_NAME);
            put(InfoTables.KEY_TAG_NAME, InfoTables.KEY_TAG_NORMALIZED_NAME);
            put(InfoTables.KEY_MODE_NAME, InfoTables.KEY_MODE_NORMALIZED_NAME);
        }
    };

    @SuppressWarnings("serial")
    private static final HashMap<String, String> mInfoColMap = new HashMap<String, String>() {
        {
            put(DbContentProvider.THIRD_PARTY_URI.toString(),
                    KEY_THIRD_PARTY_NAME);
            put(DbContentProvider.TAGS_URI.toString(), KEY_TAG_NAME);
            put(DbContentProvider.MODES_URI.toString(), KEY_MODE_NAME);
        }
    };

//    private static final int GET_TP = 700;
//    private static final int GET_MODES = 710;
//    private static final int GET_TAGS = 720;

//    private static class InfoCacheFiller implements LoaderCallbacks<Cursor> {
//        private Context mCtx;
//
//        public InfoCacheFiller(Context ctx) {
//            mCtx = ctx;
//        }
//
//        @Override
//        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//            Uri u = null;
//            String[] cols = null;
//            switch (id) {
//                case GET_TP:
//                    cols = new String[]{KEY_THIRD_PARTY_ROWID,
//                            KEY_THIRD_PARTY_NORMALIZED_NAME};
//                    u = DbContentProvider.THIRD_PARTY_URI;
//                    break;
//                case GET_MODES:
//                    cols = new String[]{KEY_MODE_ROWID, KEY_MODE_NORMALIZED_NAME};
//                    u = DbContentProvider.MODES_URI;
//                    break;
//                case GET_TAGS:
//                    cols = new String[]{KEY_TAG_ROWID, KEY_TAG_NORMALIZED_NAME};
//                    u = DbContentProvider.TAGS_URI;
//                    break;
//                default:
//                    break;
//            }
//            return new CursorLoader(mCtx, u, cols, null, null, null);
//        }
//
//        @Override
//        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//            switch (loader.getId()) {
//                case GET_TP:
//                    fillCache(data, mThirdPartiesMap);
//                    break;
//                case GET_MODES:
//                    fillCache(data, mModesMap);
//                    break;
//                case GET_TAGS:
//                    fillCache(data, mTagsMap);
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        @Override
//        public void onLoaderReset(Loader<Cursor> arg0) {
//
//        }
//
//    }

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_THIRD_PARTIES_CREATE);
        db.execSQL(DATABASE_TAGS_CREATE);
        db.execSQL(DATABASE_MODES_CREATE);
    }

//    private static void fillCache(Cursor c, Map<String, Long> map) {
//        if (c.moveToFirst()) {
//            do {
//                String key = c.getString(1).toLowerCase();
//                Long value = c.getLong(0);
//                map.put(key, value);
//            } while (c.moveToNext());
//        }
//    }

//    public static void fillCaches(FragmentActivity ctx) {
//        if (mModesMap == null || mTagsMap == null || mThirdPartiesMap == null) {
//            mModesMap = new LinkedHashMap<String, Long>();
//            mTagsMap = new LinkedHashMap<String, Long>();
//            mThirdPartiesMap = new LinkedHashMap<String, Long>();
//
//            InfoCacheFiller cbk = new InfoCacheFiller(ctx);
//
//            ctx.getSupportLoaderManager().initLoader(GET_TP, null, cbk);
//            ctx.getSupportLoaderManager().initLoader(GET_MODES, null, cbk);
//            ctx.getSupportLoaderManager().initLoader(GET_TAGS, null, cbk);
//        }
//    }

//    public static void fillCachesSync(Context ctx) {
//        if (mModesMap == null || mTagsMap == null || mThirdPartiesMap == null) {
//            mModesMap = new LinkedHashMap<String, Long>();
//            mTagsMap = new LinkedHashMap<String, Long>();
//            mThirdPartiesMap = new LinkedHashMap<String, Long>();
//            Uri u = null;
//            String[] cols = null;
//            cols = new String[]{KEY_THIRD_PARTY_ROWID,
//                    KEY_THIRD_PARTY_NORMALIZED_NAME};
//            u = DbContentProvider.THIRD_PARTY_URI;
//            Cursor data = ctx.getContentResolver().query(u, cols, null, null, null);
//            fillCache(data, mThirdPartiesMap);
//
//            cols = new String[]{KEY_MODE_ROWID, KEY_MODE_NORMALIZED_NAME};
//            u = DbContentProvider.MODES_URI;
//            data = ctx.getContentResolver().query(u, cols, null, null, null);
//            fillCache(data, mModesMap);
//
//            cols = new String[]{KEY_TAG_ROWID, KEY_TAG_NORMALIZED_NAME};
//            u = DbContentProvider.TAGS_URI;
//            data = ctx.getContentResolver().query(u, cols, null, null, null);
//            fillCache(data, mTagsMap);
//        }
//    }

//    static long getKeyIdOrCreateFromThirdParties(Context ctx, String key) {
//        return getKeyIdOrCreate(ctx, key, mThirdPartiesMap,
//                DbContentProvider.THIRD_PARTY_URI, KEY_THIRD_PARTY_NAME);
//    }
//
//    static long getKeyIdOrCreateFromTags(Context ctx, String key) {
//        return getKeyIdOrCreate(ctx, key, mTagsMap, DbContentProvider.TAGS_URI,
//                KEY_TAG_NAME);
//    }
//
//    static long getKeyIdOrCreateFromModes(Context ctx, String key) {
//        return getKeyIdOrCreate(ctx, key, mModesMap,
//                DbContentProvider.MODES_URI, KEY_MODE_NAME);
//    }

    private static long getKeyId(Context ctx, String key, Uri table, String col) throws SQLException {
        long res = -1;
        key = AsciiUtils.convertNonAscii(key).trim().toLowerCase();
        if (key == null || key.length() == 0) {
            return -1;
        }
        Cursor c = ctx.getContentResolver().query(table, new String[]{"_id"},
                mColNameNormName.get(col) + "= ?", new String[]{key}, null);
        Long i = null;
        if (c != null) {
            if (c.moveToFirst()) {
                i = c.getLong(0);
            }
            c.close();
        }
        if (null != i) {
            res = i.longValue();
        }
        return res;
    }

    private static long createKeyId(Context ctx, String key, Uri table, String col) throws SQLException {
        String origKey = key.toString();
        key = AsciiUtils.convertNonAscii(key).trim().toLowerCase();
        ContentValues initialValues = new ContentValues();
        initialValues.put(col, origKey);
        initialValues.put(mColNameNormName.get(col), key);
        Uri res = ctx.getContentResolver().insert(table, initialValues);
        long id = Long.parseLong(res.getLastPathSegment());
        if (id == -1) {
            throw new SQLException("Database insertion error : " + key
                    + " in " + table);
        }
        return id;
    }

    public static long getKeyIdIfExistsOrCreate(Context ctx, String key, Uri table) {
        String col = null;
        if (table.equals(DbContentProvider.THIRD_PARTY_URI)) {
            col = KEY_THIRD_PARTY_NAME;
        } else if (table.equals(DbContentProvider.TAGS_URI)) {
            col = KEY_TAG_NAME;
        } else if (table.equals(DbContentProvider.MODES_URI)) {
            col = KEY_MODE_NAME;
        }
        long res = getKeyId(ctx, key, table, col);
        if (res == -1) {
            createKeyId(ctx, key, table, col);
        }
        return res;
    }

    public static boolean updateInfo(Context ctx, Uri table, long rowId, String value, String oldValue) {
        ContentValues args = new ContentValues();
        args.put(mInfoColMap.get(table.toString()), value);
        args.put(mColNameNormName.get(mInfoColMap.get(table.toString())),
                AsciiUtils.convertNonAscii(value).trim().toLowerCase());
        int res = ctx.getContentResolver().update(
                Uri.parse(table + "/" + rowId), args, null, null);

        return res > 0;
    }

    public static boolean deleteInfo(Context ctx, Uri table, long rowId) {
        boolean res = ctx.getContentResolver().delete(
                Uri.parse(table + "/" + rowId), null, null) > 0;
        return res;
    }

    public static Cursor fetchMatchingInfo(Context ctx, Uri table, String colName, String constraint) {
        String where;
        String[] params;
        if (null != constraint) {
            where = mColNameNormName.get(colName) + " LIKE ?";
            params = new String[]{constraint.trim() + "%"};
        } else {
            where = null;
            params = null;
        }
        Cursor c = ctx.getContentResolver().query(table,
                new String[]{"_id", colName}, where, params,
                colName + " asc");
        if (null != c) {
            c.moveToFirst();
        }
        return c;
    }

    public static CursorLoader getMatchingInfoLoader(Context ctx, Uri table, String colName, String constraint) {
        String where;
        String[] params;
        if (null != constraint) {
            where = mColNameNormName.get(colName) + " LIKE ?";
            params = new String[]{constraint.trim() + "%"};
        } else {
            where = null;
            params = null;
        }
        CursorLoader loader = new CursorLoader(ctx, table, new String[]{
                "_id", colName}, where, params, colName + " asc");

        return loader;
    }

    static void putKeyIdInThirdParties(Context ctx, String key, ContentValues initialValues) {
        putKeyId(ctx, key, DbContentProvider.THIRD_PARTY_URI,
                KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_THIRD_PARTY,
                initialValues);
    }

    static void putKeyIdInTags(Context ctx, String key, ContentValues initialValues) {
        putKeyId(ctx, key, DbContentProvider.TAGS_URI, KEY_TAG_NAME,
                OperationTable.KEY_OP_TAG, initialValues);
    }

    static void putKeyIdInModes(Context ctx, String key, ContentValues initialValues) {
        putKeyId(ctx, key, DbContentProvider.MODES_URI, KEY_MODE_NAME,
                OperationTable.KEY_OP_MODE, initialValues);
    }

    private static void putKeyId(Context ctx, String key, Uri keyTableName, String keyTableCol, String opTableCol,
                                 ContentValues initialValues) {
        long id = getKeyId(ctx, key, keyTableName, keyTableCol);
        if (id == -1) {
            if (key.length() > 0) {
                id = createKeyId(ctx, key, keyTableName, keyTableCol);
            }
        }
        if (id != -1) {
            initialValues.put(opTableCol, id);
        } else {
            initialValues.putNull(opTableCol);
        }
    }

    // UPGRADE FUNCTIONS
    static void upgradeFromV8(SQLiteDatabase db, int oldVersion, int newVersion) {
        Cursor c = db.query(DATABASE_THIRD_PARTIES_TABLE, new String[]{
                KEY_THIRD_PARTY_ROWID, KEY_THIRD_PARTY_NAME}, null, null,
                null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                ContentValues v = new ContentValues();
                v.put(KEY_THIRD_PARTY_NORMALIZED_NAME,
                        AsciiUtils.convertNonAscii(
                                c.getString(c
                                        .getColumnIndex(KEY_THIRD_PARTY_NAME)))
                                .toLowerCase());
                db.update(
                        DATABASE_THIRD_PARTIES_TABLE,
                        v,
                        KEY_THIRD_PARTY_ROWID
                                + "="
                                + Long.toString(c.getLong(c
                                .getColumnIndex(KEY_THIRD_PARTY_ROWID))),
                        null);
            } while (c.moveToNext());
            c.close();
        }
        c = db.query(DATABASE_TAGS_TABLE, new String[]{KEY_TAG_ROWID,
                KEY_TAG_NAME}, null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                ContentValues v = new ContentValues();
                v.put(KEY_TAG_NORMALIZED_NAME,
                        AsciiUtils.convertNonAscii(
                                c.getString(c.getColumnIndex(KEY_TAG_NAME)))
                                .toLowerCase());
                db.update(
                        DATABASE_TAGS_TABLE,
                        v,
                        KEY_TAG_ROWID
                                + "="
                                + Long.toString(c.getLong(c
                                .getColumnIndex(KEY_TAG_ROWID))), null);
            } while (c.moveToNext());
            c.close();
        }

        c = db.query(DATABASE_MODES_TABLE, new String[]{KEY_MODE_ROWID,
                KEY_MODE_NAME}, null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                ContentValues v = new ContentValues();
                v.put(KEY_MODE_NORMALIZED_NAME,
                        AsciiUtils.convertNonAscii(
                                c.getString(c.getColumnIndex(KEY_MODE_NAME)))
                                .toLowerCase());
                db.update(
                        DATABASE_MODES_TABLE,
                        v,
                        KEY_MODE_ROWID
                                + "="
                                + Long.toString(c.getLong(c
                                .getColumnIndex(KEY_MODE_ROWID))), null);
            } while (c.moveToNext());
            c.close();
        }
    }

    static void upgradeFromV7(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ADD_NORMALIZED_THIRD_PARTY);
        db.execSQL(ADD_NORMALIZED_TAG);
        db.execSQL(ADD_NORMALIZED_MODE);
    }


    static void updateATableFromV13(SQLiteDatabase db, String table, String tableCreate, String rowKey, String nameKey, String normKey, String strangerKey) {
        final String oldTable = table + "_old";
        db.execSQL("ALTER TABLE " + table + " RENAME TO " + oldTable);
        db.execSQL(tableCreate);
        Cursor itor = db.query(oldTable, new String[]{rowKey, normKey, nameKey}, null, null, null, null, null);
        while (itor != null && itor.moveToFirst()) {
            final String normName = itor.getString(1);
            ContentValues initialVal = new ContentValues();
            initialVal.put(nameKey, itor.getString(2));
            initialVal.put(normKey, normName);
            final long newId = db.insert(table, null, initialVal);
            Log.d("InfoTables", "upgradeFromV13 : newId " + newId + " for " + normName);
            Cursor doubles = db.query(oldTable, new String[]{rowKey},
                    normKey + " LIKE ?", new String[]{normName}, null, null, null);
            if (doubles != null && doubles.moveToFirst()) {
                // collect ids of doubles
                Vector<Long> listOfId = new Vector<Long>(doubles.getCount());
                do {
                    listOfId.add(doubles.getLong(0));
                } while (doubles.moveToNext());
                doubles.close();
                for (Long id : listOfId) {
                    // replace id by refId in operations and sched_op tables
                    ContentValues v = new ContentValues();
                    v.put(strangerKey, newId);
                    int i = db.update(OperationTable.DATABASE_OPERATIONS_TABLE, v,
                            strangerKey + "=?", new String[]{id.toString()});

                    db.update(ScheduledOperationTable.DATABASE_SCHEDULED_TABLE, v,
                            strangerKey + "=?", new String[]{id.toString()});
                    int nb = db.delete(oldTable, rowKey + "=?", new String[]{id.toString()});
                    Log.d("InfoTables", "upgradeFromV13 : nb deleted from " + oldTable + " : " + nb);
                }
            }
            itor.close();
            itor = db.query(oldTable, new String[]{rowKey, normKey, nameKey}, null, null, null, null, null);
        }
        db.execSQL("DROP TABLE " + oldTable);
    }

    // find duplicate of infos and update operations
    static void upgradeFromV13(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TRIGGER on_delete_third_party");
        db.execSQL("DROP TRIGGER on_delete_mode");
        db.execSQL("DROP TRIGGER on_delete_tag");
        updateATableFromV13(db, DATABASE_THIRD_PARTIES_TABLE, DATABASE_THIRD_PARTIES_CREATE, KEY_THIRD_PARTY_ROWID,
                KEY_THIRD_PARTY_NAME, KEY_THIRD_PARTY_NORMALIZED_NAME, OperationTable.KEY_OP_THIRD_PARTY);
        updateATableFromV13(db, DATABASE_MODES_TABLE, DATABASE_MODES_CREATE, KEY_MODE_ROWID,
                KEY_MODE_NAME, KEY_MODE_NORMALIZED_NAME, OperationTable.KEY_OP_MODE);
        updateATableFromV13(db, DATABASE_TAGS_TABLE, DATABASE_TAGS_CREATE, KEY_TAG_ROWID,
                KEY_TAG_NAME, KEY_TAG_NORMALIZED_NAME, OperationTable.KEY_OP_TAG);
        db.execSQL(OperationTable.TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
        db.execSQL(OperationTable.TRIGGER_ON_DELETE_MODE_CREATE);
        db.execSQL(OperationTable.TRIGGER_ON_DELETE_TAG_CREATE);
    }

    public static void upgradeFromV15(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.delete(DATABASE_TAGS_TABLE, KEY_TAG_NAME + "=''", null);
        db.delete(DATABASE_MODES_TABLE, KEY_MODE_NAME + "=''", null);
    }
}
