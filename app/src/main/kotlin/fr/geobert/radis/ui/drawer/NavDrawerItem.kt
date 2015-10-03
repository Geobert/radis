package fr.geobert.radis.ui.drawer

class NavDrawerItem(val title: String, val icon: Int) {
    var isHeader: Boolean = false
        private set

    constructor(title: String) : this(title, 0) {
        this.isHeader = true
    }
}
