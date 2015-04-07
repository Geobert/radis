package fr.geobert.radis.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.ui.editor.EditorToolbarTrait

public class ConfigEditor : BaseActivity(), EditorToolbarTrait, SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(p0: SharedPreferences, p1: String) {
        val f = getSupportFragmentManager().findFragmentById(R.id.main_edit_pane)
        if (f != null && f is ConfigFragment) {
            f.onSharedPreferenceChanged(p0, p1)
        }
    }

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
