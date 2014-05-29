package fr.geobert.radis.tools

import android.view.View

object ViewUtils {

  implicit class ViewWithOnClick(view: View) {
    def onClick(action: View => Any) = {
      view.setOnClickListener(new View.OnClickListener() {
        override def onClick(v: View) {
          action(v)
        }
      })
    }
  }

}
