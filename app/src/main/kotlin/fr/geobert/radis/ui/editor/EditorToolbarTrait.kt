package fr.geobert.radis.ui.editor

import android.support.v7.widget.Toolbar
import android.view.View
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R

public interface EditorToolbarTrait : Toolbar.OnMenuItemClickListener {
    fun initToolbar(activity: BaseActivity) {
        activity.setIcon(R.drawable.cancel_48)
        activity.setIconOnClick(View.OnClickListener { activity.onBackPressed() })
        activity.setMenu(R.menu.confirm_cancel_menu)
        activity.mToolbar.setOnMenuItemClickListener(this)
    }
}

