package fr.geobert.radis.db;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.acra.ErrorReporter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import fr.geobert.radis.Operation;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.ProjectionDateController;
import fr.geobert.radis.tools.Tools;

public class AccountTable {
	static final String DATABASE_ACCOUNT_TABLE = "accounts";
	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ACCOUNT_DESC = "account_desc";
	public static final String KEY_ACCOUNT_START_SUM = "account_start_sum";
	public static final String KEY_ACCOUNT_CUR_SUM = "account_current_sum";
	public static final String KEY_ACCOUNT_OP_SUM = "account_operations_sum";
	public static final String KEY_ACCOUNT_CURRENCY = "account_currency";
	public static final String KEY_ACCOUNT_ROWID = "_id";
	public static final String KEY_ACCOUNT_CUR_SUM_DATE = "account_current_sum_date";
	public static final String KEY_ACCOUNT_PROJECTION_MODE = "account_projection_mode";
	public static final String KEY_ACCOUNT_PROJECTION_DATE = "account_projection_date";
	public static final String[] ACCOUNT_COLS = { KEY_ACCOUNT_ROWID,
			KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
			KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_PROJECTION_MODE,
			KEY_ACCOUNT_PROJECTION_DATE };

	public static final String[] ACCOUNT_FULL_COLS = { KEY_ACCOUNT_ROWID,
			KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
			KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_PROJECTION_MODE,
			KEY_ACCOUNT_PROJECTION_DATE, KEY_ACCOUNT_DESC,
			KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_OP_SUM };

	private static final String DATABASE_ACCOUNT_CREATE_v7 = "create table "
			+ DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
			+ " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
			+ " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
			+ KEY_ACCOUNT_START_SUM + " integer not null, "
			+ KEY_ACCOUNT_OP_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM
			+ " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE
			+ " integer not null, " + KEY_ACCOUNT_CURRENCY + " text not null);";

	private static final String DATABASE_ACCOUNT_CREATE = "create table "
			+ DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
			+ " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
			+ " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
			+ KEY_ACCOUNT_START_SUM + " integer not null, "
			+ KEY_ACCOUNT_OP_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM
			+ " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE
			+ " integer not null, " + KEY_ACCOUNT_CURRENCY + " text not null, "
			+ KEY_ACCOUNT_PROJECTION_MODE + " integer not null, "
			+ KEY_ACCOUNT_PROJECTION_DATE + " string);";

	protected static final String ADD_CUR_DATE_COLUNM = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
			+ KEY_ACCOUNT_CUR_SUM_DATE + " integer not null DEFAULT 0";

	protected static final String ADD_PROJECTION_MODE_COLUNM = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
			+ KEY_ACCOUNT_PROJECTION_MODE + " integer not null DEFAULT 0";

	protected static final String ADD_PROJECTION_MODE_DATE = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
			+ KEY_ACCOUNT_PROJECTION_DATE + " string";
	private static int mProjectionMode;
	private static long mProjectionDate;

	static void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_ACCOUNT_CREATE);
	}

	static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 4:
			db.execSQL(ADD_CUR_DATE_COLUNM);
		case 6:
			upgradeFromV6(db, oldVersion, newVersion);
		case 9:
			upgradeFromV9(db, oldVersion, newVersion);
		default:
			upgradeDefault(db, oldVersion, newVersion);
		}
	}

	public static long createAccount(Context ctx, String name, String desc,
			long start_sum, String currency, int projectionMode,
			String projectionDate) throws ParseException {
		ContentValues values = new ContentValues();
		values.put(KEY_ACCOUNT_NAME, name);
		values.put(KEY_ACCOUNT_DESC, desc);
		values.put(KEY_ACCOUNT_START_SUM, start_sum);
		values.put(KEY_ACCOUNT_CURRENCY, currency);
		values.put(KEY_ACCOUNT_PROJECTION_MODE, projectionMode);
		values.put(KEY_ACCOUNT_PROJECTION_DATE, projectionDate);

		setCurrentSumAndDate(ctx, 0, values, start_sum, projectionMode,
				projectionDate);
		Uri res = ctx.getContentResolver().insert(
				DbContentProvider.ACCOUNT_URI, values);
		return Long.parseLong(res.getLastPathSegment());
	}

	public static boolean deleteAccount(Context ctx, final long accountId) {
		ScheduledOperationTable.deleteScheduledOpOfAccount(ctx, accountId);
		return ctx.getContentResolver().delete(
				Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
				null, null) > 0;
	}

	public static void consolidateSums(Context ctx, final long accountId) {
		if (0 != accountId) {
			ContentValues values = new ContentValues();
			Cursor account = fetchAccount(ctx, accountId);
			if (account != null) {
				if (account.moveToFirst()) {
					try {
						setCurrentSumAndDate(
								ctx,
								accountId,
								values,
								account.getLong(account
										.getColumnIndex(KEY_ACCOUNT_START_SUM)),
								account.getInt(account
										.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
								account.getString(account
										.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));

						ctx.getContentResolver().update(
								Uri.parse(DbContentProvider.ACCOUNT_URI + "/"
										+ accountId), values, null, null);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				account.close();
			}
		}
	}

	private static void setCurrentSumAndDate(Context ctx, long accountId,
			ContentValues values, final long start_sum,
			final int projectionMode, final String projectionDate)
			throws ParseException {
		long date = 0;
		long opSum = 0;
		switch (projectionMode) {
		case 0: {
			Log.d("Radis", "setCurrentSumAndDate mAccountId = " + accountId);
			if (accountId > 0) {
				Cursor allOps = OperationTable.fetchAllOps(ctx, accountId);
				if (null != allOps) {
					Log.d("Radis", "setCurrentSumAndDate allOps not null : "
							+ allOps.getCount());
					if (allOps.moveToFirst()) {
						Log.d("Radis",
								"setCurrentSumAndDate allOps moved to first");
						date = allOps.getLong(allOps
								.getColumnIndex(OperationTable.KEY_OP_DATE));
						opSum = OperationTable.computeSumFromCursor(allOps,
								accountId);
					}
					allOps.close();
				}
			}
		}
			break;
		case 1: {
			GregorianCalendar projDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(projDate);
			if (projDate.get(Calendar.DAY_OF_MONTH) >= Integer
					.parseInt(projectionDate)) {
				projDate.roll(Calendar.MONTH, 1);
			}
			projDate.set(Calendar.DAY_OF_MONTH,
					Integer.parseInt(projectionDate));
			projDate.roll(Calendar.DAY_OF_MONTH, 1); // roll for query
			Cursor op = OperationTable.fetchOpEarlierThan(ctx,
					projDate.getTimeInMillis(), 0, accountId);
			projDate.roll(Calendar.DAY_OF_MONTH, -1); // restore date after
														// query
			if (null != op) {
				if (op.moveToFirst()) {
					opSum = OperationTable.computeSumFromCursor(op, accountId);
				}
				op.close();
			}
			date = projDate.getTimeInMillis();
		}
			break;
		case 2: {
			GregorianCalendar projDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(projDate);
			projDate.setTime(Formater.getFullDateFormater().parse(
					projectionDate));
			projDate.roll(Calendar.DAY_OF_MONTH, 1); // roll for query
			Cursor op = OperationTable.fetchOpEarlierThan(ctx,
					projDate.getTimeInMillis(), 0, accountId);
			projDate.roll(Calendar.DAY_OF_MONTH, -1); // restore date after
			// query
			if (null != op) {
				if (op.moveToFirst()) {
					opSum = OperationTable.computeSumFromCursor(op, accountId);
				}
				op.close();
			}
			date = projDate.getTimeInMillis();
		}
			break;
		default:
			break;
		}
		values.put(KEY_ACCOUNT_OP_SUM, opSum);
		values.put(KEY_ACCOUNT_CUR_SUM, start_sum + opSum);
		values.put(KEY_ACCOUNT_CUR_SUM_DATE, date);
	}

	static Cursor fetchAccount(Context ctx, final long accountId) {
		return ctx.getContentResolver().query(
				Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
				ACCOUNT_FULL_COLS, null, null, null);
	}

	public static CursorLoader getAccountLoader(Context ctx,
			final long accountId) {
		return new CursorLoader(ctx, Uri.parse(DbContentProvider.ACCOUNT_URI
				+ "/" + accountId), AccountTable.ACCOUNT_FULL_COLS, null, null,
				null);
	}

	static Cursor fetchAllAccounts(SQLiteDatabase db) {
		Cursor c = db.query(DATABASE_ACCOUNT_TABLE, ACCOUNT_COLS, null, null,
				null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public static CursorLoader getAllAccountsLoader(Context ctx) {
		return new CursorLoader(ctx, DbContentProvider.ACCOUNT_URI,
				ACCOUNT_COLS, null, null, null);
	}

	static boolean checkNeedUpdateProjection(Context ctx, Operation op,
			final long accountId) {
		Cursor c = fetchAccount(ctx, accountId);
		initProjectionDate(c);
		c.close();
		final long opDate = op.getDate();
		final long projDate = mProjectionDate;
		boolean res = (opDate <= projDate)
				|| ((mProjectionMode == 0) && (opDate >= projDate))
				|| (projDate == 0);
		// Log.d("Radis", "checkNeedUpdateProjection : " + res);
		return res;
	}

	public static void initProjectionDate(Cursor c) {
		if (c != null && c.moveToFirst()) {
			mProjectionMode = c.getInt(8);
			switch (mProjectionMode) {
			case 0:
				mProjectionDate = c.getLong(c
						.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE));
				break;
			case 1: {
				GregorianCalendar projDate = new GregorianCalendar();
				Tools.clearTimeOfCalendar(projDate);
				projDate.set(Calendar.DAY_OF_MONTH,
						Integer.parseInt(c.getString(9)));
				GregorianCalendar today = new GregorianCalendar();
				Tools.clearTimeOfCalendar(today);
				if (projDate.compareTo(today) <= 0) {
					projDate.roll(Calendar.MONTH, 1);
				}
				mProjectionDate = projDate.getTimeInMillis();
			}
				break;
			case 2:
				try {
					Date projDate = Formater.getFullDateFormater().parse(
							c.getString(9));
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime(projDate);
					cal.set(Calendar.HOUR, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					mProjectionDate = cal.getTimeInMillis();
				} catch (ParseException e) {
					ErrorReporter.getInstance().handleSilentException(e);
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
	}

	public static boolean updateAccountProjectionDate(Context ctx,
			long accountId, ProjectionDateController projectionController)
			throws ParseException {
		if (projectionController.hasChanged()) {
			updateAccountProjectionDate(ctx, accountId,
					projectionController.getMode(),
					projectionController.getDate());
		}
		return true;
	}

	private static boolean updateAccountProjectionDate(Context ctx,
			long accountId, final int projMode, final String projDate)
			throws ParseException {
		Cursor account = fetchAccount(ctx, accountId);
		if (account.moveToFirst()) {
			ContentValues args = new ContentValues();
			long start_sum = account.getLong(account
					.getColumnIndex(KEY_ACCOUNT_START_SUM));
			account.close();
			args.put(KEY_ACCOUNT_PROJECTION_MODE, projMode);
			args.put(KEY_ACCOUNT_PROJECTION_DATE, projDate);

			setCurrentSumAndDate(ctx, accountId, args, start_sum, projMode,
					projDate);
			return ctx.getContentResolver().update(
					Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
					args, null, null) > 0;
		} else {
			return false;
		}
	}

	public static boolean updateAccountProjectionDate(Context ctx,
			long accountId) throws ParseException {
		Cursor c = fetchAccount(ctx, accountId);
		try {
			boolean res = true;
			if (null != c) {
				if (c.moveToFirst()) {
					res = updateAccountProjectionDate(
							ctx,
							accountId,
							c.getInt(c
									.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
							c.getString(c
									.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));
				}
				c.close();
			}
			return res;
		} catch (ParseException e) {
			c.close();
			throw e;
		}
	}

	public static boolean updateAccountCurrency(Context ctx, long accountId,
			String currency) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		return ctx.getContentResolver().update(
				Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
				args, null, null) > 0;
	}

	public static boolean updateAccount(Context ctx, long accountId,
			String name, String desc, long start_sum, String currency,
			ProjectionDateController projectionController)
			throws ParseException {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_NAME, name);
		args.put(KEY_ACCOUNT_DESC, desc);
		args.put(KEY_ACCOUNT_START_SUM, start_sum);
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		args.put(KEY_ACCOUNT_PROJECTION_MODE, projectionController.getMode());
		args.put(KEY_ACCOUNT_PROJECTION_DATE, projectionController.getDate());
		setCurrentSumAndDate(ctx, accountId, args, start_sum,
				projectionController.getMode(), projectionController.getDate());
		return ctx.getContentResolver().update(
				Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
				args, null, null) > 0;
	}

	public static void updateProjection(Context ctx, long accountId, long sumToAdd,
			long opDate) {
		ContentValues args = new ContentValues();
		if (mProjectionMode == 0 && (opDate > mProjectionDate || opDate == 0)) {
			if (opDate == 0) {
				Cursor op = OperationTable.fetchLastOp(ctx, accountId);
				if (null != op) {
					if (op.moveToFirst()) {
						args.put(KEY_ACCOUNT_CUR_SUM_DATE, op.getLong(op
								.getColumnIndex(OperationTable.KEY_OP_DATE)));
					}
					op.close();
				}
			} else {
				args.put(KEY_ACCOUNT_CUR_SUM_DATE, opDate);
			}
		}
		Cursor accountCursor = fetchAccount(ctx, accountId);
		if (accountCursor.moveToFirst()) {
			long opSum = accountCursor.getLong(accountCursor
					.getColumnIndex(KEY_ACCOUNT_OP_SUM));
			long startSum = accountCursor.getLong(accountCursor
					.getColumnIndex(KEY_ACCOUNT_START_SUM));
			args.put(KEY_ACCOUNT_OP_SUM, opSum + sumToAdd);
			args.put(KEY_ACCOUNT_CUR_SUM, startSum + opSum + sumToAdd);
			if (ctx.getContentResolver().update(
					Uri.parse(DbContentProvider.ACCOUNT_URI + "/" + accountId),
					args, null, null) > 0) {
				if (mProjectionMode == 0) {
					mProjectionDate = opDate;
				}
			}
		}
		accountCursor.close();
	}

	// UPGRADE FUNCTIONS
	private static void rawSetCurrentSumAndDate(SQLiteDatabase db,
			long accountId, ContentValues values, final long start_sum,
			final int projectionMode, final String projectionDate)
			throws ParseException {
		long date = 0;
		long opSum = 0;
		switch (projectionMode) {
		case 0: {
			Log.d("Radis", "setCurrentSumAndDate mAccountId = " + accountId);
			if (accountId > 0) {
				Cursor allOps = db.query(
						OperationTable.DATABASE_OP_TABLE_JOINTURE,
						OperationTable.OP_COLS_QUERY,
						OperationTable.RESTRICT_TO_ACCOUNT,
						new String[] { Long.toString(accountId),
								Long.toString(accountId) }, null, null,
						OperationTable.OP_ORDERING, null);
				if (null != allOps) {
					Log.d("Radis", "setCurrentSumAndDate allOps not null : "
							+ allOps.getCount());
					if (allOps.moveToFirst()) {
						Log.d("Radis",
								"setCurrentSumAndDate allOps moved to first");
						date = allOps.getLong(allOps
								.getColumnIndex(OperationTable.KEY_OP_DATE));
						opSum = OperationTable.computeSumFromCursor(allOps,
								accountId);
					}
					allOps.close();
				}
			}
		}
			break;
		case 1: {
			GregorianCalendar projDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(projDate);
			if (projDate.get(Calendar.DAY_OF_MONTH) >= Integer
					.parseInt(projectionDate)) {
				projDate.roll(Calendar.MONTH, 1);
			}
			projDate.set(Calendar.DAY_OF_MONTH,
					Integer.parseInt(projectionDate));
			projDate.roll(Calendar.DAY_OF_MONTH, 1); // roll for query
			Cursor op = db.query(
					OperationTable.DATABASE_OP_TABLE_JOINTURE,
					OperationTable.OP_COLS_QUERY,
					OperationTable.RESTRICT_TO_ACCOUNT + " and ops."
							+ OperationTable.KEY_OP_DATE + " < ?",
					new String[] { Long.toString(accountId),
							Long.toString(accountId),
							Long.toString(projDate.getTimeInMillis()) }, null,
					null, OperationTable.OP_ORDERING);
			projDate.roll(Calendar.DAY_OF_MONTH, -1); // restore date after
														// query
			if (null != op) {
				if (op.moveToFirst()) {
					opSum = OperationTable.computeSumFromCursor(op, accountId);
				}
				op.close();
			}
			date = projDate.getTimeInMillis();
		}
			break;
		case 2: {
			GregorianCalendar projDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(projDate);
			projDate.setTime(Formater.getFullDateFormater().parse(
					projectionDate));
			projDate.roll(Calendar.DAY_OF_MONTH, 1); // roll for query
			Cursor op = db.query(
					OperationTable.DATABASE_OP_TABLE_JOINTURE,
					OperationTable.OP_COLS_QUERY,
					OperationTable.RESTRICT_TO_ACCOUNT + " and ops."
							+ OperationTable.KEY_OP_DATE + " < ?",
					new String[] { Long.toString(accountId),
							Long.toString(accountId),
							Long.toString(projDate.getTimeInMillis()) }, null,
					null, OperationTable.OP_ORDERING);
			projDate.roll(Calendar.DAY_OF_MONTH, -1); // restore date after
			// query
			if (null != op) {
				if (op.moveToFirst()) {
					opSum = OperationTable.computeSumFromCursor(op, accountId);
				}
				op.close();
			}
			date = projDate.getTimeInMillis();
		}
			break;
		default:
			break;
		}
		values.put(KEY_ACCOUNT_OP_SUM, opSum);
		values.put(KEY_ACCOUNT_CUR_SUM, start_sum + opSum);
		values.put(KEY_ACCOUNT_CUR_SUM_DATE, date);
	}

	private static void rawConsolidateSums(SQLiteDatabase db, long accountId) {
		if (0 != accountId) {
			ContentValues values = new ContentValues();
			Cursor account = db
					.query(DATABASE_ACCOUNT_TABLE, ACCOUNT_FULL_COLS, "_id=?",
							new String[] { Long.toString(accountId) }, null,
							null, null);
			if (account != null) {
				if (account.moveToFirst()) {
					try {
						rawSetCurrentSumAndDate(
								db,
								accountId,
								values,
								account.getLong(account
										.getColumnIndex(KEY_ACCOUNT_START_SUM)),
								account.getInt(account
										.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
								account.getString(account
										.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));
						db.update(DATABASE_ACCOUNT_TABLE, values, "_id=?",
								new String[] { Long.toString(accountId) });
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				account.close();
			}
		}
	}

	private static void upgradeDefault(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		Cursor c = db.query(DATABASE_ACCOUNT_TABLE,
				new String[] { KEY_ACCOUNT_ROWID }, null, null, null, null,
				null);
		if (null != c) {
			if (c.moveToFirst()) {
				do {
					rawConsolidateSums(db, c.getLong(0));
				} while (c.moveToNext());
			}
			c.close();
		}
	}

	private static void upgradeFromV9(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL(ADD_PROJECTION_MODE_COLUNM);
		db.execSQL(ADD_PROJECTION_MODE_DATE);
		Cursor c = db.query(DATABASE_ACCOUNT_TABLE, new String[] {}, null,
				null, null, null, null);
		if (null != c) {
			if (c.moveToFirst()) {
				do {
					rawConsolidateSums(db,
							c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)));
				} while (c.moveToNext());
			}
			c.close();
		}
	}

	private static void upgradeFromV6(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL("DROP TRIGGER on_delete_third_party");
		db.execSQL("DROP TRIGGER on_delete_mode");
		db.execSQL("DROP TRIGGER on_delete_tag");
		db.execSQL("ALTER TABLE accounts RENAME TO accounts_old;");
		db.execSQL(DATABASE_ACCOUNT_CREATE_v7);
		Cursor c = db.query("accounts_old", new String[] { KEY_ACCOUNT_ROWID,
				KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
				KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_DESC, KEY_ACCOUNT_OP_SUM,
				KEY_ACCOUNT_START_SUM }, null, null, null, null, null);
		if (null != c && c.moveToFirst()) {
			do {
				ContentValues initialValues = new ContentValues();
				initialValues.put(KEY_ACCOUNT_NAME,
						c.getString(c.getColumnIndex(KEY_ACCOUNT_NAME)));
				initialValues.put(KEY_ACCOUNT_DESC,
						c.getString(c.getColumnIndex(KEY_ACCOUNT_DESC)));
				double d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_START_SUM));
				long l = Math.round(d * 100);
				initialValues.put(KEY_ACCOUNT_START_SUM, l);
				d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_OP_SUM));
				l = Math.round(d * 100);
				initialValues.put(KEY_ACCOUNT_OP_SUM, l);
				d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM));
				l = Math.round(d * 100);
				initialValues.put(KEY_ACCOUNT_CUR_SUM, l);
				initialValues.put(KEY_ACCOUNT_CURRENCY,
						c.getString(c.getColumnIndex(KEY_ACCOUNT_CURRENCY)));
				initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE,
						c.getLong(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE)));
				long id = db
						.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
				initialValues = new ContentValues();
				initialValues.put(OperationTable.KEY_OP_ACCOUNT_ID, id);
				db.update(
						OperationTable.DATABASE_OPERATIONS_TABLE,
						initialValues,
						OperationTable.KEY_OP_ACCOUNT_ID
								+ "="
								+ c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)),
						null);

			} while (c.moveToNext());
			c.close();
			db.execSQL("DROP TABLE accounts_old;");
		}
	}

}