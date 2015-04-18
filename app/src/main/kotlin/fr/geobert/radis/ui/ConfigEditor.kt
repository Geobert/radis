package fr.geobert.radis.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.ui.editor.EditorToolbarTrait

public class ConfigEditor : BaseActivity(), EditorToolbarTrait {

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
        // nothing to do
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.config_activity)
        initToolbar(this)
        mToolbar.getMenu().clear()
        setIcon(R.drawable.ok_48)
        setTitle(R.string.preferences)
    }
}
