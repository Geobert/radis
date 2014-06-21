package fr.geobert.radis.tools.scala

import android.content.{DialogInterface, Context, ContentValues}
import java.lang.Long

import android.view.View
import fr.geobert.radis.R
import org.scaloid.common.AlertDialogBuilder

object STools {

  def fillContentValues(args: ContentValues, key: String, value: Any) = {
    value match {
      case s: String => args.put(key, s)
      case l: Long => args.put(key, l)
      case i: Integer => args.put(key, i)
    }
  }

  def popMessage(msg: CharSequence, btnTxt: CharSequence, title: CharSequence = "",
                 f: Option[(DialogInterface, Int) => Unit] = None)(implicit ctx: Context) = {
    new AlertDialogBuilder(title, msg) {
      neutralButton(btnTxt, f.getOrElse((_: DialogInterface, _: Int) => {}))
    }.show()
  }

  def popError(msg: CharSequence, f: Option[(DialogInterface, Int) => Unit] = None)(implicit ctx: Context) = {
    popMessage(msg, ctx.getString(R.string.ok), ctx.getString(R.string.error), f)
  }
}