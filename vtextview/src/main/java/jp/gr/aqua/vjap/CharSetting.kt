package jp.gr.aqua.vjap


import java.util.*

internal class CharSetting {

    val charcter: Char
    val angle: Float

    /**
     * xの差分
     * Paint#getFontSpacing() * xが足される
     * -0.5fが設定さsれた場合、1/2文字分左にずれる
     */
    val x: Float

    /**
     * yの差分
     * Paint#getFontSpacing() * yが足される
     * -0.5fが設定された場合、1/2文字分上にずれる
     */
    val y: Float

    val scaleX: Float
    val scaleY: Float

    private constructor(charcter: Char, angle: Float, x: Float, y: Float) : super() {
        this.charcter = charcter
        this.angle = angle
        this.x = x
        this.y = y
        this.scaleX = 1.0f
        this.scaleY = 1.0f
    }

    private constructor(charcter: Char, angle: Float, x: Float, y: Float, scaleX: Float, scaleY: Float) : super() {
        this.charcter = charcter
        this.angle = angle
        this.x = x
        this.y = y
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    companion object {

        private val settings = arrayOf(
                CharSetting('、', 0.0f, 0.7f, -0.6f), CharSetting('。', 0.0f, 0.7f, -0.6f),
                CharSetting('，', 0.0f, 0.7f, -0.6f), CharSetting('．', 0.0f, 0.7f, -0.6f),

                CharSetting('「', 90.0f, -1.0f, -0.13f), CharSetting('」', 90.0f, -0.7f, -0.13f),
                CharSetting('『', 90.0f, -1.0f, -0.13f), CharSetting('』', 90.0f, -0.7f, -0.13f),
                CharSetting('（', 90.0f, -0.8f, -0.13f), CharSetting('）', 90.0f, -0.8f, -0.13f),
                CharSetting('【', 90.0f, -0.8f, -0.13f), CharSetting('】', 90.0f, -0.8f, -0.13f),
                CharSetting('［', 90.0f, -0.8f, -0.13f), CharSetting('］', 90.0f, -0.8f, -0.13f),
                CharSetting('〔', 90.0f, -0.8f, -0.13f), CharSetting('〕', 90.0f, -0.8f, -0.13f),
                CharSetting('〈', 90.0f, -0.8f, -0.13f), CharSetting('〉', 90.0f, -0.8f, -0.13f),
                CharSetting('《', 90.0f, -0.8f, -0.13f), CharSetting('》', 90.0f, -0.8f, -0.13f),
                CharSetting('＜', 90.0f, -0.8f, -0.13f), CharSetting('＞', 90.0f, -0.8f, -0.13f),
                CharSetting('：', 90.0f, -0.8f, -0.1f), CharSetting('；', 90.0f, 0.8f, -0.1f),
                //CharSetting('／', 90.0f, -0.9f, -0.1f),
                CharSetting('｜', 90.0f, -0.8f, -0.1f),
                CharSetting('＝', 90.0f, -0.8f, -0.1f), CharSetting('÷', 90.0f, -0.8f, -0.1f),
                CharSetting('≠', 90.0f, -0.8f, -0.1f),
                CharSetting('≒', 90.0f, -0.8f, -0.1f),
                CharSetting('≡', 90.0f, -0.8f, -0.1f),

                CharSetting('“', 0.0f, -0.0f, 0.6f), CharSetting('”', 0.0f, -0.0f, 0.1f),
                CharSetting('゛', 0.0f, 0.9f, -1.0f), CharSetting('゜', 0.0f, 0.9f, -1.0f),

                //別の文字なので注意
                CharSetting('～', 90.0f, -0.8f, -0.1f), CharSetting('〜', 90.0f, -0.8f, -0.1f),
                CharSetting('─', 90.0f, -0.8f, -0.1f), CharSetting('—', 90.0f, -0.8f, -0.1f),
                CharSetting('―', 90.0f, -0.8f, -0.1f), CharSetting('―', 90.0f, -0.8f, -0.1f),
                CharSetting('−', 90.0f, -0.8f, -0.1f),

                CharSetting('↑', 90.0f, -0.8f, -0.1f),
                CharSetting('↓', 90.0f, -0.8f, -0.1f),
                CharSetting('←', 90.0f, -0.8f, -0.1f),
                CharSetting('→', 90.0f, -0.8f, -0.1f),

                CharSetting('.', 0.0f, 0.7f, -0.6f), CharSetting(',', 0.0f, 0.7f, -0.6f),
                CharSetting('(', 90.0f, -0.3f, -0.15f), CharSetting(')', 90.0f, -0.3f, -0.15f),
                CharSetting('[', 90.0f, -0.3f, -0.13f), CharSetting(']', 90.0f, -0.3f, -0.13f),
                CharSetting('{', 90.0f, -0.3f, -0.13f), CharSetting('}', 90.0f, -0.3f, -0.13f),
                CharSetting('<', 90.0f, -0.3f, -0.13f),CharSetting('>', 90.0f, -0.3f, -0.13f),
                CharSetting(':', 90.0f, -0.4f, -0.1f), CharSetting(';', 90.0f, -0.4f, -0.1f),
                CharSetting('~', 90.0f, -0.4f, -0.1f), CharSetting('|', 90.0f, -0.4f, -0.1f),
                CharSetting('/', 90.0f, -0.4f, -0.1f), CharSetting('…', 90.0f, -0.8f, -0.1f),
                CharSetting('=', 90.0f, -0.4f, -0.1f), CharSetting('-', 90.0f, -0.4f, -0.1f),

                CharSetting('〝', 90.0f, -0.9f, -0.1f), CharSetting('〟', 90.0f, -0.9f, -0.1f),


                CharSetting('ぁ', 0.0f, 0.1f, -0.1f), CharSetting('ぃ', 0.0f, 0.1f, -0.1f),
                CharSetting('ぅ', 0.0f, 0.1f, -0.1f), CharSetting('ぇ', 0.0f, 0.1f, -0.1f),
                CharSetting('ぉ', 0.0f, 0.1f, -0.1f), CharSetting('っ', 0.0f, 0.1f, -0.1f),
                CharSetting('ゃ', 0.0f, 0.1f, -0.1f), CharSetting('ゅ', 0.0f, 0.1f, -0.1f),
                CharSetting('ょ', 0.0f, 0.1f, -0.1f), CharSetting('ァ', 0.0f, 0.1f, -0.1f),
                CharSetting('ィ', 0.0f, 0.1f, -0.1f), CharSetting('ゥ', 0.0f, 0.1f, -0.1f),
                CharSetting('ェ', 0.0f, 0.1f, -0.1f), CharSetting('ォ', 0.0f, 0.1f, -0.1f),
                CharSetting('ッ', 0.0f, 0.1f, -0.1f), CharSetting('ャ', 0.0f, 0.1f, -0.1f),
                CharSetting('ュ', 0.0f, 0.1f, -0.1f), CharSetting('ョ', 0.0f, 0.1f, -0.1f),
                CharSetting('ー', -90.0f, -0.05f, 0.9f),
                CharSetting('a', 90.0f, -0.4f, -0.1f),CharSetting('b', 90.0f, -0.4f, -0.1f),
                CharSetting('c', 90.0f, -0.4f, -0.1f), CharSetting('d', 90.0f, -0.4f, -0.1f),
                CharSetting('e', 90.0f, -0.4f, -0.1f), CharSetting('f', 90.0f, -0.4f, -0.1f),
                CharSetting('g', 90.0f, -0.4f, -0.1f), CharSetting('h', 90.0f, -0.4f, -0.1f),
                CharSetting('i', 90.0f, -0.4f, -0.1f), CharSetting('j', 90.0f, -0.4f, -0.1f),
                CharSetting('k', 90.0f, -0.4f, -0.1f), CharSetting('l', 90.0f, -0.4f, -0.1f),
                CharSetting('m', 90.0f, -0.4f, -0.1f), CharSetting('n', 90.0f, -0.4f, -0.1f),
                CharSetting('o', 90.0f, -0.4f, -0.1f), CharSetting('p', 90.0f, -0.4f, -0.1f),
                CharSetting('q', 90.0f, -0.4f, -0.1f), CharSetting('r', 90.0f, -0.4f, -0.1f),
                CharSetting('s', 90.0f, -0.4f, -0.1f), CharSetting('t', 90.0f, -0.4f, -0.1f),
                CharSetting('u', 90.0f, -0.4f, -0.1f), CharSetting('v', 90.0f, -0.4f, -0.1f),
                CharSetting('w', 90.0f, -0.4f, -0.1f), CharSetting('x', 90.0f, -0.4f, -0.1f),
                CharSetting('y', 90.0f, -0.4f, -0.1f), CharSetting('z', 90.0f, -0.4f, -0.1f),
                CharSetting('A', 90.0f, -0.4f, -0.1f), CharSetting('B', 90.0f, -0.4f, -0.1f),
                CharSetting('C', 90.0f, -0.4f, -0.1f), CharSetting('D', 90.0f, -0.4f, -0.1f),
                CharSetting('E', 90.0f, -0.4f, -0.1f), CharSetting('F', 90.0f, -0.4f, -0.1f),
                CharSetting('G', 90.0f, -0.4f, -0.1f), CharSetting('H', 90.0f, -0.4f, -0.1f),
                CharSetting('I', 90.0f, -0.4f, -0.1f), CharSetting('J', 90.0f, -0.4f, -0.1f),
                CharSetting('K', 90.0f, -0.4f, -0.1f), CharSetting('L', 90.0f, -0.4f, -0.1f),
                CharSetting('M', 90.0f, -0.4f, -0.1f), CharSetting('N', 90.0f, -0.4f, -0.1f),
                CharSetting('O', 90.0f, -0.4f, -0.1f), CharSetting('P', 90.0f, -0.4f, -0.1f),
                CharSetting('Q', 90.0f, -0.4f, -0.1f), CharSetting('R', 90.0f, -0.4f, -0.1f),
                CharSetting('S', 90.0f, -0.4f, -0.1f), CharSetting('T', 90.0f, -0.4f, -0.1f),
                CharSetting('U', 90.0f, -0.4f, -0.1f), CharSetting('V', 90.0f, -0.4f, -0.1f),
                CharSetting('W', 90.0f, -0.4f, -0.1f), CharSetting('X', 90.0f, -0.4f, -0.1f),
                CharSetting('Y', 90.0f, -0.4f, -0.1f), CharSetting('Z', 90.0f, -0.4f, -0.1f)
                /* 全角英字は縦表示に
		new CharSetting('ａ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｂ', 90.0f, -0.9f, -0.1f), new CharSetting('ｃ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｄ', 90.0f, -0.9f, -0.1f), new CharSetting('ｅ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｆ', 90.0f, -0.9f, -0.1f), new CharSetting('ｇ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｈ', 90.0f, -0.9f, -0.1f), new CharSetting('ｉ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｊ', 90.0f, -0.9f, -0.1f), new CharSetting('ｋ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｌ', 90.0f, -0.9f, -0.1f), new CharSetting('ｍ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｎ', 90.0f, -0.9f, -0.1f), new CharSetting('ｏ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｐ', 90.0f, -0.9f, -0.1f), new CharSetting('ｑ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｒ', 90.0f, -0.9f, -0.1f), new CharSetting('ｓ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｔ', 90.0f, -0.9f, -0.1f), new CharSetting('ｕ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｖ', 90.0f, -0.9f, -0.1f), new CharSetting('ｗ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｘ', 90.0f, -0.9f, -0.1f), new CharSetting('ｙ', 90.0f, -0.9f, -0.1f),
        new CharSetting('ｚ', 90.0f, -0.9f, -0.1f), new CharSetting('Ａ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｂ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｃ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｄ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｅ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｆ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｇ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｈ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｉ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｊ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｋ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｌ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｍ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｎ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｏ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｐ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｑ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｒ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｓ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｔ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｕ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｖ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｗ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｘ', 90.0f, -0.9f, -0.1f), new CharSetting('Ｙ', 90.0f, -0.9f, -0.1f),
        new CharSetting('Ｚ', 90.0f, -0.9f, -0.1f),
		 */

        //        CharSetting('\u0000', 90.0f, 0.0f, -0.1f)
        )

        private val sMap = HashMap<Char, CharSetting>()

        fun getSetting(character: VChar): CharSetting? {
            if ( character.length == 1 ){
                return sMap[character.firstChar]
            }
            return null;
        }


//        fun isPunctuationMark(s: String): Boolean {
//            val PUNCTUATION_MARK = '、。「」'
//            return PUNCTUATION_MARK.contains(s)
//        }

        fun initCharMap(ipamincho: Boolean) {
            sMap.clear()
            for (setting in settings) {
                sMap.put(setting.charcter, setting)
            }
            // IPA明朝用の調整
            if (ipamincho) {
                sMap.put('ー', CharSetting('ー', 88.0f, -0.9f, 0.93f, 1.0f, -1.0f))
            }
        }
    }
}
