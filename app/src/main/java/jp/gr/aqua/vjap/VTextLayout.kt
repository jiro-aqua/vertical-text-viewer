package jp.gr.aqua.vjap

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import jp.gr.aqua.vtextviewer.BuildConfig
import jp.gr.aqua.vtextviewer.R
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import kotlin.properties.Delegates
import kotlin.system.measureTimeMillis


class VTextLayout : RelativeLayout {

    private val PAGING_BAR_SIZE = 60f

    private val layout = VerticalLayout()

    private var viewPager by Delegates.notNull<ReversedViewPager>()
    private var adapter by Delegates.notNull<PagerAdapter>()

    private var pagingBar by Delegates.notNull<ReversedSeekBar>()
    private var pagingBarLayout by Delegates.notNull<View>()
    private var pageNumText by Delegates.notNull<TextView>()
    private var progressBar by Delegates.notNull<ProgressBar>()

    private var density: Float = 0.toFloat()

    private var currentPage = 1
    private var contentText : String = ""
    private var position = 0

    private var wrapPosition = 0

    private val layoutObservable  = PublishSubject.create<Pair<Int,Int>>()
    private val subscription = CompositeSubscription()

    private var gestureDetector: GestureDetector? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, def: Int) : super(context, attrs, def) {
        init(context)
    }

    fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.view_vtext, this)

        //アダプター作成
        adapter = object : PagerAdapter() {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                //左スクロールにするためにページとポジションを反転
                //ReversedViewPagerはsetCurrentItemを上書きしているが、ここで来るpositionは生のモノ
                val page = ReversedViewPager.MAX_PAGE - position - 1
                val view = VTextView(context).apply{
                    setLayout(layout, page)
                    tag = position
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
        subscription.add( layoutObservable
                .subscribeOn(Schedulers.io())
                .filter { contentText.isNotEmpty() }
                .doOnNext {
                    val (width,height) = it
                    if ( layout.needReLayout(width, height, contentText) ) {
                        layout.setSize(width, height)
                        layout.setWrapPosition(wrapPosition)
                        val measureTime = measureTimeMillis {
                            val pageCount = layout.calcPages(contentText)
                            viewPager.totalPage = pageCount - 1
                        }
                        if ( BuildConfig.DEBUG ){
                            Log.d("======>", "time=${measureTime}ms len=${contentText.length}")
                            Log.d("======>", "speed=${(measureTime.toFloat() * 1000F) / contentText.length} us/char")
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    progressBar.visibility = View.GONE
                    currentPage = layout.getPageByPosition(position)
                    updatePageText()
                    viewPager.setCurrentItem(currentPage, false)
                    viewPager.adapter.notifyDataSetChanged()
                },{it.printStackTrace()},{} ))

        gestureDetector = GestureDetector(context, GestureListener())
    }

    internal fun updatePageText() {
        val pageCount = layout.pageCount
        val text = if (pageCount < 0) currentPage.toString() + "" else currentPage.toString() + "/" + pageCount
        pageNumText.text = text
    }

    fun updatePageNum(showSeekBar: Boolean) {
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

        return super.onInterceptTouchEvent(ev)
    }

    //VTextViewへのラッパー群
    fun setText(text: String) {
        contentText = text

        val width = viewPager.width
        val height = viewPager.height
        layoutObservable.onNext(width to height)
    }

    //フォント指定
    fun setFont(size: Int, typeface: Typeface, ipamincho: Boolean) {
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

    fun getCurrentPosition(): Int {
        return layout.getPositionByPage(currentPage)
    }

    fun getCurrentStartPosition(): Int {
        return layout.getStartPositionByPage(currentPage)
    }

    fun reLayoutChildren() {
        setOnLayoutListener()
        measure(
                View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY))
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
                layoutObservable.onNext(width to height)
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscription.unsubscribe()
        layout.clear()
    }

    fun setOnDoubleClickListener( listener : (Int)->Unit ){
        layout.setOnDoubleClickListener(listener)
    }

//    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
//    private val doubleTapTimeout by lazy { ViewConfiguration.getDoubleTapTimeout() }
//    private var lastTapTime = 0L
//    private var lastTouchX = 0F
//    private var lastTouchY = 0F

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        // event when double tap occurs
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d("=====>","Double Tapped! ${viewPager.currentItem}" )
            val vtextview = findViewWithTag(viewPager.currentItem)
            if ( vtextview is VTextView ){
                vtextview.onDoubleTapped(e.x , e.y)
            }
            return true
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
            return true;
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
            return true;
        }
        return super.dispatchKeyEvent(event)
    }

}
