package fr.geobert.radis.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AutoCompleteTextView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import fr.geobert.radis.ui.ConfigFragment;

import java.lang.reflect.Field;

public class MyAutoCompleteTextView extends AutoCompleteTextView {
    public MyAutoCompleteTextView(Context context) {
        super(context);
    }

    public MyAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyAutoCompleteTextView(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setReverseOrder(boolean reverseOrder) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Field f = AutoCompleteTextView.class.getDeclaredField("mPopup");
                f.setAccessible(true);
                ListPopupWindow popupWindow = (ListPopupWindow) f.get(this);
                ListView l = popupWindow.getListView();
                if (l != null) {
                    popupWindow.getListView().setStackFromBottom(reverseOrder);
                    popupWindow.getListView().setTranscriptMode(reverseOrder ?
                            AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL : AbsListView.TRANSCRIPT_MODE_DISABLED);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onFilterComplete(int count) {
        super.onFilterComplete(count);
        if (count > 0) {
            setReverseOrder(DBPrefsManager.getInstance(getContext()).
                    getBoolean(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD, true));
        }
    }

    @Override
    protected void replaceText(CharSequence text) {
        super.replaceText(text);
        View v = focusSearch(FOCUS_DOWN);
        v.requestFocus();
    }


}
