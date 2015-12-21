package fr.geobert.radis.ui

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.support.v7.widget.LinearLayoutManager


public class OnOperationScrollLoader(val operationListActivity: IOperationList) : RecyclerView.OnScrollListener() {// Load more operations while scrolling
    private var lastTotalCount = -1
    //    private var startOpDate: GregorianCalendar? = null
    private var lastTry = false
    private val linearLayoutManager: LinearLayoutManager = operationListActivity.getListLayoutManager()

    override fun onScrollStateChanged(recyclerView: RecyclerView?, i: Int) {
        super.onScrollStateChanged(recyclerView, i)
        // nothing to do
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val lastVisible = linearLayoutManager.findLastVisibleItemPosition()
        val totalCount = linearLayoutManager.itemCount

        Log.d("OnOperationScrollLoader", "lastVisible: $lastVisible/ totalCount: $totalCount/ lastTotal: $lastTotalCount")
        //        Log.d("OnOperationScrollLoader", "startOpDate " + startOpDate)
        val loadMore = totalCount - lastVisible <= 3

        if (loadMore) {
            if (lastTotalCount != totalCount || (lastTotalCount == 0 && !lastTry)) {
                if (lastTotalCount == 0) {
                    lastTry = true
                } else {
                    lastTry = false
                }
                operationListActivity.getMoreOperations(null)
            }
        }
    }

    //    public fun setStartDate(startOpDate: GregorianCalendar) {
    //        this.startOpDate = startOpDate
    //    }

}
