package fr.geobert.radis.ui.adapter

import android.database.Cursor
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.tools.formatDate
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.tools.map
import java.util.*

public class AccountAdapter(val activity: FragmentActivity) : BaseAdapter(), Iterable<Account> {

    private var accountsList: MutableList<Account> = LinkedList()
    private val redColor: Int by lazy(LazyThreadSafetyMode.NONE) { activity.resources.getColor(R.color.op_alert) }
    private val greenColor: Int by lazy(LazyThreadSafetyMode.NONE) { activity.resources.getColor(R.color.positiveSum) }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup): View? {
        val h: AccountRowHolder = if (p1 == null) {
            val v = activity.layoutInflater.inflate(R.layout.account_row, p2, false)
            val t = AccountRowHolder(v)
            v.tag = t
            t
        } else {
            p1.tag as AccountRowHolder
        }

        val account = accountsList.get(p0)
        val currencySymbol = account.getCurrencySymbol(activity)
        h.accountName.text = account.name
        val stringBuilder = StringBuilder()
        val sum = account.curSum
        if (sum < 0) {
            h.accountSum.setTextColor(redColor)
        } else {
            h.accountSum.setTextColor(greenColor)
        }
        stringBuilder.append((sum.toDouble() / 100.0).formatSum())
        stringBuilder.append(' ').append(currencySymbol)
        h.accountSum.text = stringBuilder

        val dateLong = account.curSumDate?.time ?: 0
        stringBuilder.setLength(0)
        if (dateLong > 0) {
            stringBuilder.append(activity.getString(R.string.balance_at).format(Date(dateLong).formatDate()))
        } else {
            stringBuilder.append(activity.getString(R.string.current_sum))
        }
        h.balanceDate.text = stringBuilder
        return h.view
    }

    fun getAccount(p: Int): Account {
        return accountsList.get(p)
    }

    override fun getItem(p0: Int): Any? {
        return accountsList.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return if (p0 < count) accountsList.get(p0).id else 0
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
        return count == 0
    }

    companion object {
        val TAG = "AccountAdapter"
    }
}

