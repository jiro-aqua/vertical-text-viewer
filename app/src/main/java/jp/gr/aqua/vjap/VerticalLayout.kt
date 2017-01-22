package jp.gr.aqua.vjap

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.util.Log

import java.lang.Character.UnicodeBlock
import java.util.ArrayList
import kotlin.properties.Delegates

//methods

class VerticalLayout {

    private var TOP_SPACE = 18
    private var BOTTOM_SPACE = 18
    private var LEFT_SPACE = 18
    private var RIGHT_SPACE = 18

    //variables

    private var mFace: Typeface? = null

    private var bodyStyle by Delegates.notNull<TextStyle>()
    private var rubyStyle by Delegates.notNull<TextStyle>() // ルビ描字用

    private var text: String by Delegates.notNull<String>()
    private val pageIndex = ArrayList<Int>()

    private var width  = 0
    private var height = 0

    private var wrapPosition = 0
    private var ruby by Delegates.notNull<Ruby>()

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun setFont(size: Int, typeface: Typeface, ipamincho: Boolean) {
        mFace = typeface
        CharSetting.initCharMap(ipamincho)
        bodyStyle = TextStyle(size)
        rubyStyle = TextStyle(size / 2)
        rubyStyle.lineSpace = bodyStyle.lineSpace
    }

    fun setPadding(padding: Int) {
        TOP_SPACE = padding
        BOTTOM_SPACE = padding
        LEFT_SPACE = padding
        RIGHT_SPACE = padding
    }

    fun setWrapPosition(wrapPosition: Int) {
        this.wrapPosition = wrapPosition
    }

    fun setRubyMode(rubyMode : String) {
        ruby = Rubys(rubyMode).getRuby()
    }

    //文字描画関数
    private fun drawChar(canvas: Canvas?, s: String, pos: PointF, style: TextStyle) {
        val setting = CharSetting.getSetting(s)
        val fontSpacing = style.fontSpace//paint.getFontSpacing();
        var halfOffset = 0f//縦書き半角文字の場合の中央寄せ
        //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
        val isLatin = UnicodeBlock.of(s[0]) === UnicodeBlock.BASIC_LATIN
        if (isLatin) {
            if (setting != null && setting.angle != 0.0f) {
                pos.y -= fontSpacing / 2
            } else {
                halfOffset = 0.2f
            }
        }
        //描画スキップのフラグ
        if (canvas != null) {
            if (setting == null) {
                // 文字設定がない場合、そのまま描画
                canvas.drawText(s, pos.x + fontSpacing * halfOffset, pos.y, style.paint)
            } else {
                // 文字設定が見つかったので、設定に従い描画
                canvas.save()
                canvas.rotate(setting.angle, pos.x, pos.y)
                canvas.scale(setting.scaleX, setting.scaleY, pos.x, pos.y)
                canvas.drawText(s,
                        pos.x + fontSpacing * setting.x, pos.y + fontSpacing * setting.y,
                        style.paint)
                canvas.restore()
            }
        }
    }

    //文字列描画関数
    private fun drawString(canvas: Canvas?, s: String, pos: PointF, style: TextStyle): Boolean {
        for (i in 0..s.length - 1) {
            canvas?.let{ drawChar(canvas, s[i] + "", pos, style) }
            if (!goNext(pos, style, true)) {
                return false
            }
        }
        return true
    }

    //改行処理。次の行が書ければtrue 端に到達したらfalse
    private fun goNextLine(pos: PointF, type: TextStyle, spaceRate: Float): Boolean {
        pos.x -= type.lineSpace * spaceRate
        pos.y = TOP_SPACE + type.fontSpace
        return pos.x > LEFT_SPACE
    }

    //次の位置へカーソル移動　次の行が書ければtrue 端に到達したらfalse
    private fun goNext(pos: PointF, type: TextStyle, lineChangable: Boolean): Boolean {
        var newLine = false

        val bottomY = (height - BOTTOM_SPACE).toFloat()
        val wrapY = TOP_SPACE + bodyStyle.fontSpace * wrapPosition

        val wrap : Float = if ( wrapPosition == 0 || bottomY < wrapY ) bottomY else wrapY

        if (pos.y + type.fontSpace > wrap ) {
            // もう文字が入らない場合
            newLine = true
        }

        if (newLine && lineChangable) {
            // 改行処理
            return goNextLine(pos, type, 1f)
        } else {
            // 文字を送る
            val fontSpace = type.fontSpace
            //if(checkHalf( s )) fontSpace /= 2;
            pos.y += fontSpace
        }
        return true
    }

    private fun initPos(pos: PointF) {
        pos.x = width.toFloat() - bodyStyle.lineSpace - RIGHT_SPACE.toFloat()
        pos.y = TOP_SPACE + bodyStyle.fontSpace
    }

    //基準値座標の設定
    private fun getHeadPos(pos: PointF): PointF {
        val res = PointF()
        res.x = pos.x
        res.y = pos.y
        return res
    }

    private fun getRubyPos(state: CurrentState): PointF {
        val res = PointF()
        res.x = state.rubyStart.x + bodyStyle.fontSpace//一文字ずらして表示
        res.y = state.rubyStart.y - rubyStyle.fontSpace//縦書きの場合は基準がずれているため補正
        if (state.pos.y - state.rubyStart.y > 0) { //改行が入っていない場合
            res.y -= (0.5 * (state.rubyText.length * rubyStyle.fontSpace - (state.pos.y - state.rubyStart.y))).toFloat()
        }
        if (res.y < TOP_SPACE) res.y = TOP_SPACE.toFloat()

        return res
    }

    fun calcPages(text: String?): Int {
        if (text != null) {
            this.text = text

            if ( BOTTOM_SPACE < bodyStyle.fontSpace ){
                BOTTOM_SPACE = bodyStyle.fontSpace.toInt()
            }

            pageIndex.clear()

            var current = 0
            Log.d("page", current.toString() + "")
            while (!textDraw(null, current)) {
                //描画を無効化して最後のページになるまで進める。
                current++
            }
            Log.d("page", current.toString() + "")

            return pageIndex.size
        } else {
            return 0
        }
    }

    fun getPageByPosition(position: Int): Int {
        val pageSize = pageIndex.size
        if (pageSize > 1) {
            (0..pageSize - 1 - 1)
                    .filter { pageIndex[it] <= position && position <= pageIndex[it + 1] }
                    .forEach { return it + 1 }
        }
        return 1
    }

    fun getPositionByPage(page: Int): Int {
        val pageSize = pageIndex.size
        if (pageSize <= page) {
            return pageIndex[pageSize - 1]
        }
        val from = pageIndex[page - 1]
        val to = pageIndex[page]
        return (from + to) / 2
    }


    fun textDraw(canvas: Canvas?, page: Int): Boolean {
        val state = CurrentState()
        initPos(state.pos)
        initPos(state.rpos)
        //テキスト位置を初期化
        //Log.d("debug", "width:"+width);
        var endFlag = true

        var index = 0
        if (page > 0) {
            index = pageIndex[page - 1]
        }

        //描画
        while (index < text.length) {
            state.lineChangable = true
            state.strPrev = state.str
            state.str = text.characterAt(index)
            val len = state.str.length
            state.sAfter = if (index + len < text.length) text.characterAt(index + len)  else ""

            if (!charDrawProcess(canvas, state)) {
                endFlag = false
                break
            }
            index += len
        }
        //this.isPageEnd = endFlag;
        if (canvas == null) {
            pageIndex.add(index + 1)
        }
        return endFlag
    }

    private fun String.characterAt(i : Int) : String {
        val c1 = this[i]
        if ( UnicodeBlock.of(c1) == UnicodeBlock.HIGH_SURROGATES ){
            val c2 = this[i+1]
            val s = c1.toString() + c2.toString()
            return s
        }
        if ( this.startsWith(ruby.bodyStart1, startIndex = i) ){
            return ruby.bodyStart1
        }
        if ( ruby.bodyStart2 != null ) {
            if (this.startsWith(ruby.bodyStart2!!, startIndex = i)) {
                return ruby.bodyStart2!!
            }
        }
        if ( this.startsWith(ruby.rubyStart, startIndex = i) ){
            return ruby.rubyStart
        }
        if ( this.startsWith(ruby.rubyEnd, startIndex = i) ){
            return ruby.rubyEnd
        }
        return c1.toString()
    }



    private fun charDrawProcess(canvas: Canvas?, state: CurrentState): Boolean {

        //ルビが振られている箇所とルビ部分の判定
        if (state.isRubyEnable) {
            if (state.isRubyBody && (state.bodyText.length > 20 || state.str == "\n")) {
                drawString(canvas, state.buf + state.bodyText, state.pos, bodyStyle )
                state.bodyText = ""
                state.buf = ""
                state.isRubyBody = false
            }

            if (state.str == ruby.bodyStart1 || state.str == ruby.bodyStart2 ) {            //ルビ本体開始
                //ルビ開始中にルビ開始した場合は出力
                if (state.bodyText.isNotEmpty()) {
                    drawString(canvas, state.buf + state.bodyText, state.pos, bodyStyle )
                    state.bodyText = ""
                    state.buf = ""
                }
                state.bodyText = ""
                state.buf = state.str
                state.isRubyBody = true
                state.rubyStart = getHeadPos(state.pos)
                return true
            }
            if (state.str == ruby.rubyStart && //ルビ開始
                    (state.isRubyBody || state.isKanjiBlock)) { //ルビ開始状態であれば
                state.isRuby = true
                state.isRubyBody = false
                state.rubyText = ""
                return true
            }
            if (state.str == ruby.rubyEnd && state.isRuby) {    //ルビ終了
                drawString(canvas, state.bodyText, state.pos, bodyStyle )
                state.rpos = getRubyPos(state)
                drawString(canvas, state.rubyText, state.rpos, rubyStyle )
                state.isRuby = false
                state.bodyText = ""
                state.buf = ""
                return !state.isPageEnd
            }

            state.isKanjiBlock = if ( ruby.aozora ) {
                //漢字判定はルビ開始判定の後に行う必要あり
                val isKanji = UnicodeBlock.of(state.str[0]) === UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                //Log.d("kanji",state.str +":" + isKanji+state.isKanjiBlock);
                if (isKanji && !state.isKanjiBlock) {
                    //漢字が始まったら漢字ブロックフラグを立てる
                    //　｜のルビ本体の中でなければ
                    if (!state.isRubyBody) {
                        state.rubyStart = getHeadPos(state.pos)
                    }
                }
                isKanji
            }else false

            if (state.isRuby) {
                state.rubyText += state.str
                return true
            }
            if (state.isRubyBody) {
                state.bodyText += state.str
                return true
            }
        }
        //その他通常描字

        //タイトルならスタイル変更
        val style = bodyStyle

        //改行処理
        if (state.str == "\n") {
            if (state.strPrev == "\n") {
                return this.goNextLine(state.pos, style, 0.5.toFloat())
            } else {
                return this.goNextLine(state.pos, style, 1f)
            }

        }
        //文字を描画して次へ
        this.drawChar(canvas, state.str, state.pos, style )

        if (!this.goNext(state.pos, style, checkLineChangable(state))) {
            state.isPageEnd = true
            //ルビがある場合はルビを描画してから終了
            return state.isRubyBody
        }
        return true
    }

    val KINSOKU = ",)]｝、〕〉》」』】〙〗〟’”｠»" +
                    "ヽヾーァィゥェォッャュョヮヵヶぁぃぅぇぉっゃゅょゎゕゖㇰㇱㇳㇲㇳㇴㇵㇶㇷㇸㇹㇺㇻㇼㇽㇾㇿ々〻"+
                    "‐゠–〜"+
                    "？！?!‼⁇⁈⁉"+
                    "・:;。."

    private fun checkLineChangable(state: CurrentState): Boolean {
        if (!state.lineChangable) {//連続で禁則処理はしない
            state.lineChangable = true
        } else if (KINSOKU.contains(state.sAfter)) {
            state.lineChangable = false
        }
        return state.lineChangable
    }

    private inner class CurrentState internal constructor() {
        internal var strPrev = ""
        internal var str = ""
        internal var sAfter = ""

        internal var rubyText = ""//ルビ
        internal var bodyText = ""//ルビ対象
        internal var buf = ""//記号の一時保持用

        internal var isRubyEnable = true
        internal var isRuby = false
        internal var isKanjiBlock = false
        internal var isRubyBody = false
        internal var lineChangable = true
        internal var isPageEnd = false

        internal var pos = PointF()//カーソル位置
        internal var rpos =  PointF()//ルビカーソル位置

        internal var rubyStart = PointF()
        internal var rubyEnd = PointF()

        init {
            rpos = PointF()
            rubyStart = PointF()
            rubyEnd = PointF()
        }
    }

    private inner class TextStyle internal constructor(size: Int) {
        internal var paint: Paint
        internal var fontSpace = 0F
        internal var lineSpace = 0F

        init {
            this.paint = Paint()
            this.paint.textSize = size.toFloat()
            this.paint.color = FONT_COLOR
            this.paint.typeface = mFace
            this.paint.isAntiAlias = true
            this.paint.isSubpixelText = true

            //this.fontSpace = this.paint.getFontSpacing();
            this.fontSpace = size.toFloat()
            this.lineSpace = this.fontSpace * 2
        }
    }

    val pageCount: Int
        get() {
            return pageIndex.size
        }

    companion object {
        private val FONT_COLOR = Color.BLACK
    }
}
