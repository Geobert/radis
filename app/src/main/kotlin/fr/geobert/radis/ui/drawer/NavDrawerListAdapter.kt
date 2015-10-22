package fr.geobert.radis.ui.drawer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fr.geobert.radis.R
import java.util.*

class NavDrawerListAdapter(private val context: Context, private val navDrawerItems: ArrayList<NavDrawerItem>) : BaseAdapter() {

    private inner class ViewHolder internal constructor(v: View) {
        internal var icon: ImageView
        internal var header: TextView
        internal var title: TextView
        internal var headerCont: LinearLayout
        internal var itemCont: LinearLayout

        init {
            icon = v.findViewById(R.id.drawer_item_img) as ImageView
            title = v.findViewById(R.id.drawer_item_lbl) as TextView
            header = v.findViewById(R.id.drawer_header_lbl) as TextView
            headerCont = v.findViewById(R.id.drawer_header) as LinearLayout
            itemCont = v.findViewById(R.id.drawer_item) as LinearLayout
        }
    }

    override fun getCount(): Int {
        return navDrawerItems.size
    }

    override fun getItem(i: Int): Any {
        return navDrawerItems.get(i)
    }

    override fun isEnabled(position: Int): Boolean {
        return !(getItem(position) as NavDrawerItem).isHeader
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, v: View?, viewGroup: ViewGroup): View {
        var view = v
        val h: ViewHolder
        if (view == null) {
            val l = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = l.inflate(R.layout.drawer_item, null, false)
            h = ViewHolder(view)
            view?.tag = h
        } else {
            h = view.tag as ViewHolder
        }

        val item = navDrawerItems.get(i)
        if (item.isHeader) {
            h.itemCont.visibility = View.GONE
            h.headerCont.visibility = View.VISIBLE
            h.header.text = item.title
        } else {
            h.itemCont.visibility = View.VISIBLE
            h.headerCont.visibility = View.GONE
            h.title.text = item.title
            if (item.icon != 0) {
                h.icon.setImageResource(item.icon)
                h.icon.visibility = View.VISIBLE
            } else {
                h.icon.visibility = View.GONE
            }
        }
        return view as View
    }
}
