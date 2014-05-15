package fr.geobert.radis.ui.drawer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import fr.geobert.radis.R;

import java.util.ArrayList;

public class NavDrawerListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItems;

    private class ViewHolder {
        ImageView icon;
        TextView header;
        TextView title;
        LinearLayout headerCont;
        LinearLayout itemCont;

        ViewHolder(View v) {
            icon = (ImageView) v.findViewById(R.id.drawer_item_img);
            title = (TextView) v.findViewById(R.id.drawer_item_lbl);
            header = (TextView) v.findViewById(R.id.drawer_header_lbl);
            headerCont = (LinearLayout) v.findViewById(R.id.drawer_header);
            itemCont = (LinearLayout) v.findViewById(R.id.drawer_item);
        }
    }

    public NavDrawerListAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItems) {
        super();
        this.context = context;
        this.navDrawerItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int i) {
        return navDrawerItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder h;
        if (view == null) {
            LayoutInflater l = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = l.inflate(R.layout.drawer_item, null);
            h = new ViewHolder(view);
            view.setTag(h);
        } else {
            h = (ViewHolder) view.getTag();
        }

        NavDrawerItem item = navDrawerItems.get(i);
        if (item.isHeader()) {
            h.itemCont.setVisibility(View.GONE);
            h.headerCont.setVisibility(View.VISIBLE);
            h.header.setText(item.getTitle());
        } else {
            h.itemCont.setVisibility(View.VISIBLE);
            h.headerCont.setVisibility(View.GONE);
            h.title.setText(item.getTitle());
            if (item.getIcon() != 0) {
                h.icon.setImageResource(item.getIcon());
            }
        }
        return view;
    }
}
