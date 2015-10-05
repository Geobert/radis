package fr.geobert.radis.tools

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout.LayoutParams

/**
 * This animation class is animating the expanding and reducing the size of a view.
 * The animation toggles between the Expand and Reduce, depending on the current state of the view
 * see https://github.com/Udinic/SmallExamples/blob/master/ExpandAnimationExample/src/com/udinic/expand_animation_example/ExpandAnimation.java

 * @author Udinic
 */
class ExpandAnimation
/**
 * Initialize the animation

 * @param view     The layout we want to animate
 * *
 * @param duration The duration of the animation, in ms
 */
(private val mAnimatedView: View, duration: Int, expand: Boolean) : Animation() {
    private val mViewLayoutParams: LayoutParams
    private var mMarginStart: Int = 0
    private val mMarginEnd: Int
    private var mIsVisibleAfter = false
    private var mWasEndedAlready = false

    init {

        setDuration(duration.toLong())
        isFillEnabled = true
        fillAfter = true
        mViewLayoutParams = mAnimatedView.layoutParams as LayoutParams

        // decide to show or hide the view
        mIsVisibleAfter = expand
        if (expand) {
            mMarginStart = -84
        } else {
            mMarginStart = 0
        }

        mMarginEnd = (if (mMarginStart == 0) -84 else 0)

        mAnimatedView.visibility = View.VISIBLE

        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                if (!mIsVisibleAfter) {
                    mViewLayoutParams.bottomMargin = -mAnimatedView.getMeasuredHeight() // -55
                    mAnimatedView.visibility = View.GONE
                } else {
                    mViewLayoutParams.bottomMargin = 0
                    mAnimatedView.visibility = View.VISIBLE
                }
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        super.applyTransformation(interpolatedTime, t)
        if (interpolatedTime < 1.0f) {

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
            }
            mWasEndedAlready = true
        }
    }
}
