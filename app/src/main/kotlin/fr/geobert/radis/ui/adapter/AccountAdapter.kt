package fr.geobert.radis.ui.adapter

import android.database.Cursor
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.tools.formatDate
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.tools.map
import java.util.Date
import java.util.LinkedList
import kotlin.properties.Delegates

public class AccountAdapter(val activity: FragmentActivity) : BaseAdapter(), Iterable<Account> {

    private var accountsList: MutableList<Account> = LinkedList()
    private val redColor: Int by Delegates.lazy { activity.getResources().getColor(R.color.op_alert) }
    private val greenColor: Int by Delegates.lazy { activity.getResources().getColor(R.color.positiveSum) }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup): View? {
        val h: AccountRowHolder = if (p1 == null) {
            val v = activity.getLayoutInflater().inflate(R.layout.account_row, null)
            val t = AccountRowHolder(v)
            v.setTag(t)
            t
        } else {
            p1.getTag() as AccountRowHolder
        }

        val account = accountsList.get(p0)
        val currencySymbol = account.getCurrencySymbol(activity)
        h.accountName.setText(account.name)
        val stringBuilder = StringBuilder()
        val sum = account.curSum
        if (sum < 0) {
            h.accountSum.setTextColor(redColor)
        } else {
            h.accountSum.setTextColor(greenColor)
        }
        stringBuilder.append((sum.toDouble() / 100.0).formatSum())
        stringBuilder.append(' ').append(currencySymbol)
        h.accountSum.setText(stringBuilder)

        val dateLong = account.curSumDate?.getTime() ?: 0
        stringBuilder.setLength(0)
        if (dateLong > 0) {
            stringBuilder.append(activity.getString(R.string.balance_at).format(Date(dateLong).formatDate()))
        } else {
            stringBuilder.append(activity.getString(R.string.current_sum))
        }
        h.balanceDate.setText(stringBuilder)
        return h.view
    }

    fun getAccount(p: Int): Account {
        return accountsList.get(p)
    }

    override fun getItem(p0: Int): Any? {
        return accountsList.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        Log.d(TAG, "getItemId:$p0")
        return accountsList.get(p0).id
    }

    override fun getCount(): Int {
        return accountsList.count()
    }

    public fun swapCursor(c: Cursor) {
        accountsList = c.map { Account(c) }
        notifyDataSetChanged()
    }

    override fun iterator(): Iterator<Account> {
        return accountsList.iterator()
    }

    override fun isEmpty(): Boolean {
        return getCount() == 0
    }

    companion object {
        val TAG = "AccountAdapter"
    }
}

