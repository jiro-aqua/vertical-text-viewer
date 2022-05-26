package jp.gr.aqua.vjap

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import jp.gr.aqua.vtextviewer.BuildConfig
import jp.gr.aqua.vtextviewer.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlin.properties.Delegates
import kotlin.system.measureTimeMillis


class VTextLayout : RelativeLayout {

    companion object {
        private const val PAGING_BAR_SIZE = 60f
    }


    private val layout = VerticalLayout()

    private var viewPager by Delegates.notNull<ReversedViewPager>()
    private var adapter by Delegates.notNull<PagerAdapter>()

    private var pagingBar by Delegates.notNull<ReversedSeekBar>()
    private var pagingBarLayout by Delegates.notNull<View>()
    private var pageNumText by Delegates.notNull<TextView>()
    private var progressBar by Delegates.notNull<ProgressBar>()

    private var density: Float = 0.toFloat()
    private var scaledDensity: Float = 0.toFloat()

    private var currentPage = 1
    private var contentText : String = ""
    private var position = 0

    private var wrapPosition = 0

    private var writingPaperChars : Int = 0

    private var writingPaperMode = false

    private val layoutObservable  = MutableSharedFlow<Pair<Int,Int>>()
    private var job : Job? = null

    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null

    private val fontScales = arrayOf(0.62F,0.68F,0.75F,0.83F,0.90F,1.0F,1.1F,1.21F,1.33F,1.46F,1.61F,1.77F)
    private var fontScale = 6
    private var fontSize = 1
    private var fontTypeface: Typeface? = null
    private var fontIpamincho: Boolean = false

    private var fontColor = Color.BLACK
    private var bgColor = Color.WHITE

    // CoroutineStart.UNDISPATCHED　を指定してlaunchすると、emitが同時に呼ばれない限りスレッド切り替えが発生しない
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    // scope内でemitする。ここ以外では、直接emit()を呼ばないこと。
    private fun <T> MutableSharedFlow<T>.emitInScope(param: T) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            emit(param)
        }
    }

    // scope内でcollectする
    private fun <T> Flow<T>.observe(block: FlowCollector<T>) {
        scope.launch {
            collect(block)
        }
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, def: Int) : super(context, attrs, def) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_vtext, this)

        //アダプター作成
        adapter = object : PagerAdapter() {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                //左スクロールにするためにページとポジションを反転
                //ReversedViewPagerはsetCurrentItemを上書きしているが、ここで来るpositionは生のモノ
                val page = ReversedViewPager.MAX_PAGE - position - 1
                val view = VTextView(context).apply{
                    setBackgroundColor(bgColor)
                    setLayout(layout, page)
                    tag = page
                }
                container.addView(view)
                return view
            }

            override fun getCount(): Int {
                return ReversedViewPager.MAX_PAGE //左スクロールにするためにページを確保
            }

            override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
                // コンテナから View を削除
                container.removeView(obj as View)
            }

            override fun isViewFromObject(view: View, obj: Any): Boolean {
                return view === obj
            }

            override fun getItemPosition(obj: Any): Int {
                return POSITION_NONE
            }

        }

        //ページャを作成
        viewPager = findViewById(R.id.view_pager) as ReversedViewPager
        viewPager.adapter = adapter

        //ページ切り替え時の処理
        viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updatePageText()

                //Log.d("page=", currentPage.toString() + "")
            }

            override fun onPageScrollStateChanged(arg0: Int) {}

            override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}
        })

        setOnLayoutListener()

        pagingBar = findViewById(R.id.seekBar) as ReversedSeekBar
        pageNumText = findViewById(R.id.pageNumText) as TextView
        pagingBarLayout = findViewById(R.id.seekBarLayout)
        progressBar = findViewById(R.id.vtextProgressBar) as ProgressBar

        density = resources.displayMetrics.density//画面クリック位置判定用
        scaledDensity = resources.displayMetrics.scaledDensity // フォントサイズ計算用

        // ページ送りバー
        pagingBarLayout.visibility = View.GONE
        pagingBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // ツマミをドラッグしたときに呼ばれる
                if (pagingBarLayout.visibility == View.VISIBLE) {
                    updatePageText()
                    viewPager.setCurrentItem(seekBar.progress, false)
                    //vTextView.setPage(seekBar.getProgress());
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        //ページカウントバー
        progressBar.visibility = View.VISIBLE

        // レイアウトイベント
        layoutObservable.observe {
            if ( contentText.isNotEmpty() ){
                try {
                    withContext(Dispatchers.IO) {
                        val (width, height) = it
                        if (layout.needReLayout(width, height, contentText)) {
                            layout.setColor(fontColor, bgColor)
                            layout.needRelayoutFlag = false
                            layout.density = density
                            layout.scaledDensity = scaledDensity
                            layout.writingPaperMode = writingPaperMode
                            layout.setWritingPaperChars(writingPaperChars)
                            layout.setFont(
                                (fontSize * fontScales[fontScale]).toInt(),
                                fontTypeface!!,
                                fontIpamincho
                            )
                            layout.setSize(width, height)
                            layout.setWrapPosition(wrapPosition)
                            val measureTime = measureTimeMillis {
                                val pageCount = layout.calcPages(contentText)
                                viewPager.totalPage = pageCount - 1
                            }
                            if (BuildConfig.DEBUG) {
                                Log.d("======>", "time=${measureTime}ms len=${contentText.length}")
                                Log.d(
                                    "======>",
                                    "speed=${(measureTime.toFloat() * 1000F) / contentText.length} us/char"
                                )
                            }
                        }
                    }
                    progressBar.visibility = View.GONE
                    currentPage = layout.getPageByPosition(position)
                    updatePageText()
                    viewPager.setCurrentItem(currentPage, false)
                    viewPager.adapter!!.notifyDataSetChanged()
                }
                catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        gestureDetector = GestureDetector(context, GestureListener())
        scaleGestureDetector = ScaleGestureDetector( context , ScaleGestureListener() )
    }

    internal fun updatePageText() {
        val pageCount = layout.pageCount
        val text = if (pageCount < 0) currentPage.toString() + "" else "$currentPage/$pageCount"
        pageNumText.text = text
    }

    private fun updatePageNum(@Suppress("SameParameterValue") showSeekBar: Boolean) {
        pagingBar.max = layout.pageCount
        pagingBar.progress = currentPage
        updatePageText()
        progressBar.visibility = View.GONE
        if (showSeekBar) pagingBarLayout.visibility = View.VISIBLE

    }

    private var touchStartX = 0F
    private var touchStartY = 0F


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //Log.d("touch vt layout", ev.getY() +":"+ getHeight()  );

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                //下エリアをタッチした時にバーが非表示なら表示、
                //それ以外をタッチした時にバーが表示なら非表示に

                if (ev.y > height - PAGING_BAR_SIZE * density) {
                    if (pagingBarLayout.visibility != View.VISIBLE && layout.pageCount > 1) {
                        updatePageNum(true)
                        return true
                    }
                } else {
                    if (pagingBarLayout.visibility == View.VISIBLE) {
                        pagingBarLayout.visibility = View.INVISIBLE
                        return true
                    }
                }
                //初期値を保存。ページ送り方向は初期値で決定
                touchStartX = ev.x
                touchStartY = ev.y
            }
            MotionEvent.ACTION_UP -> {
            }
        }

//        val action = ev.action
//        if ( action == MotionEvent.ACTION_DOWN ){
//            val now = System.currentTimeMillis()
//            val xdiff = Math.abs(ev.x - lastTouchX)
//            val ydiff = Math.abs(ev.y - lastTouchY)
//            if ( xdiff < touchSlop && ydiff < touchSlop && ( now - lastTapTime ) < doubleTapTimeout ){
//                Log.d("=====>","Double Tapped! ${viewPager.getCurrentItem()}" )
//                val vtextview = findViewWithTag(viewPager.getCurrentItem())
//                if ( vtextview is VTextView ){
//                    vtextview.onDoubleTapped(ev.x , ev.y)
//                }
//            }
//            lastTapTime = now
//            lastTouchX = ev.x
//            lastTouchY = ev.y
//        }
        gestureDetector?.onTouchEvent(ev)
        scaleGestureDetector?.onTouchEvent(ev)

        return super.onInterceptTouchEvent(ev)
    }

    //VTextViewへのラッパー群
    fun setText(text: String) {
        contentText = text

        val width = viewPager.width
        val height = viewPager.height
        layoutObservable.emitInScope(width to height)
    }

    //フォント指定
    fun setFont(size: Int, typeface: Typeface, ipamincho: Boolean) {
        fontSize = size
        fontTypeface = typeface
        fontIpamincho = ipamincho
        layout.setFont(size, typeface, ipamincho)
    }

    fun setPadding(padding: Int) {
        layout.setPadding(padding)
    }

    fun setInitialPosition(pos: Int) {
        this.position = pos
    }

    fun setWrapPosition(wrapPosition: Int) {
        this.wrapPosition = wrapPosition
    }

    fun setWritingPaperMode(writingPaperMode : Boolean) {
        this.writingPaperMode = writingPaperMode
    }

    fun setWritingPaperChars(chars : Int) {
        this.writingPaperChars = chars
    }

    fun setColor(fontColor : Int, bgColor: Int) {
        this.fontColor = fontColor
        this.bgColor = bgColor

        pageNumText.setTextColor(fontColor)
        pageNumText.setBackgroundColor(bgColor)
    }

    fun getCurrentPosition(): Int {
        return layout.getPositionByPage(currentPage)
    }

    fun getCurrentStartPosition(): Int {
        return layout.getStartPositionByPage(currentPage)
    }

    fun reLayoutChildren() {
        setOnLayoutListener()
        measure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY))
        layout(left, top, right, bottom)
    }

    private fun setOnLayoutListener() {
        val vto = viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            @Suppress("DEPRECATION")
            override fun onGlobalLayout() {
                val width = viewPager.width
                val height = viewPager.height

                val vtoo = viewTreeObserver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    vtoo.removeOnGlobalLayoutListener(this)
                } else {
                    vtoo.removeGlobalOnLayoutListener(this)
                }
                layoutObservable.emitInScope(width to height)
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
        layout.clear()
    }

    fun setOnDoubleClickListener( listener : (Int)->Unit ){
        layout.setOnDoubleClickListener(listener)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        // event when double tap occurs
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d("=====>","Double Tapped! ${viewPager.currentItem}" )
            val vtextview = viewPager.findViewWithTag<VTextView>(viewPager.currentItem)
            if ( vtextview is VTextView ){
                vtextview.onDoubleTapped(e.x , e.y)
            }
            return true
        }
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Suppress("SpellCheckingInspection")
        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            detector?.let{
                val factor = it.scaleFactor
                if ( layout.writingPaperMode ){
                    val lastWritingPaperChars = writingPaperChars
                    writingPaperChars = if ( factor < 1.0F ){
                        40
                    }else{
                        20
                    }
                    val vtextview = viewPager.findViewWithTag<VTextView>(viewPager.currentItem)
                    if ( vtextview is VTextView && lastWritingPaperChars != writingPaperChars ) {
                        val posx = it.focusX
                        val posy = it.focusY
                        val tappedPos = vtextview.getTappedPosition(posx,posy)
                        val pos = if ( tappedPos != -1 ) tappedPos else getCurrentPosition()
                        setInitialPosition(pos)
                        layout.needRelayoutFlag = true
                        val width = viewPager.width
                        val height = viewPager.height
                        layoutObservable.emitInScope(width to height)
                    }
                }else{
                    val lastFontScale = fontScale
                    if ( factor < 1.0F ){
                        fontScale --
                    }else{
                        fontScale ++
                    }
                    if ( fontScale < 0 ) fontScale = 0
                    if ( fontScale >= fontScales.size ) fontScale = fontScales.size - 1
                    val vtextview = viewPager.findViewWithTag<VTextView>(viewPager.currentItem)
                    if ( vtextview is VTextView && lastFontScale != fontScale) {
                        val posx = it.focusX
                        val posy = it.focusY
                        val tappedPos = vtextview.getTappedPosition(posx,posy)
                        val pos = if ( tappedPos != -1 ) tappedPos else getCurrentPosition()
                        setInitialPosition(pos)
                        layout.needRelayoutFlag = true
                        val width = viewPager.width
                        val height = viewPager.height
                        layoutObservable.emitInScope(width to height)
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if ( event == null ) return false

        val keyCode = event.keyCode
        if ( keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ){
            if ( event.action == KeyEvent.ACTION_DOWN ){
                val current = viewPager.currentItem
                if ( current > 0 ){
                    viewPager.setCurrentItem(current-1,true)
                }
            }
            return true
        }
        if ( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_SPACE ){
            if ( event.action == KeyEvent.ACTION_DOWN ) {
                val current = viewPager.currentItem
                if (current <= viewPager.totalPage ) {
                    viewPager.setCurrentItem(current + 1, true)
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

}
