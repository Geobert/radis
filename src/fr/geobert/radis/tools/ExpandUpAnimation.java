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
    private LinearLayout mAnimatedView;
    private LayoutParams mViewLayoutParams;
    private int mMarginStart, mMarginEnd;
    private boolean mIsVisibleAfter = false;
    private boolean mWasEndedAlready = false;

    private void setChildrenVisibility(final int visibility) {
        for (int i = 0; i < mAnimatedView.getChildCount(); i++) {
            View c = mAnimatedView.getChildAt(i);
            c.setVisibility(visibility);
        }
    }

    /**
     * Initialize the animation
     *
     * @param view     The layout we want to animate
     * @param duration The duration of the animation, in ms
     */
    public ExpandUpAnimation(View view, int duration) {

        setDuration(duration);
        //setFillAfter(true);
        mAnimatedView = (LinearLayout) view;
        mViewLayoutParams = (LayoutParams) view.getLayoutParams();

        // decide to show or hide the view
        mIsVisibleAfter = (view.getVisibility() == View.VISIBLE);

        mMarginStart = mViewLayoutParams.bottomMargin;
        mMarginEnd = (mMarginStart == 0 ? 0 - view.getHeight() : 0);

        view.setVisibility(View.INVISIBLE);
        if (mBg == null) {
            mBg = view.getBackground();
        }
        Tools.setViewBg(view, null);
        setChildrenVisibility(View.INVISIBLE);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        if (interpolatedTime < 1.0f) {
            mAnimatedView.setVisibility(View.INVISIBLE);
            // Calculating the new bottom margin, and setting it
            mViewLayoutParams.bottomMargin = mMarginStart
                    + (int) ((mMarginEnd - mMarginStart) * interpolatedTime);

            // Invalidating the layout, making us seeing the changes we made
            mAnimatedView.requestLayout();

            // Making sure we didn't run the ending before (it happens!)
        } else if (!mWasEndedAlready) {
            mViewLayoutParams.bottomMargin = mMarginEnd;
            mAnimatedView.requestLayout();

            if (mIsVisibleAfter) {
                mAnimatedView.setVisibility(View.GONE);
            } else {
                mAnimatedView.setVisibility(View.VISIBLE);
                setChildrenVisibility(View.VISIBLE);
                Tools.setViewBg(mAnimatedView, mBg);
            }
            mWasEndedAlready = true;
        }
    }
}