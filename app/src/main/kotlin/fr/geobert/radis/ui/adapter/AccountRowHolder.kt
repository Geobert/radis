package fr.geobert.radis.ui.adapter

import android.view.View
import android.widget.TextView
import fr.geobert.radis.R

class AccountRowHolder(val view: View) {
    var accountName: TextView = view.findViewById(android.R.id.text1) as TextView
    var accountSum: TextView = view.findViewById(R.id.account_sum) as TextView
    var balanceDate: TextView = view.findViewById(R.id.account_balance_at) as TextView
}
