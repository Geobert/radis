package fr.geobert.radis.tools;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;

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

	@Override
	protected void replaceText(CharSequence text) {
		super.replaceText(text);
		View v = focusSearch(FOCUS_DOWN);
		v.requestFocus();
	}
}
