package jp.gr.aqua.vjap

data class VChar(val str : String? = null , val char : Char = '\u0000' , val idx : Int = -1)
{
    val length : Int
        get() = (str?.length?:1)

    val asString: String
        get() = str?:char.toString()

    val firstChar : Char
        get() = str?.get(0)?:char

    fun isLineBreak() : Boolean = char == '\n'
    fun isLatin() : Boolean = '\u0000' < char  && char < '\u0080'
    fun isLatinString() : Boolean {
        val ch = if ( str != null ){ str[0] } else{ char }
        return '\u0000' < ch  && ch < '\u0080'
    }
    fun isGyoumatuKinsoku() : Boolean = KINSOKU_GYOUMATU.contains(char)
    fun isGyoutouKinsoku() : Boolean = KINSOKU_GYOUTOU.contains(char)
    fun isBurasageKinsoku() : Boolean = KINSOKU_BURASAGE.contains(char)
    fun isRubyMarkup(ruby:Ruby) : Boolean = str?.let{ruby.isRubyMarkup(str)}?:false

    fun isExclamation() : Boolean = char == '!' || char == '！'
    fun isQuestion() : Boolean = char == '?' || char == '？'
    fun isEmpty() : Boolean = str?.isEmpty() ?: char=='\u0000'
    fun isNotEmpty() : Boolean = !isEmpty()

    fun isKanji() : Boolean = isNotEmpty() &&
            ( Character.UnicodeBlock.of(firstChar) === java.lang.Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || this.equals("々") || this.equals("〆") || this.equals("仝") || this.equals("〇") || this.equals("ヶ") )

    override fun equals(other: Any?) : Boolean{
        return if ( other is VChar ) {
            if (this.str != null && other.str != null) {
                this.str == other.str
            } else if (this.str != null && other.str == null) {
                if (this.str.length == 1) {
                    this.firstChar == other.char
                } else {
                    false
                }
            } else if (this.str == null && other.str != null) {
                if (other.str.length == 1) {
                    this.char == other.firstChar
                } else {
                    false
                }
            } else {
                this.char == other.char
            }
        }else if ( other is String ){
            if (this.str != null ) {
                this.str == other
            } else {
                if (other.length == 1) {
                    other[0] == this.char
                }else{
                    false
                }
            }
        }else{
            false
        }
    }
    fun kind() : Int
    {
        val ch = firstChar
        if ( '0' <= ch && ch <= '9') return 1
        if ( 'A' <= ch && ch <= 'Z') return 2
        if ( 'a' <= ch && ch <= 'z') return 2
        return 0
    }
    fun isHalfAlNum() : Boolean = kind() != 0

    override fun hashCode(): Int {
        return super.hashCode()
    }

    operator fun plus(next : VChar) : VChar{
        return VChar(this.asString + next.asString)
    }

    operator fun plus(next : String) : VChar{
        return VChar(this.asString + next)
    }

    private val KINSOKU_BURASAGE = ",、。.　 "

    private val KINSOKU_GYOUTOU = ",)]｝、〕〉》」』】〙〗〟’”｠»）" +
            "ヽヾーァィゥェォッャュョヮヵヶぁぃぅぇぉっゃゅょゎゕゖㇰㇱㇳㇲㇳㇴㇵㇶㇷㇸㇹㇺㇻㇼㇽㇾㇿ々〻"+
            "‐゠–〜～"+
            "？！?!‼⁇⁈⁉"+
            "・:;。.　 "

    private val KINSOKU_GYOUMATU = "([｛〔〈《「『【〘〖〝‘“｟«"

    //private val KANJI_INKCLUDE = "々仝〆〇ヶ"      // 青空文庫では漢字じゃないけどルビ判定で漢字とみなす文字

    override fun toString(): String {
        return this.asString
    }
}

