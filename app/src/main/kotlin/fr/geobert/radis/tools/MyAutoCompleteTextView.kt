package fr.geobert.radis.tools

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.AbsListView
import android.widget.AutoCompleteTextView
import android.widget.ListPopupWindow
import fr.geobert.radis.ui.ConfigFragment

class MyAutoCompleteTextView : AutoCompleteTextView {
    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet,
                defStyle: Int) : super(context, attrs, defStyle) {
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun setReverseOrder(reverseOrder: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                val f = AutoCompleteTextView::class.java.getDeclaredField("mPopup")
                f.isAccessible = true
                val popupWindow = f.get(this) as ListPopupWindow
                val l = popupWindow.listView
                if (l != null) {
                    popupWindow.listView.isStackFromBottom = reverseOrder
                    popupWindow.listView.transcriptMode = if (reverseOrder)
                        AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
                    else
                        AbsListView.TRANSCRIPT_MODE_DISABLED
                }
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

    }

    override fun onFilterComplete(count: Int) {
        super.onFilterComplete(count)
        if (count > 0) {
            setReverseOrder(DBPrefsManager.getInstance(context).getBoolean(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD, true))
        }
    }

    override fun replaceText(text: CharSequence) {
        super.replaceText(text)
        val v = focusSearch(View.FOCUS_DOWN)
        v.requestFocus()
    }


}
