package fr.geobert.radis.ui;

import android.util.Log;
import android.widget.AbsListView;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class OnOperationScrollLoader implements AbsListView.OnScrollListener {// Load more operations while scrolling
    private int lastTotalCount = -1;
    private GregorianCalendar startOpDate;
    private IOperationList operationListActivity;
    private boolean lastTry = false;

    public OnOperationScrollLoader(IOperationList operationList) {
        operationListActivity = operationList;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        // nothing to do
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {
        Log.d("OnOperationScrollLoader", "firstVisible " + firstVisible + "/ visibleCount : " + visibleCount + "/ totalCount : " + totalCount);
        Log.d("OnOperationScrollLoader", "startOpDate " + startOpDate + "/ lastTotalCount: " + lastTotalCount);
        boolean loadMore = firstVisible + visibleCount >= totalCount - 2;

        if (loadMore) {
            if (startOpDate != null && lastTotalCount != totalCount) {
                lastTotalCount = totalCount;
                startOpDate.add(Calendar.MONTH, -1);
                operationListActivity.getMoreOperations(startOpDate);
            } else {
                if (lastTotalCount != totalCount || (lastTotalCount == 0 && !lastTry)) {
                    if (lastTotalCount == 0) {
                        lastTry = true;
                    } else {
                        lastTry = false;
                    }
                    operationListActivity.getMoreOperations(null);
                }
            }
        }
    }

    public void setStartDate(GregorianCalendar startOpDate) {
        this.startOpDate = startOpDate;
    }

}
