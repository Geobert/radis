/**
 * From https://gist.github.com/akshaydashrath/9662072
 */

package fr.geobert.radis.tools

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import android.widget.ImageButton
import fr.geobert.radis.R

class ToggleImageButton : ImageButton, Checkable {
    private var isChecked: Boolean = false
    private var isBroadCasting: Boolean = false
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initAttr(context, attrs, defStyle)
    }

    private fun initAttr(context: Context, attrs: AttributeSet, defStyle: Int) {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.ToogleImageButton, defStyle, 0)
        val checked = a.getBoolean(R.styleable.ToogleImageButton_checked, false)
        setChecked(checked)
        a.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    override fun isChecked(): Boolean {
        return isChecked  //To change body of implemented methods use File | Settings | File Templates.
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked == checked) {
            return
        }
        isChecked = checked
        refreshDrawableState()
        if (isBroadCasting) {
            return
        }
        isBroadCasting = true
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener!!.onCheckedChanged(this, isChecked)
        }
        isBroadCasting = false
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked()) {
            View.mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }

    fun setOnCheckedChangeListener(onCheckedChangeListener: OnCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener
    }

    override fun toggle() {
        setChecked(!isChecked)
    }

    public override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("instanceState", super.onSaveInstanceState())
        bundle.putBoolean("state", isChecked)
        return bundle
    }

    override fun onRestoreInstanceState(s: Parcelable) {
        var state = s
        if (state is Bundle) {
            setChecked(state.getBoolean("state"))
            state = state.getParcelable<Parcelable>("instanceState")
        }
        super.onRestoreInstanceState(state)
    }

    interface OnCheckedChangeListener {

        fun onCheckedChanged(buttonView: ToggleImageButton, isChecked: Boolean)
    }

    companion object {

        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}
