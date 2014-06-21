package fr.geobert.radis.tools.scala

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.view.View
import android.widget.DatePicker
import fr.geobert.radis.tools.ToggleImageButton

trait RadisImplicits {
  implicit def func2OnDateSetListener[F](f: (DatePicker, Int, Int, Int) => F): DatePickerDialog.OnDateSetListener =
    new OnDateSetListener {
      private var alreadyFired: Int = -1

      override def onDateSet(p1: DatePicker, p2: Int, p3: Int, p4: Int): Unit = {
        // workaround known android bug
        if (alreadyFired % 2 == 0) {
          f(p1, p2, p3, p4)
        }
        alreadyFired += 1
      }
    }

  implicit def lazy2OnDateSetListener[F](f: => F): DatePickerDialog.OnDateSetListener =
    new OnDateSetListener {
      private var alreadyFired: Int = -1

      override def onDateSet(p1: DatePicker, p2: Int, p3: Int, p4: Int): Unit = {
        // workaround known android bug
        if (alreadyFired % 2 == 0) {
          f
        }
        alreadyFired += 1
      }
    }
}

object RadisImplicits {

  implicit class ViewWithToggle(view: ToggleImageButton) {
    def onCheckedChanged(action: (ToggleImageButton, Boolean) => Any) = {
      view.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangeListener {
        override def onCheckedChanged(p1: ToggleImageButton, p2: Boolean): Unit = {
          action(p1, p2)
        }
      })
    }
  }

  //  implicit class ViewWithOnClick(view: View) {
  //    def onClick(action: View => Any) = {
  //      view.setOnClickListener(new View.OnClickListener() {
  //        override def onClick(v: View) {
  //          action(v)
  //        }
  //      })
  //    }
  //  }


}
