package jp.gr.aqua.vjap

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.Toast

class VTextView : View {

    private var currentIndex = 0
    private var layout: VerticalLayout? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        try {
            // ここで落ちると親を巻き込むので対策しておく
            layout?.textDraw(canvas, currentIndex)
        }catch(ex:Exception){
            Toast.makeText(context,"Error", Toast.LENGTH_LONG).show()
            ex.printStackTrace()
        }
    }

    fun setLayout(layout: VerticalLayout, page: Int) {
        this.layout = layout
        this.currentIndex = page
    }

//    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
//    private val doubleTapTimeout by lazy { getDoubleTapTimeout() }
//    var lastTapTime = 0L
//    var lastTouchX = 0F
//    var lastTouchY = 0F
//
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        if ( event!= null ){
//            val action = event.action
//            if ( action == MotionEvent.ACTION_DOWN ){
//                val now = System.currentTimeMillis()
//                val xdiff = Math.abs(event.x - lastTouchX)
//                val ydiff = Math.abs(event.y - lastTouchY)
//                if ( xdiff < touchSlop && ydiff < touchSlop && ( now - lastTapTime ) < doubleTapTimeout ){
//                    val theChar = layout?.getTouchedChar(currentIndex , event.x, event.y) ?: -1
//                    if ( theChar != -1 ) {
//                        layout?.onDoubleClick(theChar)
//                    }
//                }
//                lastTapTime = now
//                lastTouchX = event.x
//                lastTouchY = event.y
//            }
//        }
//        return super.onTouchEvent(event)
//    }

    fun onDoubleTapped(x: Float , y :Float){
        val theChar = layout?.getTouchedChar(currentIndex , x, y) ?: -1
        if ( theChar != -1 ) {
            layout?.onDoubleClick(theChar)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        layout = null
    }
}
