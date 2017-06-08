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
}
