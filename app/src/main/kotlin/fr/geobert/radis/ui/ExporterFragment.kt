package fr.geobert.radis.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.R
import kotlin.properties.Delegates

public class ExporterFragment : BaseFragment() {
    private var colsToExportList: RecyclerView by Delegates.notNull()
    private var filenameEdt: EditText by Delegates.notNull()
    private var exportBtn: Button by Delegates.notNull()
    private var moveUpBtn: ImageButton by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val c = inflater.inflate(R.layout.exporter_fragment, container, false)
        return c
    }

    override fun setupIcon() {
        throw UnsupportedOperationException()
    }

    override fun updateDisplay(intent: Intent?) {
        throw UnsupportedOperationException()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        throw UnsupportedOperationException()
    }
}
