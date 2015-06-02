package fr.geobert.radis.ui.adapter

import android.view.View
import android.widget.TextView
import fr.geobert.radis.R

public class AccountRowHolder(val view: View) {
    public var accountName: TextView = view.findViewById(android.R.id.text1) as TextView
    public var accountSum: TextView = view.findViewById(R.id.account_sum) as TextView
    public var balanceDate: TextView = view.findViewById(R.id.account_balance_at) as TextView
}
