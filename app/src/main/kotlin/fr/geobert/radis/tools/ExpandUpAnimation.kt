package fr.geobert.radis.tools

import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams

/**
 * This animation class is animating the expanding and reducing the size of a view.
 * The animation toggles between the Expand and Reduce, depending on the current state of the view
 * see https://github.com/Udinic/SmallExamples/blob/master/ExpandAnimationExample/src/com/udinic/expand_animation_example/ExpandAnimation.java

 * @author Udinic
 */
class ExpandUpAnimation
/**
 * Initialize the animation

 * @param view     The layout we want to animate
 * *
 * @param duration The duration of the animation, in ms
 * *
 * @param expand   expand or collapse animation
 */
(view: View, duration: Int, expand: Boolean) : Animation() {
    private val mHeightStart: Int
    private val mHeightEnd: Int
    private val mAnimatedView: LinearLayout
    private val mViewLayoutParams: LayoutParams
    private val mMarginStart: Int
    private val mMarginEnd: Int
    private var mIsVisibleAfter = false
    private var mWasEndedAlready = false

    private fun setTheVisibility() {
        //        if (mIsVisibleAfter) {
        //            mAnimatedView.setVisibility(View.GONE);
        //        } else {
        mAnimatedView.visibility = View.VISIBLE
        //        }
    }

    init {

        setDuration(duration.toLong())
        isFillEnabled = true
        fillAfter = true
        mAnimatedView = view as LinearLayout
        mViewLayoutParams = view.layoutParams as LayoutParams

        // decide to show or hide the view
        mIsVisibleAfter = expand

        mMarginStart = mViewLayoutParams.bottomMargin
        mMarginEnd = (if (mMarginStart == 0) -37 else 0)

        mHeightStart = mAnimatedView.height
        mHeightEnd = (if (mHeightStart == 0) 37 else 0)

        setTheVisibility()
        if (mBg == null) {
            mBg = view.background
        }
        Tools.setViewBg(view, null)
        ExpandUpAnimation.setChildrenVisibility(mAnimatedView, View.INVISIBLE)
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        super.applyTransformation(interpolatedTime, t)
        if (interpolatedTime < 1.0f) {
            setTheVisibility()
            // Calculating the new bottom margin, and setting it
            mViewLayoutParams.bottomMargin = mMarginStart + ((mMarginEnd - mMarginStart) * interpolatedTime).toInt()

            // Invalidating the layout, making us seeing the changes we made
            mAnimatedView.requestLayout()

            // Making sure we didn't run the ending before (it happens!)
        } else if (!mWasEndedAlready) {
            mViewLayoutParams.bottomMargin = mMarginEnd
            mAnimatedView.requestLayout()

            if (!mIsVisibleAfter) {
                mAnimatedView.visibility = View.GONE
            } else {
                mAnimatedView.visibility = View.VISIBLE
                ExpandUpAnimation.setChildrenVisibility(mAnimatedView, View.VISIBLE)
                Tools.setViewBg(mAnimatedView, mBg)
            }
            mWasEndedAlready = true
        }
    }

    companion object {
        var mBg: Drawable? = null

        fun setChildrenVisibility(view: LinearLayout, visibility: Int) {
            for (i in 0..view.childCount - 1) {
                val c = view.getChildAt(i)
                c.visibility = visibility
            }
        }
    }
}
