package jp.gr.aqua.vjap

import android.graphics.*
import android.util.Log
import android.util.SparseArray
import java.lang.Character.UnicodeBlock
import java.util.*
import kotlin.properties.Delegates

//methods

class VerticalLayout {

    private val DEBUG = false

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

    private val lines = ArrayList<Line>()
    private val charPositions = SparseArray<ArrayList<Pair<PointF,Int>>>()


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
        if ( s.isEmpty() ) return
        val setting = CharSetting.getSetting(s)
        val fontSpacing = style.fontSpace//paint.getFontSpacing();
        var halfOffset = 0f//縦書き半角文字の場合の中央寄せ
        //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
        if (s.isLatin()) {
            if (setting != null && setting.angle != 0.0f) {
                pos.y -= fontSpacing / 2
            } else {
                if ( s.length != 2 ) {
                    halfOffset = 0.2f
                }
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
    private fun drawString(canvas: Canvas?, s: ArrayList<String>, pos: PointF, style: TextStyle) {
        s.forEach {
            str ->
            canvas?.let{ drawChar(canvas, str, pos, style) }
            pos.y += style.fontSpace
        }
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
            res.y -= (0.5 * (state.rubyText.size * rubyStyle.fontSpace - (state.pos.y - state.rubyStart.y))).toFloat()
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

            charPositions.clear()

            pageIndex.clear()

            var idx = 0
            lines.clear()
            while( idx >= 0 ){
                val line = calcLine(text , idx)
                lines.add(line.first)
                idx = line.second
                if ( DEBUG ) {
                    var str = ""
                    line.first.line.forEach {
                        str += it
                    }
                    Log.d("===>", str)
                }
            }
            // PageIndexの計算
            val state = CurrentState()
            initPos(state.pos)
            initPos(state.rpos)
            lines.forEachIndexed {
                lineidx, list ->
                if ( state.pos.x < LEFT_SPACE ){
                    pageIndex.add(lineidx)
                    initPos(state.pos)
                }
                if ( list.line.size > 0 ){
                    state.pos.x -= bodyStyle.lineSpace
                }else{
                    state.pos.x -= bodyStyle.lineSpace / 2
                }
            }
            pageIndex.add(lines.size-1)

            return pageIndex.size
        } else {
            return 0
        }
    }

    fun getIndexFromPage(page:Int):Int {
        val line = lines[pageIndex[page]]
        if ( line.line.size > 0 ) {
            return line.index
        }else{ // 空行の時は後ろの行を探す
            ( pageIndex[page] .. pageIndex.last() ).forEach{
                val theLine = lines[it]
                if ( theLine.line.size > 0 ){
                    return theLine.index
                }
            }
            return text.lastIndex
        }
    }

    fun getPageByPosition(position: Int): Int {
        val pageSize = pageIndex.size
        if (pageSize > 1) {
            (0..pageSize - 1 - 1)
                    .filter { position < getIndexFromPage(it) }
                    .forEach {
                        //Log.d("====>","$it,$position,$pageIndex")
                        return it + 1
                    }
            return pageSize
        }
        return 1
    }

    fun getPositionByPage(page: Int): Int {
        val pageSize = pageIndex.size
        if (pageSize <= page) {
            return getIndexFromPage(pageSize - 1)
        }
        if ( page == 1 ){
            return 0
        }else{
            val from = getIndexFromPage(page - 2)
            val to = getIndexFromPage(page - 1)
            return (from + to) / 2
        }
    }

    fun getStartPositionByPage(page: Int): Int {
        val pageSize = pageIndex.size
        if (pageSize <= page) {
            return getIndexFromPage(pageSize - 1)
        }
        if ( page == 1 ){
            return 0
        }else{
            val from = getIndexFromPage(page - 2)
            return from
        }

    }
    fun textDraw(canvas: Canvas?, page: Int){
        val state = CurrentState()
        initPos(state.pos)
        initPos(state.rpos)
        //テキスト位置を初期化
        //Log.d("debug", "width:"+width);

        var lineptr = 0
        if (page > 0) {
            lineptr = pageIndex[page - 1]
        }

        val positionArray = ArrayList<Pair<PointF,Int>>()

//        val paint = Paint().apply {
//            style = Paint.Style.STROKE
//        }

        //描画
        while (lineptr < lines.size) {

            val line = lines[lineptr]
            var charptr = line.index
            val margin = calcMargin(line)
            val lineSize = if (line.line.size > 0) { 1.0F }else { 0.5F }
            val nextposx = state.pos.x - bodyStyle.lineSpace * lineSize
            val fontspace = bodyStyle.fontSpace
            val linespace = bodyStyle.lineSpace
            val length = line.line.size
            line.line.forEachIndexed {
                index, str ->
                state.str = str
                val oldy = state.pos.y
                val mar = if ( index == length -1 ) 0f else margin
                charDrawProcess(canvas, state , mar)
                val point = PointF( ( nextposx + linespace + state.pos.x + linespace ) / 2 ,
                        ( oldy - fontspace + state.pos.y - fontspace ) /2)
                positionArray.add( point to charptr)
//                canvas?.drawRect(rect,paint)

                charptr += str.length
            }
            if ( charPositions.get(page) == null ) {
                charPositions.append(page, positionArray)
            }

            //改行処理
            state.pos.x = nextposx
            state.pos.y = TOP_SPACE + bodyStyle.fontSpace

            // ページ幅を超えたらページエンド
            if ( state.pos.x <= LEFT_SPACE ) break

            // 次の行へ
            lineptr ++
        }
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



    private fun charDrawProcess(canvas: Canvas?, state: CurrentState , margin : Float) {

        //ルビが振られている箇所とルビ部分の判定
        if (state.isRubyEnable) {
            if (state.isRubyBody && (state.bodyText.size > 20 || state.str == "\n")) {
                state.bodyText.add(0,state.buf )
                drawString(canvas, state.bodyText, state.pos, bodyStyle )
                state.bodyText.clear()
                state.buf = ""
                state.isRubyBody = false
            }

            if (state.str == ruby.bodyStart1 || state.str == ruby.bodyStart2 ) {            //ルビ本体開始
                //ルビ開始中にルビ開始した場合は出力
                if (state.bodyText.isNotEmpty()) {
                    state.bodyText.add(0,state.buf )
                    drawString(canvas, state.bodyText, state.pos, bodyStyle )
                    state.bodyText.clear()
                    state.buf = ""
                }
                state.bodyText.clear()
                state.buf = state.str
                state.isRubyBody = true
                state.isRubyBodyStarted = true
                return
            }
            if (state.str == ruby.rubyStart && //ルビ開始
                    (state.isRubyBody || state.isKanjiBlock)) { //ルビ開始状態であれば
                state.isRuby = true
                state.isRubyBody = false
                state.rubyText.clear()
                return
            }
            if (state.str == ruby.rubyEnd && state.isRuby) {    //ルビ終了
                drawString(canvas, state.bodyText, state.pos, bodyStyle )
                state.rpos = getRubyPos(state)
                drawString(canvas, state.rubyText, state.rpos, rubyStyle )
                state.isRuby = false
                state.bodyText.clear()
                state.buf = ""
                return
            }

            state.isKanjiBlock = if ( ruby.aozora ) {

                //漢字判定はルビ開始判定の後に行う必要あり
                val isKanji = state.str.isNotEmpty() && ( UnicodeBlock.of(state.str[0]) === UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || state.str=="々" || state.str=="〆" )

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

            if ( state.isRubyBodyStarted ){
                state.rubyStart = getHeadPos(state.pos)
                state.isRubyBodyStarted = false
            }
            if (state.isRuby) {
                state.rubyText.add(state.str)
                return
            }
            if (state.isRubyBody) {
                state.bodyText.add(state.str)
                return
            }
        }
        //その他通常描字

        //タイトルならスタイル変更
        val style = bodyStyle

        //文字を描画して次へ
        this.drawChar(canvas, state.str, state.pos, style )

        state.pos.y += margin
        state.pos.y += style.fontSpace
    }

    private fun calcLine(text:String , index:Int): Pair< Line , Int > {

        val result = ArrayList<Pair<String,Int>>()

        // 文字幅
        val fontSpace = bodyStyle.fontSpace

        val bottomY = (height - BOTTOM_SPACE).toFloat() - fontSpace/2
        val wrapY = TOP_SPACE + fontSpace * wrapPosition

        val wrap : Float = if ( wrapPosition == 0 || bottomY < wrapY ) bottomY else wrapY

        var pos = 0f

        var idx = index

        var isRuby = false
        var broken = false

        while( idx < text.length ){
            val str = text.characterAt(idx)
            val len = str.length

            //ルビが振られている箇所とルビ部分の判定
            if (str == ruby.bodyStart1 || str == ruby.bodyStart2 ) {            //ルビ本体開始
                result.add(str to idx)
                idx += len
                continue
            }
            if (str == ruby.rubyStart ) { //ルビ開始状態であれば
                result.add(str to idx)
                idx += len
                isRuby = true
                continue
            }
            if (str == ruby.rubyEnd ) {    //ルビ終了
                result.add(str to idx)
                idx += len
                isRuby = false
                continue
            }
            if (isRuby) {
                result.add(str to idx)
                idx += len
                continue
            }
            //その他通常描字

            //タイトルならスタイル変更
            val style = bodyStyle

            //改行処理
            if (str == "\n") {
                idx += len
                broken = true
                break
            }

            val setting = CharSetting.getSetting(str)
            val fontSpacing = style.fontSpace//paint.getFontSpacing();
            //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
            if (str.isLatin()) {
                if (str.length != 2 && setting != null && setting.angle != 0.0f) {
                    pos -= fontSpacing / 2
                }
            }

            pos += fontSpace

            // 行端を超えた場合終了
            if ( pos > wrap ){
                break
            }

            //文字を描画して次へ
            result.add(str to idx)
            idx += len
        }

        if ( idx >= text.length ){
            broken = true
        }

        if ( !broken && result.isNotEmpty()) {
            // 行末禁則のチェック
            val last = result.last()
            if ( !ruby.isRubyMarkup(last.first) && KINSOKU_GYOUMATU.contains(last.first)) {
                result.removeAt(result.size-1)
                idx = last.second
            }else {

                // 次の行の行頭を作成
                var next = if (idx < text.length) text.characterAt(idx) to idx else "" to -1

                // ぶら下げ禁則文字をぶら下げる
                if (!ruby.isRubyMarkup(next.first) && KINSOKU_BURASAGE.contains(next.first)) {
                    result.add(next)
                    idx += next.first.length
                    next = if (idx < text.length) text.characterAt(idx) to idx else "" to -1
                }

                // 次の行をチェック
                if (pos > wrap) {
                    val firstIdx = idx
                    val firstResult = ArrayList<Pair<String, Int>>()
                    firstResult.addAll(result)

                    while (result.size > 0) {
                        val lastchar = result.last()
                        if (!ruby.isRubyMarkup(next.first) && KINSOKU_GYOUTOU.contains(next.first)) {
                            // ルビ記号でなく、行末禁則文字なら追い出す
                            idx = lastchar.second
                            next = lastchar
                            result.removeAt(result.size-1)
                        } else if ( next.first.isHalfAlNum() && lastchar.first.isHalfAlNum() ) {
                            // 英数字二連続なら追い出す
                            idx = lastchar.second
                            next = lastchar
                            result.removeAt(result.size-1)
                        }else{
                            if (next.first == "\n") {
                                idx += next.first.length
                            }
                            break
                        }
                    }
                    // 戻しすぎた時は、元の文字列を使用する
                    if (result.size == 0) {
                        result.addAll(firstResult)
                        idx = firstIdx
                    }
                }
            }
        }
        if ( idx >= text.length ){
            idx = -1
        }

        // 文字部分のみを抽出
        val strarray = ArrayList<String>()
        result.forEach {
            strarray.add(it.first)
        }

        val merged = mergeTwoLatinCharacters(strarray)

        return Line( merged, index, broken) to idx
    }

    private fun calcMargin(line : Line): Float {

        if ( line.broken ){
            return 0F
        }

        // 文字幅
        val fontSpace = bodyStyle.fontSpace

        val bottomY = (height - BOTTOM_SPACE).toFloat() - fontSpace/2
        val wrapY = TOP_SPACE + bodyStyle.fontSpace * wrapPosition

        val wrap : Float = if ( wrapPosition == 0 || bottomY < wrapY ) bottomY else wrapY

        //if(checkHalf( s )) fontSpace /= 2;
        var pos = 0f

        var isRuby = false
        var charcount = 0

        line.line.forEach {
            val str = it

            //ルビが振られている箇所とルビ部分の判定
            if (str == ruby.bodyStart1 || str == ruby.bodyStart2 ) {            //ルビ本体開始
                return@forEach
            }
            if (str == ruby.rubyStart ) { //ルビ開始状態であれば
                isRuby = true
                return@forEach
            }
            if (str == ruby.rubyEnd ) {    //ルビ終了
                isRuby = false
                return@forEach
            }
            if (isRuby) {
                return@forEach
            }
            //その他通常文字

            //タイトルならスタイル変更
            val style = bodyStyle

            val setting = CharSetting.getSetting(str)
            val fontSpacing = style.fontSpace//paint.getFontSpacing();
            //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
            val isLatin = UnicodeBlock.of(str[0]) === java.lang.Character.UnicodeBlock.BASIC_LATIN
            if (isLatin) {
                if (setting != null && setting.angle != 0.0f) {
                    pos -= fontSpacing / 2
                }
            }

            pos += fontSpace
            charcount ++
        }

        val diff = wrap - pos
        // 句読点が行末に来る場合はぶら下げになるように調整
        if ( charcount > 2 && KINSOKU_BURASAGE.contains(line.line.last()) ){
            return (diff + fontSpace ) / ( charcount -1 )
        }
        if ( diff < 0 ){
            return 0F
        }
        return diff / charcount
    }

    private fun mergeTwoLatinCharacters(line : ArrayList<String>) : ArrayList<String> {

        var last = ""
        var last1 = 0
        var last2 = 0
        val result = ArrayList<String>()

        line.forEachIndexed {
            idx, str ->
            result.add(str)
            val kind = str.kind()
            if ( idx > 0 ){
                if ( kind != 0 && last1 != last2 && last1 == kind && kind != line.kindOfNextCharacter(idx) ){
                    // 二文字のLatin連続を発見
                    // 出力側の最後二文字を削除して結合する
                    result.removeAt(result.size -1)
                    result.removeAt(result.size -1)
                    result.add( last+str )
                }
            }
            last2 = last1
            last1 = kind
            last = str
        }
        return  result
    }

    private fun String.isLatin() : Boolean {
        return  this.isNotEmpty() && UnicodeBlock.of(this[0]) === java.lang.Character.UnicodeBlock.BASIC_LATIN
    }

    private fun String.isHalfAlNum() : Boolean {
        return this.kind() != 0
    }

    private fun String.kind() : Int
    {
        if (this.isEmpty()) return 0
        val ch = this[0]
        if ( '0' <= ch && ch <= '9') return 1
        if ( 'A' <= ch && ch <= 'Z') return 2
        if ( 'a' <= ch && ch <= 'z') return 2
        return 0
    }

    private fun ArrayList<String>.kindOfNextCharacter(idx:Int) : Int
    {
        if ( idx + 1 < this.size ){
            return this[idx+1].kind()
        }else{
            return 0
        }
    }

    private val KINSOKU_BURASAGE = ",)]｝、〕〉》」』】〙〗〟’”｠»）" +
            "。.　 "

    private val KINSOKU_GYOUTOU = ",)]｝、〕〉》」』】〙〗〟’”｠»）" +
                    "ヽヾーァィゥェォッャュョヮヵヶぁぃぅぇぉっゃゅょゎゕゖㇰㇱㇳㇲㇳㇴㇵㇶㇷㇸㇹㇺㇻㇼㇽㇾㇿ々〻"+
                    "‐゠–〜～"+
                    "？！?!‼⁇⁈⁉"+
                    "・:;。.　 "

    private val KINSOKU_GYOUMATU = "([｛〔〈《「『【〘〖〝‘“｟«"

    private inner class CurrentState internal constructor() {
        internal var str = ""

        internal var rubyText = ArrayList<String>() //ルビ
        internal var bodyText = ArrayList<String>() //ルビ対象
        internal var buf = "" //記号の一時保持用

        internal var isRubyEnable = true
        internal var isRuby = false
        internal var isKanjiBlock = false
        internal var isRubyBody = false
        internal var isRubyBodyStarted = false

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

    fun  getTouchedChar( page:Int , x: Float, y: Float): Int {
        val array = charPositions.get(page)

        var nearest : Pair<Float,Int> = 1000000F to -1
        array.forEach {
            val distance = Math.sqrt( (( it.first.x - x ) * ( it.first.x - x ) +  ( it.first.y - y ) * ( it.first.y - y )).toDouble() ).toFloat()
            if ( distance < nearest.first ){
                nearest = distance to it.second
            }
        }
        return nearest.second
    }

    fun setOnDoubleClickListener( listener : (Int)->Unit ){
        onDoubleClickListener = listener
    }

    private var onDoubleClickListener : ((Int)->Unit)? = null
    fun onDoubleClick( pos:Int){
        onDoubleClickListener?.invoke(pos)
    }

}
