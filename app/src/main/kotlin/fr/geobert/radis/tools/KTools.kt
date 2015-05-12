package fr.geobert.radis.tools

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.os.Parcel
import hirondelle.date4j.DateTime
import java.text.ParseException
import java.util.LinkedList
import java.util.TimeZone

//public val UNIX_DATE: DateTime = DateTime(1970, 1, 1, 0, 0, 0, 0)
public val TIME_ZONE: TimeZone = TimeZone.getDefault()

public fun DateTime.plusMonth(nb: Int): DateTime = this.plus(0, nb, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
public fun DateTime.minusMonth(nb: Int): DateTime = this.minus(0, nb, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
public fun DateTime.plusYear(nb: Int): DateTime = this.plus(nb, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
public fun DateTime.minusYear(nb: Int): DateTime = this.minus(nb, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)


public fun fillContentValuesWith(args: ContentValues, key: String, value: Any) {
    when (value) {
        is String -> args.put(key, value)
        is Long -> args.put(key, value)
        is Int -> args.put(key, value)
    }
}

fun createBuilder(ctx: Context, titleId: Int, message: String): AlertDialog.Builder {
    val builder = AlertDialog.Builder(ctx)
    builder.setTitle(titleId).setMessage(message)
    return builder
}

fun createBuilder(ctx: Context, titleId: Int, messageId: Int): AlertDialog.Builder {
    val builder = AlertDialog.Builder(ctx)
    builder.setTitle(titleId).setMessage(messageId)
    return builder
}

public fun alert(ctx: Context, titleId: Int, message: String): Unit {
    val builder = createBuilder(ctx, titleId, message)
    builder.create().show()
}

public fun alert(ctx: Context, titleId: Int, messageId: Int): Unit {
    val builder = createBuilder(ctx, titleId, messageId)
    builder.create().show()
}

public fun alert(ctx: Context, titleId: Int, messageId: Int, btnTxtId: Int?,
                 f: ((DialogInterface, Int) -> Unit)? = null) {
    val builder = createBuilder(ctx, titleId, messageId)
    builder.setNeutralButton(btnTxtId ?: android.R.string.ok) { d, i -> if (f != null) f(d, i) }
}

public inline fun Cursor?.forEach(f: (it: Cursor) -> Unit): Unit {
    if (this != null) {
        if (moveToFirst()) {
            do {
                f(this)
            } while (moveToNext())
        }
    }
}

public inline fun <T> Cursor?.map(transform: (it: Cursor) -> T): MutableCollection<T> {
    return mapTo(LinkedList<T>(), transform)
}

public inline fun <T, C : MutableCollection<T>> Cursor?.mapTo(result: C, transform: (it: Cursor) -> T): C {
    return if (this == null) result else {
        if (moveToFirst())
            do {
                result.add(transform(this))
            } while (moveToNext())
        result
    }
}

public inline fun <T> Cursor?.mapAndClose(create: (it: Cursor) -> T): Collection<T> {
    try {
        return map(create)
    } finally {
        this?.close()
    }
}

public fun <T> Cursor?.getByFilter(filter: (it: Cursor) -> Boolean, create: (it: Cursor) -> T): T {
    var result: T = null
    this.forEach {
        if (filter(it)) {
            result = create(it)
            return@forEach
        }
    }
    return result
}

public fun String?.extractSumFromStr(): Long {
    val s = this?.replace('+', ' ')?.trim() as String
    val d: Double
    try {
        d = s.parseSum()
    } catch (e: ParseException) {
        d = 0.0
    }
    return Math.round(d * 100)
}

public fun Parcel.writeBoolean(b: Boolean) {
    this.writeByte(if (b) 1 else 0)
}

public fun Parcel.readBoolean(): Boolean = this.readByte() != 0.toByte()
public fun <T> List<T>.forMutableEach(): Unit {

}
