package fr.geobert.radis.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import fr.geobert.radis.Operation;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.tools.Tools;

public class CommonDbAdapter {
	private static final String TAG = "CommonDbAdapter";
	protected static final String DATABASE_NAME = "radisDb";
	protected static final int DATABASE_VERSION = 7;

	public static final String DATABASE_ACCOUNT_TABLE = "accounts";
	public static final String DATABASE_MODES_TABLE = "modes";
	public static final String DATABASE_THIRD_PARTIES_TABLE = "third_parties";
	public static final String DATABASE_TAGS_TABLE = "tags";
	public static final String DATABASE_OPERATIONS_TABLE = "operations";
	public static final String DATABASE_SCHEDULED_TABLE = "scheduled_ops";

	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ACCOUNT_DESC = "account_desc";
	public static final String KEY_ACCOUNT_START_SUM = "account_start_sum";
	public static final String KEY_ACCOUNT_CUR_SUM = "account_current_sum";
	public static final String KEY_ACCOUNT_OP_SUM = "account_operations_sum";
	public static final String KEY_ACCOUNT_CURRENCY = "account_currency";
	public static final String KEY_ACCOUNT_ROWID = "_id";
	public static final String KEY_ACCOUNT_CUR_SUM_DATE = "account_current_sum_date";

	private static final String DATABASE_ACCOUNT_CREATE = "create table "
			+ DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
			+ " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
			+ " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
			+ KEY_ACCOUNT_START_SUM + " integer not null, "
			+ KEY_ACCOUNT_OP_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM
			+ " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE
			+ " integer not null, " + KEY_ACCOUNT_CURRENCY + " text not null);";

	public static final String KEY_THIRD_PARTY_ROWID = "_id";
	public static final String KEY_THIRD_PARTY_NAME = "third_party_name";

	private static final String DATABASE_THIRD_PARTIES_CREATE = "create table "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ " integer primary key autoincrement, " + KEY_THIRD_PARTY_NAME
			+ " text not null);";

	public static final String KEY_TAG_ROWID = "_id";
	public static final String KEY_TAG_NAME = "tag_name";

	private static final String DATABASE_TAGS_CREATE = "create table "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ " integer primary key autoincrement, " + KEY_TAG_NAME
			+ " text not null);";

	public static final String KEY_MODE_ROWID = "_id";
	public static final String KEY_MODE_NAME = "mode_name";

	private static final String DATABASE_MODES_CREATE = "create table "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID
			+ " integer primary key autoincrement, " + KEY_MODE_NAME
			+ " text not null);";

	public static final String KEY_OP_DATE = "date";
	public static final String KEY_OP_THIRD_PARTY = "third_party";
	public static final String KEY_OP_TAG = "tag";
	public static final String KEY_OP_MODE = "mode";
	public static final String KEY_OP_SUM = "sum";
	public static final String KEY_OP_SCHEDULED_ID = "scheduled_id";
	public static final String KEY_OP_ACCOUNT_ID = "account_id";
	public static final String KEY_OP_ROWID = "_id";
	public static final String KEY_OP_NOTES = "notes";

	public static final String KEY_SCHEDULED_END_DATE = "end_date";
	public static final String KEY_SCHEDULED_PERIODICITY = "periodicity";
	public static final String KEY_SCHEDULED_ACCOUNT_ID = "scheduled_account_id";
	public static final String KEY_SCHEDULED_ROWID = "_id";
	public static final String KEY_SCHEDULED_PERIODICITY_UNIT = "periodicity_units";

	protected static final String DATABASE_SCHEDULED_CREATE = "create table "
			+ DATABASE_SCHEDULED_TABLE + "(" + KEY_SCHEDULED_ROWID
			+ " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY
			+ " integer, " + KEY_OP_TAG + " integer, " + KEY_OP_SUM
			+ " integer not null, " + KEY_SCHEDULED_ACCOUNT_ID
			+ " integer not null, " + KEY_OP_MODE + " integer, " + KEY_OP_DATE
			+ " integer not null, " + KEY_SCHEDULED_END_DATE + " integer, "
			+ KEY_SCHEDULED_PERIODICITY + " integer, "
			+ KEY_SCHEDULED_PERIODICITY_UNIT + " integer not null, "
			+ KEY_OP_NOTES + " text, FOREIGN KEY (" + KEY_OP_THIRD_PARTY
			+ ") REFERENCES " + DATABASE_THIRD_PARTIES_TABLE + "("
			+ KEY_THIRD_PARTY_ROWID + "), FOREIGN KEY (" + KEY_OP_TAG
			+ ") REFERENCES " + DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_MODE + ") REFERENCES "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID + "));";

	protected static final String DATABASE_OP_CREATE = "create table "
			+ DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ROWID
			+ " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY
			+ " integer, " + KEY_OP_TAG + " integer, " + KEY_OP_SUM
			+ " integer not null, " + KEY_OP_ACCOUNT_ID + " integer not null, "
			+ KEY_OP_MODE + " integer, " + KEY_OP_DATE + " integer not null, "
			+ KEY_OP_NOTES + " text, " + KEY_OP_SCHEDULED_ID
			+ " integer, FOREIGN KEY (" + KEY_OP_THIRD_PARTY + ") REFERENCES "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_TAG + ") REFERENCES "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID + "), FOREIGN KEY ("
			+ KEY_OP_MODE + ") REFERENCES " + DATABASE_MODES_TABLE + "("
			+ KEY_MODE_ROWID + "), FOREIGN KEY (" + KEY_OP_SCHEDULED_ID
			+ ") REFERENCES " + DATABASE_SCHEDULED_TABLE + "("
			+ KEY_SCHEDULED_ROWID + "));";

	// meta
	protected static final String INDEX_ON_ACCOUNT_ID_CREATE = "CREATE INDEX IF NOT EXISTS account_id_idx ON "
			+ DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ACCOUNT_ID + ")";

	protected static final String TRIGGER_ON_DELETE_THIRD_PARTY_CREATE = "CREATE TRIGGER on_delete_third_party AFTER DELETE ON "
			+ DATABASE_THIRD_PARTIES_TABLE
			+ " BEGIN UPDATE "
			+ DATABASE_OPERATIONS_TABLE
			+ " SET "
			+ KEY_OP_THIRD_PARTY
			+ " = null WHERE "
			+ KEY_OP_THIRD_PARTY
			+ " = old."
			+ KEY_THIRD_PARTY_ROWID
			+ "; UPDATE "
			+ DATABASE_SCHEDULED_TABLE
			+ " SET "
			+ KEY_OP_THIRD_PARTY
			+ " = null WHERE "
			+ KEY_OP_THIRD_PARTY + " = old." + KEY_THIRD_PARTY_ROWID + "; END";
	protected static final String TRIGGER_ON_DELETE_MODE_CREATE = "CREATE TRIGGER on_delete_mode AFTER DELETE ON "
			+ DATABASE_MODES_TABLE
			+ " BEGIN UPDATE "
			+ DATABASE_OPERATIONS_TABLE
			+ " SET "
			+ KEY_OP_MODE
			+ " = null WHERE "
			+ KEY_OP_MODE
			+ " = old."
			+ KEY_MODE_ROWID
			+ "; UPDATE "
			+ DATABASE_SCHEDULED_TABLE
			+ " SET "
			+ KEY_OP_MODE
			+ " = null WHERE "
			+ KEY_OP_MODE
			+ " = old."
			+ KEY_MODE_ROWID
			+ "; END";
	protected static final String TRIGGER_ON_DELETE_TAG_CREATE = "CREATE TRIGGER on_delete_tag AFTER DELETE ON "
			+ DATABASE_TAGS_TABLE
			+ " BEGIN UPDATE "
			+ DATABASE_OPERATIONS_TABLE
			+ " SET "
			+ KEY_OP_TAG
			+ " = null WHERE "
			+ KEY_OP_TAG
			+ " = old."
			+ KEY_TAG_ROWID
			+ "; UPDATE "
			+ DATABASE_SCHEDULED_TABLE
			+ " SET "
			+ KEY_OP_TAG
			+ " = null WHERE "
			+ KEY_OP_TAG
			+ " = old."
			+ KEY_TAG_ROWID
			+ "; END";
	protected static final String ADD_NOTES_COLUNM = "ALTER TABLE "
			+ DATABASE_OPERATIONS_TABLE + " ADD COLUMN " + KEY_OP_NOTES
			+ " text";
	protected static final String ADD_CUR_DATE_COLUNM = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
			+ KEY_ACCOUNT_CUR_SUM_DATE + " integer not null DEFAULT 0";

	private long mAccountId;

	private LinkedHashMap<String, Long> mModesMap;
	private LinkedHashMap<String, Long> mTagsMap;
	private LinkedHashMap<String, Long> mThirdPartiesMap;

	private HashMap<String, Cursor> mInfoCursorMap;

	@SuppressWarnings("serial")
	private static final HashMap<String, String> mInfoColMap = new HashMap<String, String>() {
		{
			put(DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME);
			put(DATABASE_TAGS_TABLE, KEY_TAG_NAME);
			put(DATABASE_MODES_TABLE, KEY_MODE_NAME);
		}
	};

	private static final String OP_ORDERING = "ops." + KEY_OP_DATE
			+ " desc, ops." + KEY_OP_ROWID + " desc";

	private final String DATABASE_OP_TABLE_JOINTURE = DATABASE_OPERATIONS_TABLE
			+ " ops LEFT OUTER JOIN " + DATABASE_THIRD_PARTIES_TABLE
			+ " tp ON ops." + KEY_OP_THIRD_PARTY + " = tp."
			+ KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN "
			+ DATABASE_MODES_TABLE + " mode ON ops." + KEY_OP_MODE + " = mode."
			+ KEY_MODE_ROWID + " LEFT OUTER JOIN " + DATABASE_TAGS_TABLE
			+ " tag ON ops." + KEY_OP_TAG + " = tag." + KEY_TAG_ROWID;

	public static final String[] OP_COLS_QUERY = { "ops." + KEY_OP_ROWID,
			"tp." + KEY_THIRD_PARTY_NAME, "tag." + KEY_TAG_NAME,
			"mode." + KEY_MODE_NAME, "ops." + KEY_OP_SUM, "ops." + KEY_OP_DATE,
			"ops." + KEY_OP_ACCOUNT_ID, "ops." + KEY_OP_NOTES,
			"ops." + KEY_OP_SCHEDULED_ID };

	private static final String RESTRICT_TO_ACCOUNT = "ops."
			+ KEY_OP_ACCOUNT_ID + " = %d";

	private static final String SCHEDULED_OP_ORDERING = "sch." + KEY_OP_DATE
			+ " desc, sch." + KEY_SCHEDULED_ROWID + " desc";

	private final String DATABASE_SCHEDULED_TABLE_JOINTURE = DATABASE_SCHEDULED_TABLE
			+ " sch LEFT OUTER JOIN "
			+ DATABASE_THIRD_PARTIES_TABLE
			+ " tp ON sch."
			+ KEY_OP_THIRD_PARTY
			+ " = tp."
			+ KEY_THIRD_PARTY_ROWID
			+ " LEFT OUTER JOIN "
			+ DATABASE_MODES_TABLE
			+ " mode ON sch."
			+ KEY_OP_MODE
			+ " = mode."
			+ KEY_MODE_ROWID
			+ " LEFT OUTER JOIN "
			+ DATABASE_TAGS_TABLE
			+ " tag ON sch."
			+ KEY_OP_TAG
			+ " = tag."
			+ KEY_TAG_ROWID
			+ " LEFT OUTER JOIN "
			+ DATABASE_ACCOUNT_TABLE
			+ " acc ON sch."
			+ KEY_SCHEDULED_ACCOUNT_ID + " = acc." + KEY_ACCOUNT_ROWID;

	public static final String[] SCHEDULED_OP_COLS_QUERY = {
			"sch." + KEY_SCHEDULED_ROWID, "tp." + KEY_THIRD_PARTY_NAME,
			"tag." + KEY_TAG_NAME, "mode." + KEY_MODE_NAME,
			"sch." + KEY_OP_SUM, "sch." + KEY_OP_DATE,
			"sch." + KEY_SCHEDULED_ACCOUNT_ID, "acc." + KEY_ACCOUNT_NAME,
			"sch." + KEY_OP_NOTES, "sch." + KEY_SCHEDULED_END_DATE,
			"sch." + KEY_SCHEDULED_PERIODICITY,
			"sch." + KEY_SCHEDULED_PERIODICITY_UNIT };

	protected DatabaseHelper mDbHelper;
	protected SQLiteDatabase mDb;
	protected Context mCtx;
	protected Cursor mCurAccount;

	private static CommonDbAdapter mInstance = null;

	// private static int mRefCounter = 0;

	public CommonDbAdapter() {
		mInfoCursorMap = new HashMap<String, Cursor>();
	}

	private void init(Context ctx, long accountRowId) {
		this.mCtx = ctx;
		mAccountId = accountRowId;
	}

	public static CommonDbAdapter getInstance(Context ctx) {
		return CommonDbAdapter.getInstance(ctx, 0);
	}

	public static CommonDbAdapter getInstance(Context ctx, final long accountId) {
		if (null == mInstance) {
			mInstance = new CommonDbAdapter();
		}
		mInstance.init(ctx, accountId);
		return mInstance;
	}

	protected class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_ACCOUNT_CREATE);
			db.execSQL(DATABASE_OP_CREATE);
			db.execSQL(DATABASE_THIRD_PARTIES_CREATE);
			db.execSQL(DATABASE_MODES_CREATE);
			db.execSQL(DATABASE_TAGS_CREATE);
			db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE);
			db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
			db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
			db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
			db.execSQL(DATABASE_SCHEDULED_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", converting to new database");
			switch (oldVersion) {
			case 1:
				db.execSQL(DATABASE_OP_CREATE);
				Cursor allAccounts = db.query(DATABASE_ACCOUNT_TABLE,
						new String[] {}, null, null, null, null, null);
				if (null != allAccounts) {
					if (allAccounts.moveToFirst()) {
						do {
							int accountId = allAccounts.getInt(allAccounts
									.getColumnIndex(KEY_ACCOUNT_ROWID));
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
			case 2:
				db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
			case 3:
				db.execSQL(ADD_NOTES_COLUNM);
			case 4:
				db.execSQL(ADD_CUR_DATE_COLUNM);
			case 5:
				// must recreate operations table as ALTER does not support
				// ALTER COLUMN in SQLITE
				db.execSQL("DROP TRIGGER on_delete_third_party");
				db.execSQL("DROP TRIGGER on_delete_mode");
				db.execSQL("DROP TRIGGER on_delete_tag");

				db.execSQL("ALTER TABLE operations RENAME TO operations_old;");
				db.execSQL(DATABASE_SCHEDULED_CREATE);
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
			case 6:
				db.execSQL("DROP TRIGGER on_delete_third_party");
				db.execSQL("DROP TRIGGER on_delete_mode");
				db.execSQL("DROP TRIGGER on_delete_tag");

				db.execSQL("ALTER TABLE accounts RENAME TO accounts_old;");
				db.execSQL(DATABASE_ACCOUNT_CREATE);
				Cursor c = db.query("accounts_old", new String[] {
						KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME,
						KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
						KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_DESC,
						KEY_ACCOUNT_OP_SUM, KEY_ACCOUNT_START_SUM }, null,
						null, null, null, null);
				if (null != c && c.moveToFirst()) {
					do {
						ContentValues initialValues = new ContentValues();
						initialValues
								.put(KEY_ACCOUNT_NAME, c.getString(c
										.getColumnIndex(KEY_ACCOUNT_NAME)));
						initialValues
								.put(KEY_ACCOUNT_DESC, c.getString(c
										.getColumnIndex(KEY_ACCOUNT_DESC)));
						double d = c.getDouble(c
								.getColumnIndex(KEY_ACCOUNT_START_SUM));
						long l = Math.round(d * 100);
						initialValues.put(KEY_ACCOUNT_START_SUM, l);
						d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_OP_SUM));
						l = Math.round(d * 100);
						initialValues.put(KEY_ACCOUNT_OP_SUM, l);
						d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM));
						l = Math.round(d * 100);
						initialValues.put(KEY_ACCOUNT_CUR_SUM, l);
						initialValues.put(KEY_ACCOUNT_CURRENCY, c.getString(c
								.getColumnIndex(KEY_ACCOUNT_CURRENCY)));
						initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE, c.getLong(c
								.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE)));
						long id = db.insert(DATABASE_ACCOUNT_TABLE, null,
								initialValues);
						initialValues = new ContentValues();
						initialValues.put(KEY_OP_ACCOUNT_ID, id);
						db.update(
								DATABASE_OPERATIONS_TABLE,
								initialValues,
								KEY_OP_ACCOUNT_ID
										+ "="
										+ c.getLong(c
												.getColumnIndex(KEY_ACCOUNT_ROWID)),
								null);

					} while (c.moveToNext());
					c.close();
				}

				db.execSQL("ALTER TABLE scheduled_ops RENAME TO scheduled_ops_old;");
				db.execSQL(DATABASE_SCHEDULED_CREATE);
				c = db.query("scheduled_ops_old", new String[] {
						KEY_SCHEDULED_ROWID, KEY_OP_THIRD_PARTY, KEY_OP_TAG,
						KEY_OP_SUM, KEY_SCHEDULED_ACCOUNT_ID, KEY_OP_MODE,
						KEY_OP_DATE, KEY_SCHEDULED_END_DATE,
						KEY_SCHEDULED_PERIODICITY,
						KEY_SCHEDULED_PERIODICITY_UNIT, KEY_OP_NOTES }, null,
						null, null, null, null);
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
						initialValues.put(KEY_SCHEDULED_ACCOUNT_ID, c.getInt(c
								.getColumnIndex(KEY_SCHEDULED_ACCOUNT_ID)));
						initialValues.put(KEY_OP_MODE,
								c.getInt(c.getColumnIndex(KEY_OP_MODE)));
						initialValues.put(KEY_OP_DATE,
								c.getLong(c.getColumnIndex(KEY_OP_DATE)));
						initialValues.put(KEY_SCHEDULED_END_DATE, c.getLong(c
								.getColumnIndex(KEY_SCHEDULED_END_DATE)));
						initialValues.put(KEY_SCHEDULED_PERIODICITY, c.getInt(c
								.getColumnIndex(KEY_SCHEDULED_PERIODICITY)));
						initialValues
								.put(KEY_SCHEDULED_PERIODICITY_UNIT,
										c.getInt(c
												.getColumnIndex(KEY_SCHEDULED_PERIODICITY_UNIT)));
						initialValues.put(KEY_OP_NOTES,
								c.getString(c.getColumnIndex(KEY_OP_NOTES)));
						long id = db.insert(DATABASE_SCHEDULED_TABLE, null,
								initialValues);
						initialValues = new ContentValues();
						initialValues.put(KEY_OP_SCHEDULED_ID, id);
						db.update(
								DATABASE_OPERATIONS_TABLE,
								initialValues,
								KEY_OP_SCHEDULED_ID
										+ "="
										+ c.getLong(c
												.getColumnIndex(KEY_SCHEDULED_ROWID)),
								null);

					} while (c.moveToNext());
					c.close();
				}

				db.execSQL("ALTER TABLE operations RENAME TO operations_old;");
				db.execSQL(DATABASE_OP_CREATE);
				c = db.query("operations_old", new String[] { KEY_OP_ROWID,
						KEY_OP_ACCOUNT_ID, KEY_OP_THIRD_PARTY, KEY_OP_TAG,
						KEY_OP_SUM, KEY_OP_MODE, KEY_OP_DATE,
						KEY_OP_SCHEDULED_ID, KEY_OP_NOTES }, null, null, null,
						null, null);
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
						initialValues.put(KEY_OP_SCHEDULED_ID, c.getLong(c
								.getColumnIndex(KEY_OP_SCHEDULED_ID)));
						initialValues.put(KEY_OP_NOTES,
								c.getString(c.getColumnIndex(KEY_OP_NOTES)));
						db.insert(DATABASE_OPERATIONS_TABLE, null,
								initialValues);
					} while (c.moveToNext());
					c.close();
				}

				db.execSQL("DROP TABLE accounts_old;");
				db.execSQL("DROP TABLE operations_old;");
				db.execSQL("DROP TABLE scheduled_ops_old;");
				db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);

				c = db.query(DATABASE_ACCOUNT_TABLE,
						new String[] { KEY_ACCOUNT_ROWID }, null, null, null,
						null, null);
				if (null != c && c.moveToFirst()) {
					do {
						CommonDbAdapter.this.consolidateSums(c.getLong(0), db);
					} while (c.moveToNext());
					c.close();
				}
				break;
			default:
				break;
			}

		}
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public CommonDbAdapter open() throws SQLException {
		if (null == mDbHelper) {
			mDbHelper = new DatabaseHelper(mCtx);
			mDb = mDbHelper.getWritableDatabase();
			fillCaches();
		}
		return this;
	}

	public void close() {
		if (null != mDbHelper) {
			mDbHelper.close();
			mDbHelper = null;
			mDb.close();
			mDb = null;
		}
	}

	private void fillCache(String table, String[] cols, Map<String, Long> map) {
		Cursor c = mDb.query(table, cols, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				String key = c.getString(1);
				Long value = c.getLong(0);
				map.put(key, value);
			} while (c.moveToNext());
		}
		c.close();
	}

	private void fillCaches() {
		mModesMap = new LinkedHashMap<String, Long>();
		mTagsMap = new LinkedHashMap<String, Long>();
		mThirdPartiesMap = new LinkedHashMap<String, Long>();

		fillCache(DATABASE_MODES_TABLE, new String[] { KEY_MODE_ROWID,
				KEY_MODE_NAME }, mModesMap);
		fillCache(DATABASE_TAGS_TABLE, new String[] { KEY_TAG_ROWID,
				KEY_TAG_NAME }, mTagsMap);
		fillCache(DATABASE_THIRD_PARTIES_TABLE, new String[] {
				KEY_THIRD_PARTY_ROWID, KEY_THIRD_PARTY_NAME }, mThirdPartiesMap);
	}

	public long createAccount(String name, String desc, long start_sum,
			String currency) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ACCOUNT_NAME, name);
		initialValues.put(KEY_ACCOUNT_DESC, desc);
		initialValues.put(KEY_ACCOUNT_START_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_OP_SUM, 0);
		initialValues.put(KEY_ACCOUNT_CUR_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_CURRENCY, currency);
		initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE, 0);
		return mDb.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
	}

	public boolean deleteAccount(long rowId) {
		return mDb.delete(DATABASE_ACCOUNT_TABLE, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public Cursor fetchAllAccounts() {
		Cursor c = mDb.query(DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM,
				KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE }, null, null,
				null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAccount(long accountId) throws SQLException {
		mCurAccount = mDb.query(true, DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_DESC,
				KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_OP_SUM,
				KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE },
				KEY_ACCOUNT_ROWID + "=" + accountId, null, null, null, null,
				null);
		Cursor c = mCurAccount;
		if (c != null) {
			c.moveToFirst();
		}
		return c;

	}

	public boolean updateAccount(long accountId, String name, String desc,
			long start_sum, String currency) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_NAME, name);
		args.put(KEY_ACCOUNT_DESC, desc);
		args.put(KEY_ACCOUNT_START_SUM, start_sum);
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ accountId, null) > 0;
	}

	public boolean updateOpSum(long accountId, long newSum) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_OP_SUM, newSum);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ accountId, null) > 0;
	}

	public long updateCurrentSum(long accountId, long date) {
		Cursor account = fetchAccount(accountId);
		long start = account.getLong(account
				.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_START_SUM));
		long opSum = account.getLong(account
				.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
		account.close();
		ContentValues args = new ContentValues();
		long curSum = start + opSum;
		args.put(KEY_ACCOUNT_CUR_SUM, curSum);
		if (0 != date) {
			args.put(KEY_ACCOUNT_CUR_SUM_DATE, date);
		}
		mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ accountId, null);
		return curSum;
	}

	public long getKeyIdOrCreate(String key, String table) throws SQLException {
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			return getKeyIdOrCreate(key, mThirdPartiesMap, table,
					KEY_THIRD_PARTY_NAME);
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			return getKeyIdOrCreate(key, mTagsMap, table, KEY_TAG_NAME);
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			return getKeyIdOrCreate(key, mModesMap, table, KEY_MODE_NAME);
		}
		return 0;
	}

	public long getKeyIdIfExists(String key, String table) {
		Long res = null;
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			res = mThirdPartiesMap.get(key);
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			res = mTagsMap.get(key);
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			res = mModesMap.get(key);
		}
		if (null != res) {
			return res.longValue();
		}
		return -1;
	}

	private long getKeyIdOrCreate(String key, LinkedHashMap<String, Long> map,
			String table, String col) throws SQLException {
		key = key.trim();
		if (key.length() == 0) {
			return -1;
		}
		Long i = map.get(key);
		if (null != i) {
			return i.longValue();
		} else {
			ContentValues initialValues = new ContentValues();
			initialValues.put(col, key);
			long id = mDb.insert(table, null, initialValues);
			if (id != -1) {
				map.put(key, id);
			} else {
				throw new SQLException("Database insertion error : " + key
						+ " in " + table);
			}
			return id;
		}
	}

	private void putKeyId(String key, String keyTableName, String keyTableCol,
			String opTableCol, LinkedHashMap<String, Long> keyMap,
			ContentValues initialValues) {
		long id = getKeyIdOrCreate(key, keyMap, keyTableName, keyTableCol);
		if (id != -1) {
			initialValues.put(opTableCol, id);
		} else {
			initialValues.putNull(opTableCol);
		}
	}

	public long createOp(Operation op) {
		return createOp(op, mAccountId);
	}

	public long createOp(Operation op, final long accountId) {
		ContentValues initialValues = new ContentValues();
		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, initialValues);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				initialValues);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, initialValues);

		initialValues.put(KEY_OP_SUM, op.mSum);
		initialValues.put(KEY_OP_DATE, op.getDate());
		initialValues.put(KEY_OP_ACCOUNT_ID, accountId);
		initialValues.put(KEY_OP_NOTES, op.mNotes);
		initialValues.put(KEY_OP_SCHEDULED_ID, op.mScheduledId);
		return mDb.insert(DATABASE_OPERATIONS_TABLE, null, initialValues);
	}

	public boolean deleteOp(long rowId) {
		return mDb.delete(DATABASE_OPERATIONS_TABLE,
				KEY_OP_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchNLastOps(int nbOps) {
		return fetchNLastOps(nbOps, mAccountId);
	}

	public Cursor fetchNLastOps(int nbOps, final long accountId) {
		return mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId), null, null,
				null, OP_ORDERING, Integer.toString(nbOps));
	}

	public Cursor fetchLastOp(final long accountId) {
		Cursor c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops."
						+ KEY_OP_DATE + " = (SELECT max(ops." + KEY_OP_DATE
						+ ") FROM " + DATABASE_OPERATIONS_TABLE + ") ", null,
				null, null, OP_ORDERING, null);
		return c;
	}

	public Cursor fetchAllOps(final long accountId) {
		return mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId), null, null,
				null, OP_ORDERING, null);
	}

	public Cursor fetchOneOp(final long rowId, final long accountId) {
		Cursor c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops."
						+ KEY_OP_ROWID + " = " + rowId, null, null, null, null,
				null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOneOp(final long rowId) {
		return fetchOneOp(rowId, mAccountId);
	}

	public Cursor fetchOpEarlierThan(long date, int nbOps) {
		Cursor c = null;
		c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, mAccountId) + " AND ops."
						+ KEY_OP_DATE + " < " + date, null, null, null,
				OP_ORDERING, Integer.toString(nbOps));
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOpOfMonth(final int curMonth) {
		Cursor c = null;
		GregorianCalendar startDate = new GregorianCalendar();
		GregorianCalendar endDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(startDate);
		Tools.clearTimeOfCalendar(endDate);
		startDate.set(Calendar.MONTH, curMonth);
		endDate.set(Calendar.MONTH, curMonth);
		startDate.set(Calendar.DAY_OF_MONTH,
				startDate.getActualMinimum(Calendar.DAY_OF_MONTH));
		endDate.set(Calendar.DAY_OF_MONTH,
				endDate.getActualMaximum(Calendar.DAY_OF_MONTH));

		c = mDb.query(
				DATABASE_OP_TABLE_JOINTURE,
				OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, mAccountId) + " AND ops."
						+ KEY_OP_DATE + " <= " + endDate.getTimeInMillis()
						+ " AND ops." + KEY_OP_DATE + " >= "
						+ startDate.getTimeInMillis(), null, null, null,
				OP_ORDERING, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	// start < end
	public Cursor fetchOpBetweenMonthes(final int startMonth, final int endMonth) {
		Cursor c = null;
		GregorianCalendar startDate = new GregorianCalendar();
		GregorianCalendar endDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(startDate);
		Tools.clearTimeOfCalendar(endDate);
		startDate.set(Calendar.MONTH, startMonth);
		endDate.set(Calendar.MONTH, endMonth);
		startDate.set(Calendar.DAY_OF_MONTH,
				startDate.getActualMinimum(Calendar.DAY_OF_MONTH));
		endDate.set(Calendar.DAY_OF_MONTH,
				endDate.getActualMaximum(Calendar.DAY_OF_MONTH));

		c = mDb.query(
				DATABASE_OP_TABLE_JOINTURE,
				OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, mAccountId) + " AND ops."
						+ KEY_OP_DATE + " <= " + endDate.getTimeInMillis()
						+ " AND ops." + KEY_OP_DATE + " >= "
						+ startDate.getTimeInMillis(), null, null, null,
				OP_ORDERING, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	private ContentValues createContentValuesFromOp(final Operation op,
			final boolean updateOccurrences) {
		ContentValues args = new ContentValues();

		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, args);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				args);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, args);

		args.put(KEY_OP_SUM, op.mSum);
		args.put(KEY_OP_NOTES, op.mNotes);
		if (!updateOccurrences) {
			args.put(KEY_OP_DATE, op.getDate());
			args.put(KEY_OP_SCHEDULED_ID, op.mScheduledId);
		}
		return args;
	}

	public boolean updateOp(final long rowId, final Operation op) {
		ContentValues args = createContentValuesFromOp(op, false);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ROWID + "="
				+ rowId, null) > 0;
	}

	public boolean updateOp(final long opId, final long schOpId) {
		ContentValues args = new ContentValues();
		args.put(KEY_OP_SCHEDULED_ID, schOpId);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ROWID + "="
				+ opId, null) > 0;
	}

	// ----------------------
	// SCHEDULED TRANSACTIONS
	// ----------------------
	public Cursor fetchAllScheduledOps() {
		Cursor c = mDb.query(DATABASE_SCHEDULED_TABLE_JOINTURE,
				SCHEDULED_OP_COLS_QUERY, null, null, null, null,
				SCHEDULED_OP_ORDERING);
		if (null != c) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOneScheduledOp(long rowId) {
		Cursor c = mDb.query(DATABASE_SCHEDULED_TABLE_JOINTURE,
				SCHEDULED_OP_COLS_QUERY, "sch." + KEY_OP_ROWID + " = " + rowId,
				null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public long createScheduledOp(ScheduledOperation op) {
		ContentValues initialValues = new ContentValues();
		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, initialValues);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				initialValues);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, initialValues);

		initialValues.put(KEY_OP_SUM, op.mSum);
		initialValues.put(KEY_OP_DATE, op.getDate());
		initialValues.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId);
		initialValues.put(KEY_OP_NOTES, op.mNotes);
		initialValues.put(KEY_SCHEDULED_END_DATE, op.getEndDate());
		initialValues.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity);
		initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit);
		return mDb.insert(DATABASE_SCHEDULED_TABLE, null, initialValues);
	}

	public boolean updateScheduledOp(long rowId, ScheduledOperation op,
			final boolean isUpdatedFromOccurence) {
		ContentValues args = new ContentValues();

		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, args);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				args);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, args);

		args.put(KEY_OP_SUM, op.mSum);
		args.put(KEY_OP_NOTES, op.mNotes);
		if (!isUpdatedFromOccurence) {
			args.put(KEY_SCHEDULED_END_DATE, op.getEndDate());
			args.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity);
			args.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit);
			args.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId);
			args.put(KEY_OP_DATE, op.getDate());
		}
		return mDb.update(DATABASE_SCHEDULED_TABLE, args, KEY_OP_ROWID + "="
				+ rowId, null) > 0;
	}

	public boolean deleteScheduledOp(final long schOpId) {
		return mDb.delete(DATABASE_SCHEDULED_TABLE, KEY_SCHEDULED_ROWID + "="
				+ schOpId, null) > 0;
	}

	public int deleteAllOccurrences(final long accountId, final long schOpId) {
		return mDb.delete(DATABASE_OPERATIONS_TABLE, KEY_OP_ACCOUNT_ID + "="
				+ accountId + " AND " + KEY_OP_SCHEDULED_ID + "=" + schOpId,
				null);
	}

	public int updateAllOccurrences(final long accountId, final long schOpId,
			final Operation op) {
		ContentValues args = createContentValuesFromOp(op, true);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ACCOUNT_ID
				+ "=" + accountId + " AND " + KEY_OP_SCHEDULED_ID + "="
				+ schOpId, null);
	}

	public int disconnectAllOccurrences(final long accountId, final long schOpId) {
		ContentValues args = new ContentValues();
		args.put(KEY_OP_SCHEDULED_ID, 0);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ACCOUNT_ID
				+ "=" + accountId + " AND " + KEY_OP_SCHEDULED_ID + "="
				+ schOpId, null);
	}

	// ------------------------------
	// INFOS (third party, tag, mode)
	// ------------------------------
	public boolean updateInfo(String table, long rowId, String value) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		return mDb.update(table, args, "_id =" + rowId, null) > 0;
	}

	public long createInfo(String table, String value) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		return mDb.insert(table, null, args);
	}

	public boolean deleteInfo(String table, long rowId) {
		boolean res = mDb.delete(table, "_id =" + rowId, null) > 0;
		mInfoCursorMap.get(table).requery();
		return res;
	}

	public Cursor fetchMatchingInfo(String table, String colName,
			String constraint) {
		String where;
		String[] params;
		if (null != constraint) {
			where = colName + " LIKE ?";
			params = new String[] { constraint.trim() + "%" };
		} else {
			where = null;
			params = null;
		}
		Cursor c = mDb.query(table, new String[] { "_id", colName }, where,
				params, null, null, colName + " asc");
		if (null != c) {
			c.moveToFirst();
		}
		mInfoCursorMap.put(table, c);
		return c;
	}

	public void trashDatabase() {
		close();
		mCtx.deleteDatabase(DATABASE_NAME);
	}

	public boolean backupDatabase() {
		close();
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
					FileChannel src = new FileInputStream(currentDB)
							.getChannel();
					FileChannel dst = new FileOutputStream(backupDB)
							.getChannel();

					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean restoreDatabase() {
		close();
		try {
			File sd = Environment.getExternalStorageDirectory();

			String backupDBPath = "/radis/radisDb";
			File currentDB = mCtx.getDatabasePath(DATABASE_NAME);
			File backupDB = new File(sd, backupDBPath);

			if (backupDB.exists()) {
				FileChannel dst = new FileOutputStream(currentDB).getChannel();
				FileChannel src = new FileInputStream(backupDB).getChannel();

				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void consolidateSums() {
		consolidateSums(mAccountId, null);
	}

	private void consolidateSums(final long accountId, SQLiteDatabase db) {
		if (0 != accountId) {
			if (null != db) {
				mDb = db;
			}
			long recalculatedOpSum = 0;
			Cursor c = fetchAllOps(accountId);
			if (null != c && c.moveToFirst()) {
				do {
					recalculatedOpSum += c.getDouble(c
							.getColumnIndex(KEY_OP_SUM));
				} while (c.moveToNext());
				updateOpSum(accountId, recalculatedOpSum);
				updateCurrentSum(accountId, 0);
			}
		}
	}

}
