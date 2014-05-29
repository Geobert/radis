package fr.geobert.radis.tools

import android.content.ContentValues
import java.lang.Long

object STools {

  def fillContentValues(args: ContentValues, key: String, value: Any) = {
    value match {
      case s: String => args.put(key, s)
      case l: Long => args.put(key, l)
      case i: Integer => args.put(key, i)
    }
  }
}