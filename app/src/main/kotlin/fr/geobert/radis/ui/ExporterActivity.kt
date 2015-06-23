package fr.geobert.radis.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.DialogFragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.ExportCol
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.filenameTimetag
import fr.geobert.radis.tools.formatDate
import fr.geobert.radis.tools.map
import fr.geobert.radis.ui.adapter.ExportColsAdapter
import fr.geobert.radis.ui.editor.EditorToolbarTrait
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import kotlin.properties.Delegates

public class ExporterActivity : BaseActivity(), EditorToolbarTrait {

    private var colsToExportList: RecyclerView by Delegates.notNull()
    private var moveUpBtn: ImageButton by Delegates.notNull()
    private var moveDownBtn: ImageButton by Delegates.notNull()
    private var moveBtnCont: LinearLayout by Delegates.notNull()
    private var adapter: ExportColsAdapter by Delegates.notNull()
    private var oneFilePerAccountChk: CheckBox by Delegates.notNull()
    private var isExporting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.exporter_fragment)
        setTitle(R.string.export_csv)
        initToolbar(this)
        colsToExportList = findViewById(R.id.cols_to_export_list) as RecyclerView
        moveUpBtn = findViewById(R.id.move_up) as ImageButton
        moveDownBtn = findViewById(R.id.move_down) as ImageButton
        moveBtnCont = findViewById(R.id.move_buttons_cont) as LinearLayout
        oneFilePerAccountChk = findViewById(R.id.one_file_per_account_chk) as CheckBox

        val strings = getResources().getStringArray(R.array.csv_header_item)
        adapter = ExportColsAdapter(this, strings.map { ExportCol(it) })
        colsToExportList.setLayoutManager(LinearLayoutManager(this))
        colsToExportList.setItemAnimator(DefaultItemAnimator())
        colsToExportList.setAdapter(adapter)

        moveUpBtn.setOnClickListener { moveItem(true) }
        moveDownBtn.setOnClickListener { moveItem(false) }

        if (savedInstanceState != null) onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseActivity>.onSaveInstanceState(outState)
        outState.putBoolean("oneFilePerAccount", oneFilePerAccountChk.isChecked())
        outState.putParcelableArray("columns", adapter.toArray())
        outState.putInt("selected", adapter.selectedPos)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super<BaseActivity>.onRestoreInstanceState(savedInstanceState)
        oneFilePerAccountChk.setChecked(savedInstanceState.getBoolean("oneFilePerAccount"))
        adapter.fromArray(savedInstanceState.getParcelableArray("columns"))
        adapter.selectedPos = savedInstanceState.getInt("selected")
        if (adapter.selectedPos > -1) {
            adapter.notifyItemChanged(adapter.selectedPos)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (isExporting) return true
        isExporting = true
        object : AsyncTask<Void, Void, Void>() {
            override fun onPreExecute() {
                super.onPreExecute()
                startProgress()
            }

            override fun doInBackground(vararg p0: Void?): Void? {
                exportToCSV()
                return null
            }

            override fun onPostExecute(result: Void?) {
                super.onPostExecute(result)
                stopProgress()
                isExporting = false
            }
        }.execute()
        return true
    }

    // workaround https://youtrack.jetbrains.com/issue/KT-8152
    private fun startProgress() = showProgress()

    // workaround https://youtrack.jetbrains.com/issue/KT-8152
    private fun stopProgress() = hideProgress()

    private fun exportToCSV() {
        try {
            val sd = Environment.getExternalStorageDirectory()
            if (sd.canWrite()) {
                val backupDBDir = "/radis/"
                val backupDir = File(sd, backupDBDir)
                backupDir.mkdirs()
                val path = processExportCSV(sd, backupDBDir)
                if (path != null) {
                    val p = if (path.length() > 0) path else backupDir.getAbsolutePath()
                    ExportCSVSucceedDialog.newInstance(p, path.length() > 0).show(getSupportFragmentManager(), "csv_export")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isExporting = false
        }
    }

    private fun moveItem(up: Boolean) {
        adapter.moveItem(if (up) -1 else 1)
    }

    private fun processExportCSV(sd: File, backupDBDir: String): String? {
        var writer: BufferedWriter? = null
        val accountsCursor = AccountTable.fetchAllAccounts(this)
        val accounts = accountsCursor.map { Account(it) }
        accountsCursor.close()
        val timeTag = filenameTimetag()
        val filename = "${timeTag}_radis.csv"
        val file = File(sd, "$backupDBDir$filename")
        if (!oneFilePerAccountChk.isChecked()) {
            writer = BufferedWriter(FileWriter(file))
            val stringbuilder = adapter.fold(StringBuilder(), { sb, col ->
                if (col.toExport) {
                    sb.append("\"${col.label}\",")
                }
                sb
            })
            if (stringbuilder.length() > 0) {
                stringbuilder.replace(stringbuilder.length() - 1, stringbuilder.length(), "\n")
                writer.write(stringbuilder.toString())
            } else {
                Tools.popError(this, getString(R.string.no_column_selected), null)
                return null
            }
        }
        accounts.forEach {
            exportOneAccount(it, sd, backupDBDir, writer, timeTag)
        }
        writer?.close()

        return if (writer != null) {
            file.getAbsolutePath()
        } else {
            ""
        }
    }

    private fun exportOneAccount(account: Account, sd: File, backupDBDir: String, writer: Writer?, timeTag: String) {
        val opCursor = OperationTable.fetchAllOps(this, account.id)
        val operations = opCursor.map { Operation(it) }
        val accountName = account.name
        val w = if (writer != null) writer else {
            val filename = "${timeTag}_${accountName}_radis.csv"
            BufferedWriter(FileWriter(File(sd, "$backupDBDir$filename")))
        }

        val colsLabel = getResources().getStringArray(R.array.csv_header_item)
        operations.forEach {
            val stringbuilder = adapter.fold(StringBuilder(), { sb, col ->
                if (col.toExport) {
                    val str = when (col.label) {
                        colsLabel[0] -> accountName
                        colsLabel[1] -> it.getDate().formatDate()
                        colsLabel[2] -> it.mThirdParty
                        colsLabel[3] -> it.getSumStr()
                        colsLabel[4] -> it.mTag
                        colsLabel[5] -> it.mMode
                        colsLabel[6] -> it.mNotes
                        else -> ""
                    }
                    sb.append("\"${str}\",")
                }
                sb
            })
            if (stringbuilder.length() > 0) {
                stringbuilder.replace(stringbuilder.length() - 1, stringbuilder.length(), "\n")
            }
            w.write(stringbuilder.toString())
        }
        if (writer == null) {
            w.close()
        }
    }

    public class ExportCSVSucceedDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreateDialog(savedInstanceState)
            val builder = AlertDialog.Builder(getActivity())
            val path = getArguments().getString("path")
            val share = getArguments().getBoolean("share")
            val msg = getString(R.string.export_csv_success).format(path.substring(0, path.lastIndexOf(File.separator) + 1))
            builder.setTitle(R.string.export_csv).setMessage(msg).setCancelable(false).
                    setNegativeButton(R.string.ok, { d, i ->
                        getActivity().finish()
                    })
            if (share) {
                builder.setPositiveButton(R.string.open, { d, i ->
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.setType("text/plain")
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$path"))
                    startActivity(Intent.createChooser(intent, getString(R.string.open)))
                })
            }
            return builder.create()
        }

        companion object {
            public fun newInstance(path: String, share: Boolean): ExportCSVSucceedDialog {
                val f = ExportCSVSucceedDialog()
                val args = Bundle()
                args.putString("path", path)
                args.putBoolean("share", share)
                f.setArguments(args)
                return f
            }
        }
    }

    companion object {
        public fun callMe(ctx: Context) {
            val i = Intent(ctx, javaClass<ExporterActivity>())
            ctx.startActivity(i)
        }
    }
}
