package jp.gr.aqua.vjap

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.util.AttributeSet
import android.view.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import jp.gr.aqua.vtextviewer.R
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import kotlin.properties.Delegates


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
    private var contentText by Delegates.notNull<String>()
    private var position = 0

    private var wrapPosition = 0
    private var rubyMode = ""

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

                val view = VTextView(context)
                view.setLayout(layout, page)
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

    }

    internal fun updatePageText() {
        val pageCount = layout.pageCount
        val text = if (pageCount < 0) currentPage.toString() + "" else currentPage.toString() + "/" + pageCount
        pageNumText.text = text
    }

    fun updatePageNum(showSeekBar: Boolean) {
        pagingBar.max = layout.pageCount - 1
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
        }//			if( viewPager.isScrollDisabled() ){//ページングが有効ならクリックでページ送り
        //				int direction =  isClickDirectionLeft ? 1 : -1; //方向によって係数を変える
        //				if( direction * touchStartX > direction * vTextView.width /2 ) {
        //					if( currentPage > 1 ) {
        //						viewPager.setCurrentItem( currentPage -1 , false);
        //					}
        //				}else{
        //					if( currentPage < vTextView.getTotalPage() ||  vTextView.getTotalPage() < 0 ){
        //						viewPager.setCurrentItem( currentPage +1 , false);
        //					}
        //				}
        //			}
        //			updatePageText();

        return super.onInterceptTouchEvent(ev)
    }

    //VTextViewへのラッパー群
    fun setText(text: String) {
        contentText = text
        //reset();
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

    fun setRubyMode(rubyMode : String) {
        this.rubyMode = rubyMode
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

                Observable.just( 1 )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            layout.setSize(width, height)
                            layout.setWrapPosition(wrapPosition)
                            layout.setRubyMode(rubyMode)
                            val pageCount = layout.calcPages(contentText)
                            viewPager.totalPage = pageCount - 1
                            progressBar.visibility = View.GONE

                            currentPage = layout.getPageByPosition(position)
                            updatePageText()
                            viewPager.setCurrentItem(currentPage, false)
                        }
            }
        })
    }

}
