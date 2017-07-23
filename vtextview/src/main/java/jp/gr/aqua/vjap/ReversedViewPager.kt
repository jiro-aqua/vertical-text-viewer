package jp.gr.aqua.vjap


import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import java.util.*


class ReversedViewPager : ViewPager {

    private var leftScrollDisabled = false

    var isScrollDisabled = false

    var totalPage = -1
        set(totalPage) {
            field = totalPage
            isScrollDisabled = totalPage <= 0
        }
    private var currentPage: Int = 0

    constructor(context: Context) : super(context) {
        super.addOnPageChangeListener(reversedOnPageChangeListener)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        super.addOnPageChangeListener(reversedOnPageChangeListener)
    }

    internal var prevX = 0f
    var virtualX = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isScrollDisabled) { //無効フラグが立っていればスクロールしない
            //event.setLocation( 0 ,0);
            return true
        }

        if (leftScrollDisabled) {
            val ex = event.x
            if (event.x - prevX > 0) {
                virtualX += (0.1 * (event.x - prevX)).toFloat()
            } else {
                virtualX += event.x - prevX
            }

            event.setLocation(virtualX, event.y)
            prevX = ex
//            Log.d("ex", virtualX.toString() + "")
        }
        return super.onTouchEvent(event)
    }

    override fun addOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        listeners.add(listener)
    }

    private val listeners = ArrayList<OnPageChangeListener>()

    /*@Override
	public int getCurrentItem(){
		return currentPage;
	}*/

    override fun getCurrentItem(): Int {
        return MAX_PAGE - super.getCurrentItem()
    }

    override fun setCurrentItem(i: Int) {
        super.setCurrentItem(MAX_PAGE - i)
    }

    override fun setCurrentItem(i: Int, smooth: Boolean) {
        super.setCurrentItem(MAX_PAGE - i, smooth)
    }


    override fun setAdapter(arg0: PagerAdapter) {
        super.setAdapter(arg0)
        this.setCurrentItem(0, false)
    }

    //ページ切り替え時の処理
    internal var reversedOnPageChangeListener: ViewPager.OnPageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageSelected(position: Int) {

            virtualX = 0f

            leftScrollDisabled = MAX_PAGE - position > totalPage && totalPage > 0

            if (MAX_PAGE - position > totalPage + 1 && totalPage > 0) {
                currentItem = totalPage + 1
            } else {
                currentPage = MAX_PAGE - position
                listeners.forEach {
                    it.onPageSelected(currentPage)
                }
                //Log.d("page 1",currentPage+"");
            }
        }

        override fun onPageScrollStateChanged(arg0: Int) {
            listeners.forEach {
                it.onPageScrollStateChanged(arg0)
            }
        }

        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
            listeners.forEach {
                it.onPageScrolled(arg0, arg1, arg2)
            }
        }
    }

    companion object {

        internal val MAX_PAGE = 2048
    }
}
