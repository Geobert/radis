package fr.geobert.radis.ui.drawer;

public class NavDrawerItem {
    private String title;
    private int icon;
    private boolean isHeader;

    public NavDrawerItem(String title) {
        this.title = title;
        this.isHeader = true;
    }

    public NavDrawerItem(String title, int icon) {
        this.title = title;
        this.icon = icon;
        this.isHeader = false;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public String getTitle() {
        return title;
    }

    public int getIcon() {
        return icon;
    }
}
