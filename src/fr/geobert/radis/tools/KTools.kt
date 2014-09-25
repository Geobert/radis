package fr.geobert.radis.tools

import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.app.AlertDialog
import android.database.Cursor
import java.util.LinkedList

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
    builder.setNeutralButton(btnTxtId ?: android.R.string.ok) {(d, i) -> if (f != null) f(d, i) }
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

public inline fun <T> Cursor?.map(transform: (it: Cursor) -> T): Collection<T> {
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