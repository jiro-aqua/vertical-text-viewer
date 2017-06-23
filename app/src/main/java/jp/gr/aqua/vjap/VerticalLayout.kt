package jp.gr.aqua.vjap

import android.graphics.*
import android.util.Log
import android.util.SparseArray
import java.lang.Character.UnicodeBlock
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.measureNanoTime

//methods

class VerticalLayout {

//    private val DEBUG = false

    private var TOP_SPACE = 18
    private var BOTTOM_SPACE = 18
    private var LEFT_SPACE = 18
    private var RIGHT_SPACE = 18

    //variables

    private var mFace: Typeface? = null

    private var bodyStyle by Delegates.notNull<TextStyle>()
    private var rubyStyle by Delegates.notNull<TextStyle>() // ルビ描字用

    private var text: String = ""
    private val pageIndex = ArrayList<Int>()

    private var width  = 0
    private var height = 0

    private var wrapPosition = 0
    private var ruby by Delegates.notNull<Ruby>()

    private val lines = ArrayList<Line>()
    private val charPositions = SparseArray<ArrayList<CharPoint>>()

    fun clear() {
        charPositions.clear()
        lines.clear()
        pageIndex.clear()
        text = ""
        mFace = null
    }

    fun needReLayout(width: Int, height: Int, contentText: String) : Boolean
    {
        return !(this.width == width && this.height == height && text == contentText)
    }

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

    private fun setRubyMode(rubyMode : String) {
        ruby = Rubys(rubyMode).getRuby()
    }

    //文字描画関数
    private fun drawChar(canvas: Canvas?, vchar: VChar, pos: PointF, style: TextStyle) {
        if ( vchar.isEmpty() ) return
        val firstChar = vchar.firstChar
        val setting = CharSetting.getSetting(firstChar)
        val fontSpacing = style.fontSpace//paint.getFontSpacing();
        var halfOffset = 0f//縦書き半角文字の場合の中央寄せ
        //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
        if (vchar.isLatin()) {
            if (setting != null && setting.angle != 0.0f) {
                pos.y -= fontSpacing / 2
            } else {
                if ( vchar.length != 2 ) {
                    halfOffset = 0.2f
                }
            }
        }
        //描画スキップのフラグ
        if (canvas != null) {
            if (setting == null) {
                // 文字設定がない場合、そのまま描画
                canvas.drawText(vchar.string, pos.x + fontSpacing * halfOffset, pos.y, style.paint)
            } else {
                // 文字設定が見つかったので、設定に従い描画
                canvas.save()
                canvas.rotate(setting.angle, pos.x, pos.y)
                canvas.scale(setting.scaleX, setting.scaleY, pos.x, pos.y)
                canvas.drawText(vchar.string,
                        pos.x + fontSpacing * setting.x, pos.y + fontSpacing * setting.y,
                        style.paint)
                canvas.restore()
            }
        }
    }

    //文字列描画関数
    private fun drawString(canvas: Canvas?, s: ArrayList<VChar>, pos: PointF, style: TextStyle) {
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
        if (width != 0 && height !=0 && text != null) {
            this.text = text

            setRubyMode( Rubys.detectRubyMode(text) )

            if ( BOTTOM_SPACE < bodyStyle.fontSpace ){
                BOTTOM_SPACE = bodyStyle.fontSpace.toInt()
            }

            charPositions.clear()

            pageIndex.clear()

            val _lines = lines
            val _ruby = ruby

            // 文字幅
            val fontSpace = bodyStyle.fontSpace
            val bottomY = (height - BOTTOM_SPACE).toFloat() - fontSpace/2
            val wrapY = TOP_SPACE + fontSpace * wrapPosition

            val wrap : Float = if ( wrapPosition == 0 || bottomY < wrapY ) bottomY else wrapY

            var idx = 0
            _lines.clear()
            while( idx >= 0 ){
                val line = calcLine(text , idx , fontSpace , wrap , _ruby)
                _lines.add(line)
                idx = line.next
//                    if ( DEBUG ) {
//                        var str = ""
//                        line.first.line.forEach {
//                            str += it
//                        }
//                        Log.d("===>", str)
//                    }
            }

            // PageIndexの計算
            val state = CurrentState()
            initPos(state.pos)
            val _pageIndex = pageIndex
            val origx = state.pos.x
            var x = origx
            val lineSpace1 = bodyStyle.lineSpace
            val lineSpace2 = bodyStyle.lineSpace / 2
            val _LEFT_SPACE = LEFT_SPACE
            _lines.forEachIndexed {
                lineidx, list ->
                if ( x < _LEFT_SPACE ){
                    _pageIndex.add(lineidx)
                    x = origx
                }
                if ( list.line.size > 0 ){
                    x -= lineSpace1
                }else{
                    x -= lineSpace2
                }
            }
            _pageIndex.add(_lines.size-1)

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

        val positionArray = ArrayList<CharPoint>()

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
                val pointx = ( nextposx + linespace + state.pos.x + linespace ) / 2
                val pointy = ( oldy - fontspace + state.pos.y - fontspace ) /2
                positionArray.add( CharPoint(pointx, pointy, charptr))
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

    private fun String.characterAt(i : Int, isRuby : (Char)->Boolean) : VChar {
        val ch = this[i]
//        if ( UnicodeBlock.of(c1) == UnicodeBlock.HIGH_SURROGATES ){
        if ( '\uD800' <= ch && ch <= '\uDBFF' ){
            val c2 = this[i+1]
            val s = ch.toString() + c2.toString()
            return VChar(str = s, idx = i)
        }
        if ( isRuby.invoke(ch) ) { // ルビ候補ならルビチェック
            if (this.startsWith(ruby.bodyStart1, startIndex = i)) {
                return VChar(str=ruby.bodyStart1, idx = i)
            }
            if (ruby.bodyStart2 != null) {
                if (this.startsWith(ruby.bodyStart2!!, startIndex = i)) {
                    return VChar(str = ruby.bodyStart2!!, idx = i)
                }
            }
            if (this.startsWith(ruby.rubyStart, startIndex = i)) {
                return VChar(ruby.rubyStart, idx = i)
            }
            if (this.startsWith(ruby.rubyEnd, startIndex = i)) {
                return VChar(ruby.rubyEnd, idx = i)
            }
        }
        return VChar(char=ch, idx = i)
    }



    private fun charDrawProcess(canvas: Canvas?, state: CurrentState , margin : Float) {

        //ルビが振られている箇所とルビ部分の判定
        if (state.isRubyEnable) {
            if (state.isRubyBody && (state.bodyText.size > 20 || state.str.isLineBreak())) {
                state.bodyText.add(0,state.buf)
                drawString(canvas, state.bodyText, state.pos, bodyStyle )
                state.bodyText.clear()
                state.buf = VChar()
                state.isRubyBody = false
            }

            if (state.str.equals(ruby.bodyStart1) || state.str.equals(ruby.bodyStart2)) {            //ルビ本体開始
                //ルビ開始中にルビ開始した場合は出力
                if (state.bodyText.isNotEmpty()) {
                    state.bodyText.add(0,state.buf )
                    drawString(canvas, state.bodyText, state.pos, bodyStyle )
                    state.bodyText.clear()
                    state.buf = VChar()
                }
                state.bodyText.clear()
                state.buf = state.str
                state.isRubyBody = true
                state.isRubyBodyStarted = true
                return
            }
            if (state.str.equals(ruby.rubyStart) && //ルビ開始
                    (state.isRubyBody || state.isKanjiBlock)) { //ルビ開始状態であれば
                state.isRuby = true
                state.isRubyBody = false
                state.rubyText.clear()
                return
            }
            if (state.str.equals(ruby.rubyEnd) && state.isRuby) {    //ルビ終了
                drawString(canvas, state.bodyText, state.pos, bodyStyle )
                state.rpos = getRubyPos(state)
                drawString(canvas, state.rubyText, state.rpos, rubyStyle )
                state.isRuby = false
                state.bodyText.clear()
                state.buf = VChar()
                return
            }

            state.isKanjiBlock = if ( ruby.aozora ) {

                //漢字判定はルビ開始判定の後に行う必要あり
                val isKanji = state.str.isNotEmpty() && ( UnicodeBlock.of(state.str.firstChar) === UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || state.str.equals("々") || state.str.equals("〆"))

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

    private fun calcLine(text:String , index:Int , fontSpace : Float , wrap : Float , ruby : Ruby): Line  {

        val result = ArrayList<VChar>()

        var pos = 0f

        var idx = index

        var isRuby = false
        var broken = false

        val rubyBodyStart1 = ruby.bodyStart1
        val rubyBodyStart2 = ruby.bodyStart2
        val rubyRubyStart = ruby.rubyStart
        val rubyRubyEnd  = ruby.rubyEnd
        val checkRuby = ruby.isRuby

        while( idx < text.length ){
            val vchar = text.characterAt(idx,checkRuby)
            val len = vchar.length
            if ( vchar.str != null && checkRuby.invoke(vchar.str[0]) ) {
                //ルビが振られている箇所とルビ部分の判定
                if (vchar.str == rubyBodyStart1 || vchar.str == rubyBodyStart2) {            //ルビ本体開始
                    result.add(vchar)
                    idx += len
                    continue
                }
                if (vchar.str == rubyRubyStart) { //ルビ開始状態であれば
                    result.add(vchar)
                    idx += len
                    isRuby = true
                    continue
                }
                if (vchar.str == rubyRubyEnd) {    //ルビ終了
                    result.add(vchar)
                    idx += len
                    isRuby = false
                    continue
                }
            }
            if (isRuby) {
                result.add(vchar)
                idx += len
                continue
            }
            //その他通常描字

            //改行処理
            if (vchar.isLineBreak()) {
                idx += len
                broken = true
                break
            }

            //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
            if (vchar.isLatin()) {
                val setting = CharSetting.getSetting(vchar.char)
                if (setting != null && setting.angle != 0.0f) {
                    pos -= fontSpace / 2
                }
            }

            pos += fontSpace

            // 行端を超えた場合終了
            if ( pos > wrap ){
                break
            }

            //文字を描画して次へ
            result.add(vchar)
            idx += len
        }

        if ( idx >= text.length ){
            broken = true
        }

        if ( !broken && result.isNotEmpty()) {
            // 行末禁則のチェック
            val vchar = result.last()
            if ( !vchar.isRubyMarkup(ruby) && vchar.isGyoumatuKinsoku() ) {
                result.removeAt(result.size-1)
                idx = vchar.idx
            }else{
                // 次の行の行頭を作成
                var next = if (idx < text.length) text.characterAt(idx,checkRuby) else VChar()

                // ぶら下げ禁則文字をぶら下げる
                if ( !next.isRubyMarkup(ruby) && next.isBurasageKinsoku()) {
                    result.add(next)
                    idx += next.length
                    next = if (idx < text.length) text.characterAt(idx,checkRuby) else VChar()
                }

                // 次の行をチェック
                if (pos > wrap) {
                    val firstIdx = idx
                    val firstResult = ArrayList<VChar>()
                    firstResult.addAll(result)

                    while (result.size > 0) {
                        val lastchar = result.last()
                        if ( !next.isRubyMarkup(ruby) && next.isGyoutouKinsoku()) {
                            // ルビ記号でなく、行末禁則文字なら追い出す
                            idx = lastchar.idx
                            next = lastchar
                            result.removeAt(result.size-1)
                        } else if ( next.isHalfAlNum() && lastchar.isHalfAlNum() ) {
                            // 英数字二連続なら追い出す
                            idx = lastchar.idx
                            next = lastchar
                            result.removeAt(result.size-1)
                        }else{
                            if (next.isLineBreak()) {
                                idx += next.length
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

        val merged = mergeTwoLatinCharacters(result)

        return Line( merged, index , broken , next = idx )
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
            if (str.equals(ruby.bodyStart1) || str.equals(ruby.bodyStart2)) {            //ルビ本体開始
                return@forEach
            }
            if (str.equals(ruby.rubyStart)) { //ルビ開始状態であれば
                isRuby = true
                return@forEach
            }
            if (str.equals(ruby.rubyEnd)) {    //ルビ終了
                isRuby = false
                return@forEach
            }
            if (isRuby) {
                return@forEach
            }
            //その他通常文字

            //タイトルならスタイル変更
            val style = bodyStyle

            val setting = CharSetting.getSetting(str.firstChar)
            val fontSpacing = style.fontSpace//paint.getFontSpacing();
            //半角チェック　縦書きの場合 座標の基準値の扱いが縦横で変わるので、分割
            if (str.isLatin()) {
                if (setting != null && setting.angle != 0.0f) {
                    pos -= fontSpacing / 2
                }
            }

            pos += fontSpace
            charcount ++
        }

        val diff = wrap - pos
        // 句読点が行末に来る場合はぶら下げになるように調整
        if ( charcount > 2 && line.line.last().isBurasageKinsoku() ){
            return (diff + fontSpace ) / ( charcount -1 )
        }
        if ( diff < 0 ){
            return 0F
        }
        return diff / charcount
    }

    private fun mergeTwoLatinCharacters(line : ArrayList<VChar>) : ArrayList<VChar> {

        var last = VChar()
        var last1 = 0
        var last2 = 0
        val result = ArrayList<VChar>()

        line.forEachIndexed {
            idx, vchar ->
            var str = vchar
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
                mergeExclamationAndQuestion( last , str )?.let {
                    // 二文字の！？の組み合わせを発見
                    // 出力側の最後二文字を削除して結合する
                    result.removeAt(result.size -1)
                    result.removeAt(result.size -1)
                    result.add( it )
                    str = VChar()
                }
            }
            last2 = last1
            last1 = kind
            last = str
        }
        return  result
    }

    private fun mergeExclamationAndQuestion(prev:VChar , next:VChar ): VChar?
    {
        if ( prev.isExclamation() && next.isExclamation() ){
            return VChar("!!")
        }else if ( prev.isExclamation() && next.isQuestion() ){
            return VChar("!?")
        }else if ( prev.isQuestion() && next.isQuestion() ){
            return VChar("??")
        }else if ( prev.isQuestion() && next.isExclamation() ){
            return VChar("?!")
        }
        return null
    }

    private fun ArrayList<VChar>.kindOfNextCharacter(idx:Int) : Int
    {
        if ( idx + 1 < this.size ){
            return this[idx+1].kind()
        }else{
            return 0
        }
    }

    private inner class CurrentState internal constructor() {
        internal var str = VChar()

        internal var rubyText = ArrayList<VChar>() //ルビ
        internal var bodyText = ArrayList<VChar>() //ルビ対象
        internal var buf = VChar() //記号の一時保持用

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

        var nearest : CharPoint = CharPoint( 1000000F , 0F , -1 )
        array?.forEach {
            val distance = Math.sqrt( (( it.x - x ) * ( it.x - x ) +  ( it.y - y ) * ( it.y - y )).toDouble() ).toFloat()
            if ( distance < nearest.x ){
                nearest = CharPoint(distance , y - it.y , it.ptr)
            }
        }
        if ( nearest.ptr != -1 ){
            if ( nearest.y > 0 ) {
                val next = nearest.ptr
                if ( next < text.length -1 ){
                    return nearest.ptr + 1
                }
            }
        }
        return nearest.ptr
    }

    fun setOnDoubleClickListener( listener : (Int)->Unit ){
        onDoubleClickListener = listener
    }

    private var onDoubleClickListener : ((Int)->Unit)? = null
    fun onDoubleClick( pos:Int){
        onDoubleClickListener?.invoke(pos)
    }

//    private fun printMeasureNanoTime( tag: String , block : ()->Unit ){
//        val measured = measureNanoTime( block )
//        if ( DEBUG ) {
//            Log.d("=====>", "$tag=${measured}ns")
//        }
//    }

}
