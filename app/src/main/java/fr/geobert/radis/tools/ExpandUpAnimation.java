package fr.geobert.radis.tools;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * This animation class is animating the expanding and reducing the size of a view.
 * The animation toggles between the Expand and Reduce, depending on the current state of the view
 * see https://github.com/Udinic/SmallExamples/blob/master/ExpandAnimationExample/src/com/udinic/expand_animation_example/ExpandAnimation.java
 *
 * @author Udinic
 */
public class ExpandUpAnimation extends Animation {
    public static Drawable mBg = null;
    private final int mHeightStart, mHeightEnd;
    private LinearLayout mAnimatedView;
    private LayoutParams mViewLayoutParams;
    private int mMarginStart, mMarginEnd;
    private boolean mIsVisibleAfter = false;
    private boolean mWasEndedAlready = false;

    public static void setChildrenVisibility(final LinearLayout view, final int visibility) {
        for (int i = 0; i < view.getChildCount(); i++) {
            View c = view.getChildAt(i);
            c.setVisibility(visibility);
        }
    }

    private void setTheVisibility() {
//        if (mIsVisibleAfter) {
//            mAnimatedView.setVisibility(View.GONE);
//        } else {
        mAnimatedView.setVisibility(View.VISIBLE);
//        }
    }

    /**
     * Initialize the animation
     *
     * @param view     The layout we want to animate
     * @param duration The duration of the animation, in ms
     * @param expand   expand or collapse animation
     */
    public ExpandUpAnimation(View view, int duration, boolean expand) {

        setDuration(duration);
        setFillEnabled(true);
        setFillAfter(true);
        mAnimatedView = (LinearLayout) view;
        mViewLayoutParams = (LayoutParams) view.getLayoutParams();

        // decide to show or hide the view
        mIsVisibleAfter = expand;

        mMarginStart = mViewLayoutParams.bottomMargin;
        mMarginEnd = (mMarginStart == 0 ? -37 : 0);

        mHeightStart = mAnimatedView.getHeight();
        mHeightEnd = (mHeightStart == 0 ? 37 : 0);

        setTheVisibility();
        if (mBg == null) {
            mBg = view.getBackground();
        }
        Tools.setViewBg(view, null);
        ExpandUpAnimation.setChildrenVisibility(mAnimatedView, View.INVISIBLE);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        if (interpolatedTime < 1.0f) {
            setTheVisibility();
            // Calculating the new bottom margin, and setting it
            mViewLayoutParams.bottomMargin = mMarginStart
                    + (int) ((mMarginEnd - mMarginStart) * interpolatedTime);

            // Invalidating the layout, making us seeing the changes we made
            mAnimatedView.requestLayout();

            // Making sure we didn't run the ending before (it happens!)
        } else if (!mWasEndedAlready) {
            mViewLayoutParams.bottomMargin = mMarginEnd;
            mAnimatedView.requestLayout();

            if (!mIsVisibleAfter) {
                mAnimatedView.setVisibility(View.GONE);
            } else {
                mAnimatedView.setVisibility(View.VISIBLE);
                ExpandUpAnimation.setChildrenVisibility(mAnimatedView, View.VISIBLE);
                Tools.setViewBg(mAnimatedView, mBg);
            }
            mWasEndedAlready = true;
        }
    }
}
