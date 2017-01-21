package jp.gr.aqua.vjap


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar

class ReversedSeekBar(context: Context, attrs: AttributeSet) : SeekBar(context, attrs) {

    override fun onDraw(c: Canvas) {
        val px = this.width / 2.0f
        val py = this.height / 2.0f
        c.scale(-1f, 1f, px, py)
        super.onDraw(c)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        event.setLocation(this.width - event.x, event.y)
        return super.onTouchEvent(event)
    }
}