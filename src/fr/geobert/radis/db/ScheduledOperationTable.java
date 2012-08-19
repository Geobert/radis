package fr.geobert.radis.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import fr.geobert.radis.ScheduledOperation;

public class ScheduledOperationTable {
	static final String DATABASE_SCHEDULED_TABLE = "scheduled_ops";
	public static final String KEY_SCHEDULED_END_DATE = "end_date";
	public static final String KEY_SCHEDULED_PERIODICITY = "periodicity";
	public static final String KEY_SCHEDULED_ACCOUNT_ID = "scheduled_account_id";
	public static final String KEY_SCHEDULED_ROWID = "_id";
	public static final String KEY_SCHEDULED_PERIODICITY_UNIT = "periodicity_units";

	protected static final String DATABASE_SCHEDULED_CREATE = "create table "
			+ DATABASE_SCHEDULED_TABLE + "(" + KEY_SCHEDULED_ROWID
			+ " integer primary key autoincrement, "
			+ OperationTable.KEY_OP_THIRD_PARTY + " integer, "
			+ OperationTable.KEY_OP_TAG + " integer, "
			+ OperationTable.KEY_OP_SUM + " integer not null, "
			+ KEY_SCHEDULED_ACCOUNT_ID + " integer not null, "
			+ OperationTable.KEY_OP_MODE + " integer, "
			+ OperationTable.KEY_OP_DATE + " integer not null, "
			+ KEY_SCHEDULED_END_DATE + " integer, " + KEY_SCHEDULED_PERIODICITY
			+ " integer, " + KEY_SCHEDULED_PERIODICITY_UNIT
			+ " integer not null, " + OperationTable.KEY_OP_NOTES + " text, "
			+ OperationTable.KEY_OP_TRANSFERT_ACC_NAME + " text, "
			+ OperationTable.KEY_OP_TRANSFERT_ACC_ID
			+ " integer not null, FOREIGN KEY ("
			+ OperationTable.KEY_OP_THIRD_PARTY + ") REFERENCES "
			+ InfoTables.DATABASE_THIRD_PARTIES_TABLE + "("
			+ InfoTables.KEY_THIRD_PARTY_ROWID + "), FOREIGN KEY ("
			+ OperationTable.KEY_OP_TAG + ") REFERENCES "
			+ InfoTables.DATABASE_TAGS_TABLE + "(" + InfoTables.KEY_TAG_ROWID
			+ "), FOREIGN KEY (" + OperationTable.KEY_OP_MODE + ") REFERENCES "
			+ InfoTables.DATABASE_MODES_TABLE + "(" + InfoTables.KEY_MODE_ROWID
			+ "));";

	private static final String SCHEDULED_OP_ORDERING = "sch."
			+ OperationTable.KEY_OP_DATE + " desc, sch." + KEY_SCHEDULED_ROWID
			+ " desc";

	public final static String DATABASE_SCHEDULED_TABLE_JOINTURE = DATABASE_SCHEDULED_TABLE
			+ " sch LEFT OUTER JOIN "
			+ InfoTables.DATABASE_THIRD_PARTIES_TABLE
			+ " tp ON sch."
			+ OperationTable.KEY_OP_THIRD_PARTY
			+ " = tp."
			+ InfoTables.KEY_THIRD_PARTY_ROWID
			+ " LEFT OUTER JOIN "
			+ InfoTables.DATABASE_MODES_TABLE
			+ " mode ON sch."
			+ OperationTable.KEY_OP_MODE
			+ " = mode."
			+ InfoTables.KEY_MODE_ROWID
			+ " LEFT OUTER JOIN "
			+ InfoTables.DATABASE_TAGS_TABLE
			+ " tag ON sch."
			+ OperationTable.KEY_OP_TAG
			+ " = tag."
			+ InfoTables.KEY_TAG_ROWID
			+ " LEFT OUTER JOIN "
			+ AccountTable.DATABASE_ACCOUNT_TABLE
			+ " acc ON sch."
			+ KEY_SCHEDULED_ACCOUNT_ID
			+ " = acc."
			+ AccountTable.KEY_ACCOUNT_ROWID;

	static final String[] SCHEDULED_OP_COLS_QUERY = {
			"sch." + KEY_SCHEDULED_ROWID,
			"tp." + InfoTables.KEY_THIRD_PARTY_NAME,
			"tag." + InfoTables.KEY_TAG_NAME,
			"mode." + InfoTables.KEY_MODE_NAME,
			"sch." + OperationTable.KEY_OP_SUM,
			"sch." + OperationTable.KEY_OP_DATE,
			"sch." + KEY_SCHEDULED_ACCOUNT_ID,
			"acc." + AccountTable.KEY_ACCOUNT_NAME,
			"sch." + OperationTable.KEY_OP_NOTES,
			"sch." + KEY_SCHEDULED_END_DATE,
			"sch." + KEY_SCHEDULED_PERIODICITY,
			"sch." + KEY_SCHEDULED_PERIODICITY_UNIT,
			"sch." + OperationTable.KEY_OP_TRANSFERT_ACC_ID,
			"sch." + OperationTable.KEY_OP_TRANSFERT_ACC_NAME };

	static void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_SCHEDULED_CREATE);
	}

	static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 5:
			db.execSQL(DATABASE_SCHEDULED_CREATE);
		case 6:
			upgradeFromV6(db, oldVersion, newVersion);
		case 11:
			upgradeFromV11(db, oldVersion, newVersion);
		}
	}

	public Cursor fetchAllScheduledOps(SQLiteDatabase db) {
		Cursor c = db.query(DATABASE_SCHEDULED_TABLE_JOINTURE,
				SCHEDULED_OP_COLS_QUERY, null, null, null, null,
				SCHEDULED_OP_ORDERING);
		if (null != c) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchScheduledOpsOfAccount(SQLiteDatabase db,
			final long accountId) {
		Cursor c = db.query(DATABASE_SCHEDULED_TABLE_JOINTURE,
				SCHEDULED_OP_COLS_QUERY, "sch." + KEY_SCHEDULED_ACCOUNT_ID
						+ " = " + accountId, null, null, null,
				SCHEDULED_OP_ORDERING);
		if (null != c) {
			c.moveToFirst();
		}
		return c;
	}

	static Cursor fetchOneScheduledOp(SQLiteDatabase db, long rowId) {
		Cursor c = db.query(DATABASE_SCHEDULED_TABLE_JOINTURE,
				SCHEDULED_OP_COLS_QUERY, "sch." + OperationTable.KEY_OP_ROWID
						+ " = " + rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	static long createScheduledOp(Context ctx, ScheduledOperation op) {
		ContentValues initialValues = new ContentValues();
		String key = op.mThirdParty;
		InfoTables.putKeyIdInThirdParties(ctx, key, initialValues);

		key = op.mTag;
		InfoTables.putKeyIdInTags(ctx, key, initialValues);

		key = op.mMode;
		InfoTables.putKeyIdInModes(ctx, key, initialValues);

		initialValues.put(OperationTable.KEY_OP_SUM, op.mSum);
		initialValues.put(OperationTable.KEY_OP_DATE, op.getDate());
		initialValues.put(OperationTable.KEY_OP_TRANSFERT_ACC_ID,
				op.mTransferAccountId);
		initialValues.put(OperationTable.KEY_OP_TRANSFERT_ACC_NAME,
				op.mTransSrcAccName);
		initialValues.put(OperationTable.KEY_OP_NOTES, op.mNotes);

		initialValues.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId);
		initialValues.put(KEY_SCHEDULED_END_DATE, op.getEndDate());
		initialValues.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity);
		initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit);
		Uri res = ctx.getContentResolver().insert(
				DbContentProvider.SCHEDULED_OP_URI, initialValues);
		return Long.parseLong(res.getLastPathSegment());
	}

	public static boolean updateScheduledOp(Context ctx, long rowId,
			ScheduledOperation op, final boolean isUpdatedFromOccurence) {
		ContentValues args = new ContentValues();

		String key = op.mThirdParty;
		InfoTables.putKeyIdInThirdParties(ctx, key, args);

		key = op.mTag;
		InfoTables.putKeyIdInTags(ctx, key, args);

		key = op.mMode;
		InfoTables.putKeyIdInModes(ctx, key, args);

		args.put(OperationTable.KEY_OP_SUM, op.mSum);
		args.put(OperationTable.KEY_OP_NOTES, op.mNotes);
		args.put(OperationTable.KEY_OP_TRANSFERT_ACC_ID, op.mTransferAccountId);
		args.put(OperationTable.KEY_OP_TRANSFERT_ACC_NAME, op.mTransSrcAccName);
		if (!isUpdatedFromOccurence) { // update from schedule editor
			args.put(KEY_SCHEDULED_END_DATE, op.getEndDate());
			args.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity);
			args.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit);
			args.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId);
			args.put(OperationTable.KEY_OP_DATE, op.getDate());
		}
		return ctx.getContentResolver().update(
				Uri.parse(DbContentProvider.SCHEDULED_OP_URI + "/" + rowId),
				args, null, null) > 0;
	}

	static boolean deleteScheduledOpOfAccount(Context ctx, final long accountId) {
		return ctx.getContentResolver().delete(
				DbContentProvider.SCHEDULED_JOINED_OP_URI,
				KEY_SCHEDULED_ACCOUNT_ID + "=?",
				new String[] { Long.toString(accountId) }) > 0;
	}

	public static boolean deleteScheduledOp(Context ctx, final long schOpId) {
		return ctx.getContentResolver().delete(
				Uri.parse(DbContentProvider.SCHEDULED_OP_URI + "/" + schOpId),
				null, null) > 0;
	}

	// UPGRADE FUNCTIONS
	private static void upgradeFromV11(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL(String.format(OperationTable.ADD_TRANSFERT_ID_COLUNM,
				DATABASE_SCHEDULED_TABLE));
		db.execSQL(String.format(OperationTable.ADD_TRANSFERT_NAME_COLUNM,
				DATABASE_SCHEDULED_TABLE));
	}

	private static void upgradeFromV6(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL("ALTER TABLE scheduled_ops RENAME TO scheduled_ops_old;");
		db.execSQL(DATABASE_SCHEDULED_CREATE);
		Cursor c = db.query("scheduled_ops_old", new String[] {
				KEY_SCHEDULED_ROWID, OperationTable.KEY_OP_THIRD_PARTY,
				OperationTable.KEY_OP_TAG, OperationTable.KEY_OP_SUM,
				KEY_SCHEDULED_ACCOUNT_ID, OperationTable.KEY_OP_MODE,
				OperationTable.KEY_OP_DATE, KEY_SCHEDULED_END_DATE,
				KEY_SCHEDULED_PERIODICITY, KEY_SCHEDULED_PERIODICITY_UNIT,
				OperationTable.KEY_OP_NOTES }, null, null, null, null, null);
		if (null != c && c.moveToFirst()) {
			do {
				ContentValues initialValues = new ContentValues();
				initialValues.put(OperationTable.KEY_OP_THIRD_PARTY, c.getInt(c
						.getColumnIndex(OperationTable.KEY_OP_THIRD_PARTY)));
				initialValues.put(OperationTable.KEY_OP_TAG,
						c.getInt(c.getColumnIndex(OperationTable.KEY_OP_TAG)));
				double d = c.getDouble(c
						.getColumnIndex(OperationTable.KEY_OP_SUM));
				long l = Math.round(d * 100);
				initialValues.put(OperationTable.KEY_OP_SUM, l);
				initialValues.put(KEY_SCHEDULED_ACCOUNT_ID,
						c.getInt(c.getColumnIndex(KEY_SCHEDULED_ACCOUNT_ID)));
				initialValues.put(OperationTable.KEY_OP_MODE,
						c.getInt(c.getColumnIndex(OperationTable.KEY_OP_MODE)));
				initialValues
						.put(OperationTable.KEY_OP_DATE, c.getLong(c
								.getColumnIndex(OperationTable.KEY_OP_DATE)));
				initialValues.put(KEY_SCHEDULED_END_DATE,
						c.getLong(c.getColumnIndex(KEY_SCHEDULED_END_DATE)));
				initialValues.put(KEY_SCHEDULED_PERIODICITY,
						c.getInt(c.getColumnIndex(KEY_SCHEDULED_PERIODICITY)));
				initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT, c.getInt(c
						.getColumnIndex(KEY_SCHEDULED_PERIODICITY_UNIT)));
				initialValues.put(OperationTable.KEY_OP_NOTES, c.getString(c
						.getColumnIndex(OperationTable.KEY_OP_NOTES)));
				long id = db.insert(DATABASE_SCHEDULED_TABLE, null,
						initialValues);
				initialValues = new ContentValues();
				initialValues.put(OperationTable.KEY_OP_SCHEDULED_ID, id);
				db.update(
						OperationTable.DATABASE_OPERATIONS_TABLE,
						initialValues,
						OperationTable.KEY_OP_SCHEDULED_ID
								+ "="
								+ c.getLong(c
										.getColumnIndex(KEY_SCHEDULED_ROWID)),
						null);

			} while (c.moveToNext());
			c.close();
		}
		db.execSQL("DROP TABLE scheduled_ops_old;");
	}
}
